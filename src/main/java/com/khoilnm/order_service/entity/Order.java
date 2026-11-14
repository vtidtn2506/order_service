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
@Table(name = "`order`", schema = "`order`")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Order {

    @Id
    private String id;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "status")
    private String status;

    @Column(name = "amount")
    private int amount;
}
