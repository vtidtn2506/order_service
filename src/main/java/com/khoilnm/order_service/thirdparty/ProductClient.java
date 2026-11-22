package com.khoilnm.order_service.thirdparty;

import com.khoilnm.commoncore.exception.BusinessException;
import com.khoilnm.commoncore.response.BaseResponse;
import com.khoilnm.commoncore.response.BaseResponseCode;
import com.khoilnm.commoncore.response.ResponseCode;
import com.khoilnm.order_service.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * Returns the non-deleted products for the given ids, keyed by id.
     * Ids that do not exist (or are deleted) are simply absent from the map.
     */
    public Map<String, ProductResponse> getProductsByIds(Collection<String> ids) {
        Mono<BaseResponse<List<ProductResponse>>> call = productWebClient.get()
                .uri(uri -> uri.path("/v1/product").queryParam("ids", ids.toArray()).build())
                // product_service answers errors with a BaseResponse body too → decode it whatever the status
                .exchangeToMono(response -> response.bodyToMono(PRODUCTS_TYPE));
        List<ProductResponse> products = unwrap("get products", call);
        return products == null ? Map.of() : products.stream()
                .collect(Collectors.toMap(ProductResponse::getId, Function.identity()));
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
