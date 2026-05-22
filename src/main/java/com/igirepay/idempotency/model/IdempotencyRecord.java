package com.igirepay.idempotency.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an idempotency record in the database.
 * This stores information about each unique payment request.
 */
@Entity
@Table(name = "idempotency_records", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique key sent by the client in the header
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 500)
    private String idempotencyKey;

    /**
     * Hash of the request body - used to detect if request body changed
     */
    @Column(name = "request_body_hash", nullable = false, length = 64)
    private String requestBodyHash;

    /**
     * Original request body stored as JSON string
     */
    @Column(name = "request_body", nullable = false, columnDefinition = "TEXT")
    private String requestBody;

    /**
     * Saved response body to return for duplicate requests
     */
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    /**
     * HTTP status code of the response
     */
    @Column(name = "status_code")
    private Integer statusCode;

    /**
     * Current processing status: PROCESSING, COMPLETED, FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus;

    /**
     * Timestamp when record was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when record was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Enum for processing status
     */
    public enum ProcessingStatus {
        PROCESSING,  // Request is currently being processed
        COMPLETED,   // Request completed successfully
        FAILED       // Request failed
    }
}