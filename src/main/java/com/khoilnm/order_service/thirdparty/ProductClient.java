package com.khoilnm.order_service.thirdparty;

import com.khoilnm.commoncore.exception.BusinessException;
import com.khoilnm.commoncore.response.BaseResponse;
import com.khoilnm.commoncore.response.BaseResponseCode;
import com.khoilnm.commoncore.response.ResponseCode;
import com.khoilnm.order_service.dto.request.DeductStockRequest;
import com.khoilnm.order_service.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.khoilnm.order_service.constant.OrderResponseCode.INSUFFICIENT_STOCK;
import static com.khoilnm.order_service.constant.OrderResponseCode.PRODUCT_NOT_FOUND;
import static com.khoilnm.order_service.constant.OrderResponseCode.PRODUCT_SERVICE_UNAVAILABLE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductClient {
    private static final Map<String, ResponseCode> ERROR_CODES = Map.of(
            "PRODUCT_STOCK400", INSUFFICIENT_STOCK,
            "PRODUCT404", PRODUCT_NOT_FOUND);

    private static final ParameterizedTypeReference<BaseResponse<List<ProductResponse>>> PRODUCTS_TYPE =
            new ParameterizedTypeReference<>() { };

    private final WebClient productWebClient;

    public List<ProductResponse> deductStock(DeductStockRequest request) {
        Mono<BaseResponse<List<ProductResponse>>> call = productWebClient.post()
                .uri("/v1/product/deduct-stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(response -> response.bodyToMono(PRODUCTS_TYPE));
        return unwrap("deduct stock", call);
    }

    private <T> T unwrap(String action, Mono<BaseResponse<T>> call) {
        BaseResponse<T> response;
        try {
            response = call.block();
        } catch (RuntimeException e) {
            // connect error, response timeout, unreadable body...
            log.error("product_service call failed ({}): {}", action, e.getMessage());
            throw businessException(PRODUCT_SERVICE_UNAVAILABLE, "Product service is unavailable");
        }

        if (response != null && BaseResponseCode.SYSTEM_SUCCESS.getCode().equals(response.getCode())) {
            return response.getData();
        }

        ResponseCode mapped = response == null ? null : ERROR_CODES.get(response.getCode());
        if (mapped != null) {
            throw businessException(mapped, response.getMessage());
        }
        String detail = response == null ? "empty response" : response.getCode() + " - " + response.getMessage();
        log.error("product_service returned an error ({}): {}", action, detail);
        throw businessException(PRODUCT_SERVICE_UNAVAILABLE, "Product service error: " + detail);
    }

    private static BusinessException businessException(ResponseCode code, String message) {
        return new BusinessException(code, message, code.getDefaultHttpStatus());
    }
}
