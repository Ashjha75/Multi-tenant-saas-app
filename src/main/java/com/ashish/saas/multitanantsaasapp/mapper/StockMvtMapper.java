package com.ashish.saas.multitanantsaasapp.mapper;

import com.ashish.saas.multitanantsaasapp.dto.request.StockMvtRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.StockMvtResponse;
import com.ashish.saas.multitanantsaasapp.entities.Product;
import com.ashish.saas.multitanantsaasapp.entities.StockMvt;
import org.springframework.stereotype.Service;

@Service
public class StockMvtMapper {

    public StockMvt toEntity(final StockMvtRequest request, final Product product) {
        return StockMvt.builder()
                .typeMvt(request.getTypeMvt())
                .quantity(request.getQuantity())
                .dateMvt(request.getDateMvt())
                .comment(request.getComment())
                .product(product)
                .deleted(false)
                .build();
    }

    public StockMvtResponse toResponse(final StockMvt entity) {
        return StockMvtResponse.builder()
                .id(entity.getId())
                .typeMvt(entity.getTypeMvt())
                .quantity(entity.getQuantity())
                .dateMvt(entity.getDateMvt())
                .comment(entity.getComment())
                .productId(entity.getProduct() != null ? entity.getProduct().getId() : null)
                .build();
    }
}

