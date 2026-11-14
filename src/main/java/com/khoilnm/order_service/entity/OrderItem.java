package com.khoilnm.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "order_item", schema = "`order`")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OrderItem {

    @Id
    private String id;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "price")
    private int price;

    @Column(name = "quantity")
    private int quantity;

}
