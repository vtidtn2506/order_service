package com.khoilnm.order_service.service.impl;

import com.khoilnm.order_service.constant.KafkaTopic;
import com.khoilnm.order_service.dto.event.OrderCreatedEvent;
import com.khoilnm.order_service.dto.event.StockDeductResultEvent;
import com.khoilnm.order_service.thirdparty.ProductClient;
import com.khoilnm.order_service.dto.response.ProductResponse;
import com.khoilnm.commoncore.exception.BusinessException;
import com.khoilnm.commoncore.response.ResponseCode;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.khoilnm.order_service.constant.OrderResponseCode.ORDER_NOT_FOUND;
import static com.khoilnm.order_service.constant.OrderResponseCode.PRODUCT_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
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
                .orElseThrow(() -> businessException(ORDER_NOT_FOUND, "Order not found: " + id));
        return orderMapper.toDetailResponse(order, orderItemRepository.findAllByOrderId(id));
    }

    /**
     * 1. Read prices from product_service (HTTP, read-only)
     * 2. Save order PENDING + items (one transaction)
     * 3. After commit: publish "order-create" (product_service consumes it and deducts the stock)
     * The response is returned right away with status PENDING; the stock is deducted later.
     */
    @Override
    public OrderDetailResponse createOrder(CreateOrderRequest request) {
        // 1. Outside the DB transaction, so no connection is held during the HTTP call
        Map<String, ProductResponse> products = getProducts(request.getItems());

        // 2.
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

        OrderDetailResponse response = transactionTemplate.execute(status -> {
            Order savedOrder = orderRepository.save(order);
            List<OrderItem> savedItems = orderItemRepository.saveAll(items);
            return orderMapper.toDetailResponse(savedOrder, savedItems);
        });

        // 3. The transaction is committed here: a rolled-back order is never published
        publish(KafkaTopic.ORDER_CREATE, order.getId(), toEvent(order, items));
        return response;
    }

    @Override
    @Transactional
    public void applyStockDeductResult(StockDeductResultEvent result) {
        Order order = orderRepository.findById(result.orderId()).orElse(null);
        if (order == null) {
            log.warn("Stock deduct result for unknown order {}, ignoring", result.orderId());
            return;
        }
        // Idempotent: a redelivered result (or a late one) can't change a final status
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            log.info("Order {} is already {}, ignoring stock deduct result {}",
                    order.getId(), order.getStatus(), result.status());
            return;
        }

        OrderStatus newStatus = result.status() == StockDeductResultEvent.Status.SUCCESS
                ? OrderStatus.CONFIRMED
                : OrderStatus.FAILED;
        order.setStatus(newStatus.name());
        log.info("Order {}: PENDING -> {}{}", order.getId(), newStatus,
                result.reason() == null ? "" : " (" + result.reason() + ")");
    }

    private Map<String, ProductResponse> getProducts(List<CreateOrderItemRequest> items) {
        Set<String> productIds = items.stream()
                .map(CreateOrderItemRequest::getProductId)
                .collect(Collectors.toSet());

        Map<String, ProductResponse> products = productClient.getProductsByIds(productIds);

        List<String> missing = productIds.stream().filter(id -> !products.containsKey(id)).sorted().toList();
        if (!missing.isEmpty()) {
            throw businessException(PRODUCT_NOT_FOUND, "Product not found: " + String.join(", ", missing));
        }
        return products;
    }

    private void publish(String topic, String orderId, Object payload) {
        kafkaTemplate.send(topic, orderId, payload).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} for order {}", topic, orderId, ex);
            } else {
                log.info("Published {} for order {} (partition={}, offset={})", topic, orderId,
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
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

    private static BusinessException businessException(ResponseCode code, String message) {
        return new BusinessException(code, message, code.getDefaultHttpStatus());
    }
}
