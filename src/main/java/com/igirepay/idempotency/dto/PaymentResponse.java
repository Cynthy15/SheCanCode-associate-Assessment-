package com.igirepay.idempotency.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for payment responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private String message;
    private Double amount;
    private String currency;
    private String transactionId;
    private LocalDateTime processedAt;
    private String status; // SUCCESS, FAILED, etc.
}