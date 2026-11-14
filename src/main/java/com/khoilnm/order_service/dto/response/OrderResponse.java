package com.khoilnm.order_service.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderResponse {
    private String id;

    private String customerId;

    private String status;

    private int amount;
}
