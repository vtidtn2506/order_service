package com.khoilnm.order_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Body of product_service POST /v1/product/deduct-stock. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeductStockRequest {
    private List<Item> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String productId;
        private int quantity;
    }
}
