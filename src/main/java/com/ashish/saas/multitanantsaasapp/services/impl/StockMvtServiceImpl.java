package com.ashish.saas.multitanantsaasapp.services.impl;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.dto.request.StockMvtRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.StockMvtResponse;
import com.ashish.saas.multitanantsaasapp.entities.Product;
import com.ashish.saas.multitanantsaasapp.entities.StockMvt;
import com.ashish.saas.multitanantsaasapp.exception.AppException;
import com.ashish.saas.multitanantsaasapp.mapper.StockMvtMapper;
import com.ashish.saas.multitanantsaasapp.repositories.ProductRepo;
import com.ashish.saas.multitanantsaasapp.repositories.StockMvtRepo;
import com.ashish.saas.multitanantsaasapp.services.StockMvtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class StockMvtServiceImpl implements StockMvtService {
    private final StockMvtRepo stockMvtRepository;
    private final ProductRepo productRepository;
    private final StockMvtMapper stockMvtMapper;

    @Override
    public void create(StockMvtRequest request) {
        // check if product exists
        final Product product = getProductOrThrow(request.getProductId());
        final StockMvt entity = this.stockMvtMapper.toEntity(request, product);
        this.stockMvtRepository.save(entity);
    }

    private Product getProductOrThrow(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product not found");
                    return new AppException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found");
                });
    }

    @Override
    public void update(String id, StockMvtRequest request) {
        final StockMvt existing = stockMvtRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("StockMvt not found");
                    return new AppException(HttpStatus.NOT_FOUND, "STOCK_MVT_NOT_FOUND", "StockMvt not found");
                });

        final Product product = getProductOrThrow(request.getProductId());

        // update in-place to preserve tenant/audit fields
        existing.setTypeMvt(request.getTypeMvt());
        existing.setQuantity(request.getQuantity());
        existing.setDateMvt(request.getDateMvt());
        existing.setComment(request.getComment());
        existing.setProduct(product);

        this.stockMvtRepository.save(existing);
    }

    @Override
    public StockMvtResponse findByID(String id) {
        return this.stockMvtRepository.findById(id)
                .map(this.stockMvtMapper::toResponse)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "STOCK_MVT_NOT_FOUND", "StockMvt not found"));
    }

    @Override
    public PageResponse<StockMvtResponse> findAll(int page, int size) {
        final PageRequest pageRequest = PageRequest.of(page, size);
        return PageResponse.of(
                stockMvtRepository.findAll(pageRequest)
                        .map(stockMvtMapper::toResponse)
        );
    }

    @Override
    public void delete(String id) {
        final StockMvt stockMvt = this.stockMvtRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "STOCK_MVT_NOT_FOUND", "StockMvt not found"));

        // Soft-delete — preserves audit trail; Hibernate filter (deleted = false) hides it from all future queries
        stockMvt.setDeleted(true);
        this.stockMvtRepository.save(stockMvt);
        log.debug("Soft-deleted stock movement id='{}' for tenant='{}'", id, stockMvt.getTenantId());
    }
}
