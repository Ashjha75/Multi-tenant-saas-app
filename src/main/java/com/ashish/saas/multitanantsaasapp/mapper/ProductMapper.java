package com.ashish.saas.multitanantsaasapp.mapper;

import com.ashish.saas.multitanantsaasapp.dto.request.ProductRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.ProductResponse;
import com.ashish.saas.multitanantsaasapp.entities.Category;
import com.ashish.saas.multitanantsaasapp.entities.Product;
import org.springframework.stereotype.Service;

@Service
public class ProductMapper {

    public Product toEntity(final ProductRequest request, final Category category) {
        return Product.builder()
                .name(request.getName())
                .reference(request.getReference())
                .description(request.getDescription())
                .alertThreshold(request.getAlertThreshold())
                .price(request.getPrice())
                .category(category)
                .build();
    }

    public ProductResponse toResponse(final Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .reference(product.getReference())
                .description(product.getDescription())
                .alertThreshold(product.getAlertThreshold())
                .price(product.getPrice())
                .category(product.getCategory() != null ? product.getCategory().getName() : null)
                .build();
    }
}
