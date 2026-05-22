package com.igirepay.idempotency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igirepay.idempotency.dto.PaymentRequest;
import com.igirepay.idempotency.dto.PaymentResponse;
import com.igirepay.idempotency.exception.IdempotencyKeyMismatchException;
import com.igirepay.idempotency.model.IdempotencyRecord;
import com.igirepay.idempotency.model.IdempotencyRecord.ProcessingStatus;
import com.igirepay.idempotency.repository.IdempotencyRecordRepository;
import com.igirepay.idempotency.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of PaymentService with full idempotency logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ResponseEntity<PaymentResponse> processPayment(String idempotencyKey, PaymentRequest request) {
        
        log.info("Processing payment with idempotency key: {}", idempotencyKey);

        try {
            // Convert request to JSON string and generate hash
            String requestBodyJson = objectMapper.writeValueAsString(request);
            String requestHash = HashUtil.generateSHA256(requestBodyJson);

            // Try to find existing record with pessimistic lock (prevents race conditions)
            Optional<IdempotencyRecord> existingRecordOpt = 
                idempotencyRecordRepository.findByIdempotencyKeyWithLock(idempotencyKey);

            if (existingRecordOpt.isPresent()) {
                IdempotencyRecord existingRecord = existingRecordOpt.get();
                
                // Check if request body is different (User Story 3)
                if (!existingRecord.getRequestBodyHash().equals(requestHash)) {
                    log.warn("Idempotency key reused with different request body");
                    throw new IdempotencyKeyMismatchException(
                        "Idempotency key already used for a different request body"
                    );
                }

                // If still processing, wait (In-Flight Bonus)
                if (existingRecord.getProcessingStatus() == ProcessingStatus.PROCESSING) {
                    log.info("Request is still processing, current request will wait");
                    // The pessimistic lock will make this request wait until the first completes
                    // After the first transaction commits, this will see the COMPLETED status
                    return handleInFlightRequest(existingRecord);
                }

                // Return cached response (User Story 2)
                log.info("Returning cached response for duplicate request");
                return buildCachedResponse(existingRecord);
            }

            // First time seeing this key - create new record with PROCESSING status
            IdempotencyRecord newRecord = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .requestBodyHash(requestHash)
                .requestBody(requestBodyJson)
                .processingStatus(ProcessingStatus.PROCESSING)
                .build();
            
            idempotencyRecordRepository.save(newRecord);

            // Process the payment (User Story 1)
            PaymentResponse response = executePaymentProcessing(request);

            // Save the response
            String responseJson = objectMapper.writeValueAsString(response);
            newRecord.setResponseBody(responseJson);
            newRecord.setStatusCode(HttpStatus.CREATED.value());
            newRecord.setProcessingStatus(ProcessingStatus.COMPLETED);
            idempotencyRecordRepository.save(newRecord);

            log.info("Payment processed successfully");

            // Return response with appropriate headers
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Cache-Hit", "false");
            
            return new ResponseEntity<>(response, headers, HttpStatus.CREATED);

        } catch (JsonProcessingException e) {
            log.error("Error processing JSON", e);
            throw new RuntimeException("Error processing request", e);
        }
    }

    /**
     * Simulates actual payment processing
     * In real system, this would call payment gateway API
     */
    private PaymentResponse executePaymentProcessing(PaymentRequest request) {
        log.info("Executing payment processing for amount: {} {}", request.getAmount(), request.getCurrency());
        
        // Simulate processing delay (2 seconds as per requirements)
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted", e);
        }

        // Build response
        return PaymentResponse.builder()
            .message(String.format("Charged %.2f %s", request.getAmount(), request.getCurrency()))
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .transactionId(UUID.randomUUID().toString())
            .processedAt(LocalDateTime.now())
            .status("SUCCESS")
            .build();
    }

    /**
     * Handle request that arrived while another is still processing (Bonus User Story)
     */
    private ResponseEntity<PaymentResponse> handleInFlightRequest(IdempotencyRecord record) 
            throws JsonProcessingException {
        
        // Thanks to pessimistic lock, by the time we reach here, 
        // the first request should have completed
        // Re-fetch the record to get updated status
        IdempotencyRecord updatedRecord = idempotencyRecordRepository
            .findById(record.getId())
            .orElseThrow(() -> new RuntimeException("Record disappeared"));

        if (updatedRecord.getProcessingStatus() == ProcessingStatus.COMPLETED) {
            return buildCachedResponse(updatedRecord);
        }

        // If still processing (unlikely due to lock), return processing status
        PaymentResponse response = PaymentResponse.builder()
            .message("Request is being processed")
            .status("PROCESSING")
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    /**
     * Build response from cached record
     */
    private ResponseEntity<PaymentResponse> buildCachedResponse(IdempotencyRecord record) 
            throws JsonProcessingException {
        
        PaymentResponse cachedResponse = objectMapper.readValue(
            record.getResponseBody(), 
            PaymentResponse.class
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Cache-Hit", "true");

        return new ResponseEntity<>(
            cachedResponse, 
            headers, 
            HttpStatus.valueOf(record.getStatusCode())
        );
    }
}