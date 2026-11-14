package com.khoilnm.order_service.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderDetailResponse extends OrderResponse {
    private List<OrderItemResponse> items;
}
