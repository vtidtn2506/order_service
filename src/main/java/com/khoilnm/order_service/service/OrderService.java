package com.khoilnm.order_service.service;

import com.khoilnm.order_service.dto.event.StockDeductResultEvent;
import com.khoilnm.order_service.dto.request.CreateOrderRequest;
import com.khoilnm.order_service.dto.response.OrderDetailResponse;
import com.khoilnm.order_service.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {
    List<OrderResponse> getOrders();

    OrderDetailResponse getOrder(String id);

    OrderDetailResponse createOrder(CreateOrderRequest request);

    void applyStockDeductResult(StockDeductResultEvent result);
}
