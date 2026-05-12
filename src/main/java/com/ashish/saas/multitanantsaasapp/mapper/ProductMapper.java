package com.ashish.saas.multitanantsaasapp.mapper;

import com.ashish.saas.multitanantsaasapp.dto.request.ProductRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.ProductResponse;
import com.ashish.saas.multitanantsaasapp.entities.Category;
import com.ashish.saas.multitanantsaasapp.entities.Product;
import org.springframework.stereotype.Service;

@Service
public class ProductMapper {

    public Product toEntity(final ProductRequest request) {
        return Product.builder()
                .name(request.getName())
                .reference(request.getReference())
                .description(request.getDescription())
                .alertThreshold(request.getAlertThreshold())
                .price(request.getPrice())
                .category(Category.builder().id(request.getCategoryId()).build())
                .build();
    }

    public ProductResponse toResponse(final Product product) {
        return ProductResponse.builder()
                .name(product.getName())
                .reference(product.getReference())
                .description(product.getDescription())
                .alertThreshold(product.getAlertThreshold())
                .price(product.getPrice())
                .category(product.getCategory().getName())
                .build();
    }
}
