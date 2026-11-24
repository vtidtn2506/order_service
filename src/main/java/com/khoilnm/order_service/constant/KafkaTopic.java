package com.khoilnm.order_service.constant;

public final class KafkaTopic {
    /**
     * Published after an order (and its items) is committed. Key = orderId.
     * product_service consumes it to deduct the stock.
     */
    public static final String ORDER_CREATE = "order-create";

    /** Result of the stock deduction, published by product_service. Key = orderId. */
    public static final String STOCK_DEDUCT_RESULT = "stock-deduct-result";

    private KafkaTopic() {
    }
}
