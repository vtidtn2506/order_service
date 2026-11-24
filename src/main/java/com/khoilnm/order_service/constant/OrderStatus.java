package com.khoilnm.order_service.constant;

public enum OrderStatus {
    /** Saved, waiting for product_service to deduct the stock. */
    PENDING,
    /** Stock deducted. */
    CONFIRMED,
    /** Stock could not be deducted (insufficient stock, unknown product...). */
    FAILED,
    CANCELLED,
    COMPLETED
}
