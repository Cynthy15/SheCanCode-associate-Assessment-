package com.igirepay.idempotency.exception;

public class IdempotencyKeyMismatchException extends RuntimeException {

    public IdempotencyKeyMismatchException(String message) {
        super(message);
    }
}
