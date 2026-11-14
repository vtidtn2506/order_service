package com.khoilnm.order_service.thirdparty;

import com.khoilnm.order_service.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductClient {
    private final RestClient productRestClient;

    /**
     * Returns the non-deleted products for the given ids, keyed by id.
     * Ids that do not exist (or are deleted) are simply absent from the map.
     */
    public Map<String, ProductResponse> getProductsByIds(Collection<String> ids) {
        try {
            List<ProductResponse> products = productRestClient.get()
                    .uri(uri -> uri.path("/v1/product").queryParam("ids", ids.toArray()).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return products == null ? Map.of() : products.stream()
                    .collect(Collectors.toMap(ProductResponse::getId, Function.identity()));
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product service is unavailable", e);
        }
    }
}
