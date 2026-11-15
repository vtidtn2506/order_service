package com.khoilnm.order_service.constant;

import com.khoilnm.commoncore.response.BaseResponseCode;
import com.khoilnm.commoncore.response.ResponseCode;
import org.springframework.http.HttpStatus;

public final class OrderResponseCode {
    public static final ResponseCode BAD_REQUEST = new BaseResponseCode("ORDER400", HttpStatus.BAD_REQUEST);
    public static final ResponseCode ORDER_NOT_FOUND = new BaseResponseCode("ORDER404", HttpStatus.NOT_FOUND);
    public static final ResponseCode PRODUCT_NOT_FOUND = new BaseResponseCode("ORDER_PRODUCT404", HttpStatus.BAD_REQUEST);
    public static final ResponseCode INSUFFICIENT_STOCK = new BaseResponseCode("ORDER_STOCK400", HttpStatus.BAD_REQUEST);
    public static final ResponseCode PRODUCT_SERVICE_UNAVAILABLE = new BaseResponseCode("ORDER_PRODUCT503", HttpStatus.SERVICE_UNAVAILABLE);

    private OrderResponseCode() {
    }
}
