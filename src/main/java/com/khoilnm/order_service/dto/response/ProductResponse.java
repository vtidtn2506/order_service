package com.khoilnm.order_service.dto.response;

import lombok.Getter;
import lombok.Setter;

/** Response of product_service GET /v1/product. */
@Getter
@Setter
public class ProductResponse {
    private String id;

    private String title;

    private String categoryId;

    private int price;

    private int stock;
}
