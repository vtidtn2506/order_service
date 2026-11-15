package com.khoilnm.order_service.service.impl;

import com.khoilnm.order_service.constant.KafkaTopic;
import com.khoilnm.order_service.dto.event.OrderCreatedEvent;
import com.khoilnm.order_service.thirdparty.ProductClient;
import com.khoilnm.order_service.dto.response.ProductResponse;
import com.khoilnm.commoncore.exception.BusinessException;
import com.khoilnm.commoncore.response.ResponseCode;
import com.khoilnm.order_service.constant.OrderStatus;
import com.khoilnm.order_service.dto.request.CreateOrderItemRequest;
import com.khoilnm.order_service.dto.request.CreateOrderRequest;
import com.khoilnm.order_service.dto.request.DeductStockRequest;
import com.khoilnm.order_service.dto.response.OrderDetailResponse;
import com.khoilnm.order_service.dto.response.OrderResponse;
import com.khoilnm.order_service.entity.Order;
import com.khoilnm.order_service.entity.OrderItem;
import com.khoilnm.order_service.mapper.OrderMapper;
import com.khoilnm.order_service.repository.OrderItemRepository;
import com.khoilnm.order_service.repository.OrderRepository;
import com.khoilnm.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.khoilnm.order_service.constant.OrderResponseCode.ORDER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders() {
        return orderMapper.toResponses(orderRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> businessException(ORDER_NOT_FOUND, "Order not found: " + id));
        return orderMapper.toDetailResponse(order, orderItemRepository.findAllByOrderId(id));
    }

    @Override
    public OrderDetailResponse createOrder(CreateOrderRequest request) {
        // 1. Called outside the DB transaction, so no connection is held while waiting for product_service
        Map<String, ProductResponse> products = productClient.deductStock(toDeductStockRequest(request.getItems()))
                .stream()
                .collect(Collectors.toMap(ProductResponse::getId, Function.identity()));

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING.name())
                .build();

        List<OrderItem> items = request.getItems().stream()
                .map(item -> OrderItem.builder()
                        .orderId(order.getId())
                        .productId(item.getProductId())
                        .price(products.get(item.getProductId()).getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        order.setAmount(items.stream()
                .mapToInt(item -> Math.multiplyExact(item.getPrice(), item.getQuantity()))
                .reduce(0, Math::addExact));

        Order savedOrder = orderRepository.save(order);
        List<OrderItem> savedItems = orderItemRepository.saveAll(items);
        // Sent to Kafka only after this transaction commits
        OrderCreatedEvent event = toEvent(order, items);
        kafkaTemplate.send(KafkaTopic.ORDER_CREATE, event.orderId(), event);
        return orderMapper.toDetailResponse(savedOrder, savedItems);
    }

    private static OrderCreatedEvent toEvent(Order order, List<OrderItem> items) {
        return new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getAmount(),
                items.stream()
                        .map(item -> new OrderCreatedEvent.Item(
                                item.getId(), item.getProductId(), item.getPrice(), item.getQuantity()))
                        .toList(),
                order.getCreatedDate(),
                order.getCreatedBy());
    }

    private static DeductStockRequest toDeductStockRequest(List<CreateOrderItemRequest> items) {
        return new DeductStockRequest(items.stream()
                .map(item -> new DeductStockRequest.Item(item.getProductId(), item.getQuantity()))
                .toList());
    }

    private static BusinessException businessException(ResponseCode code, String message) {
        return new BusinessException(code, message, code.getDefaultHttpStatus());
    }
}
