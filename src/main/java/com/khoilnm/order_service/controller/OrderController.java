package com.khoilnm.order_service.controller;

import com.khoilnm.commoncore.response.BaseResponse;
import com.khoilnm.order_service.dto.request.CreateOrderRequest;
import com.khoilnm.order_service.dto.response.OrderDetailResponse;
import com.khoilnm.order_service.dto.response.OrderResponse;
import com.khoilnm.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    public BaseResponse<List<OrderResponse>> getOrders() {
        return BaseResponse.success(orderService.getOrders());
    }

    @GetMapping("/{id}")
    public BaseResponse<OrderDetailResponse> getOrder(@PathVariable String id) {
        return BaseResponse.success(orderService.getOrder(id));
    }

    @PostMapping
    public BaseResponse<OrderDetailResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        BaseResponse<OrderDetailResponse> response = BaseResponse.success(orderService.createOrder(request), "Order created");
        // BaseResponseStatusAdvice uses BaseResponse.status as the HTTP status → 201
        response.setStatus(HttpStatus.CREATED);
        return response;
    }
}
