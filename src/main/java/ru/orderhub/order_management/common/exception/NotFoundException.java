package ru.orderhub.order_management.common.exception;

/**
 * 404 — объект не найден.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
