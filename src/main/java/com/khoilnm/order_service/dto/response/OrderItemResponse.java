package com.khoilnm.order_service.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemResponse {
    private String id;

    private String productId;

    private int price;

    private int quantity;
}
