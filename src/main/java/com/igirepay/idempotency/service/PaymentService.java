package com.igirepay.idempotency.service;

import com.igirepay.idempotency.dto.PaymentRequest;
import com.igirepay.idempotency.dto.PaymentResponse;
import org.springframework.http.ResponseEntity;

/**
 * Service interface for payment processing
 */
public interface PaymentService {
    
    /**
     * Process a payment request with idempotency guarantee
     * 
     * @param idempotencyKey Unique key from request header
     * @param request Payment request details
     * @return Response entity with payment response and headers
     */
    ResponseEntity<PaymentResponse> processPayment(String idempotencyKey, PaymentRequest request);
}
