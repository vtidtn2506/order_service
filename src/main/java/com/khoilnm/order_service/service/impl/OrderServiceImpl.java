package com.khoilnm.order_service.service.impl;

import com.khoilnm.order_service.thirdparty.ProductClient;
import com.khoilnm.order_service.dto.response.ProductResponse;
import com.khoilnm.order_service.constant.OrderStatus;
import com.khoilnm.order_service.dto.request.CreateOrderItemRequest;
import com.khoilnm.order_service.dto.request.CreateOrderRequest;
import com.khoilnm.order_service.dto.response.OrderDetailResponse;
import com.khoilnm.order_service.dto.response.OrderResponse;
import com.khoilnm.order_service.entity.Order;
import com.khoilnm.order_service.entity.OrderItem;
import com.khoilnm.order_service.mapper.OrderMapper;
import com.khoilnm.order_service.repository.OrderItemRepository;
import com.khoilnm.order_service.repository.OrderRepository;
import com.khoilnm.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders() {
        return orderMapper.toResponses(orderRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
        return orderMapper.toDetailResponse(order, orderItemRepository.findAllByOrderId(id));
    }

    @Override
    public OrderDetailResponse createOrder(CreateOrderRequest request) {
        // Fetch prices from product_service before opening the DB transaction,
        // so a DB connection isn't held during the HTTP call.
        Map<String, ProductResponse> products = getProducts(request.getItems());

        return transactionTemplate.execute(status -> {
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
            return orderMapper.toDetailResponse(savedOrder, savedItems);
        });
    }

    private Map<String, ProductResponse> getProducts(List<CreateOrderItemRequest> items) {
        Set<String> productIds = items.stream()
                .map(CreateOrderItemRequest::getProductId)
                .collect(Collectors.toSet());

        Map<String, ProductResponse> products = productClient.getProductsByIds(productIds);

        List<String> missing = productIds.stream().filter(id -> !products.containsKey(id)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found: " + String.join(", ", missing));
        }
        return products;
    }
}
