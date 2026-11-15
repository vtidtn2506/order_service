package com.khoilnm.order_service.dto.event;

import java.time.Instant;
import java.util.List;

/** Payload of topic {@link com.khoilnm.order_service.constant.KafkaTopic#ORDER_CREATE}. */
public record OrderCreatedEvent(
        String orderId,
        String customerId,
        String status,
        int amount,
        List<Item> items,
        Instant createdDate,
        String createdBy
) {
    public record Item(String orderItemId, String productId, int price, int quantity) {
    }
}
