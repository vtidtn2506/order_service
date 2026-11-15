package com.khoilnm.order_service.thirdparty;

import com.khoilnm.commoncore.exception.BusinessException;
import com.khoilnm.commoncore.response.BaseResponse;
import com.khoilnm.commoncore.response.BaseResponseCode;
import com.khoilnm.order_service.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.khoilnm.order_service.constant.OrderResponseCode.PRODUCT_SERVICE_UNAVAILABLE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductClient {
    private final RestClient productRestClient;

    /**
     * Returns the non-deleted products for the given ids, keyed by id.
     * Ids that do not exist (or are deleted) are simply absent from the map.
     */
    public Map<String, ProductResponse> getProductsByIds(Collection<String> ids) {
        BaseResponse<List<ProductResponse>> response;
        try {
            response = productRestClient.get()
                    .uri(uri -> uri.path("/v1/product").queryParam("ids", ids.toArray()).build())
                    .retrieve()
                    // product_service returns a BaseResponse body for errors too → read it instead of throwing
                    .onStatus(HttpStatusCode::isError, (req, res) -> { })
                    .body(new ParameterizedTypeReference<BaseResponse<List<ProductResponse>>>() { });
        } catch (RestClientException e) {
            log.error("Call product_service failed: {}", e.getMessage(), e);
            throw unavailable("Product service is unavailable");
        }

        if (response == null || !BaseResponseCode.SYSTEM_SUCCESS.getCode().equals(response.getCode())) {
            String detail = response == null ? "empty response" : response.getCode() + " - " + response.getMessage();
            log.error("product_service returned an error: {}", detail);
            throw unavailable("Product service error: " + detail);
        }

        List<ProductResponse> products = response.getData();
        return products == null ? Map.of() : products.stream()
                .collect(Collectors.toMap(ProductResponse::getId, Function.identity()));
    }

    private static BusinessException unavailable(String message) {
        return new BusinessException(PRODUCT_SERVICE_UNAVAILABLE, message, PRODUCT_SERVICE_UNAVAILABLE.getDefaultHttpStatus());
    }
}
