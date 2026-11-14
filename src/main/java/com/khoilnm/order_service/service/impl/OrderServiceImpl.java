package com.khoilnm.order_service.service.impl;

import com.khoilnm.order_service.dto.response.OrderDetailResponse;
import com.khoilnm.order_service.dto.response.OrderResponse;
import com.khoilnm.order_service.entity.Order;
import com.khoilnm.order_service.mapper.OrderMapper;
import com.khoilnm.order_service.repository.OrderItemRepository;
import com.khoilnm.order_service.repository.OrderRepository;
import com.khoilnm.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;

    @Override
    public List<OrderResponse> getOrders() {
        return orderMapper.toResponses(orderRepository.findAll());
    }

    @Override
    public OrderDetailResponse getOrder(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
        return orderMapper.toDetailResponse(order, orderItemRepository.findAllByOrderId(id));
    }
}
