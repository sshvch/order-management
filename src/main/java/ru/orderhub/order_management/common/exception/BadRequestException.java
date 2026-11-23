package ru.orderhub.order_management.common.exception;

/**
 * 400 — запрос некорректный с точки зрения бизнес-логики.
 * Например: товара не хватает на складе или email уже занят.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
