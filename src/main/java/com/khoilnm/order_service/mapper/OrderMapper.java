package com.khoilnm.order_service.mapper;

import com.khoilnm.order_service.dto.response.OrderDetailResponse;
import com.khoilnm.order_service.dto.response.OrderResponse;
import com.khoilnm.order_service.entity.Order;
import com.khoilnm.order_service.entity.OrderItem;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface OrderMapper {
    List<OrderResponse> toResponses(List<Order> orders);

    OrderDetailResponse toDetailResponse(Order order, List<OrderItem> items);
}
