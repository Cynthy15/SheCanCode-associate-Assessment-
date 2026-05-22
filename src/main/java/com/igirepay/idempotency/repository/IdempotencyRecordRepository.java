package com.igirepay.idempotency.repository;

import com.igirepay.idempotency.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Repository interface for database operations on IdempotencyRecord
 */
@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    /**
     * Find a record by idempotency key with pessimistic write lock
     * This prevents race conditions when multiple requests arrive simultaneously
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ir FROM IdempotencyRecord ir WHERE ir.idempotencyKey = :key")
    Optional<IdempotencyRecord> findByIdempotencyKeyWithLock(@Param("key") String idempotencyKey);

    /**
     * Find a record by idempotency key without locking
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);
}

