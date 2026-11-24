package com.khoilnm.order_service.dto.event;

/** Payload of topic {@link com.khoilnm.order_service.constant.KafkaTopic#STOCK_DEDUCT_RESULT}. */
public record StockDeductResultEvent(String orderId, Status status, String reason) {

    public enum Status {
        SUCCESS,
        FAILED
    }
}
