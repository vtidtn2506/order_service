package com.khoilnm.order_service.entity;


import com.khoilnm.commoncore.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "`order`", schema = "`order`")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order extends BaseEntity {

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "status")
    private String status;

    @Column(name = "amount")
    private int amount;
}
