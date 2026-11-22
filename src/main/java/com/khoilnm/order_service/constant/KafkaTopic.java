package com.khoilnm.order_service.constant;

public final class KafkaTopic {
    /**
     * Published after an order (and its items) is committed. Key = orderId.
     * product_service consumes it to deduct the stock.
     */
    public static final String ORDER_CREATE = "order-create";

    private KafkaTopic() {
    }
}
