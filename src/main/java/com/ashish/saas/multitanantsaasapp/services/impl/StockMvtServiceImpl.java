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
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
        checkIfProductExistsById(request.getProductId());
        final Optional<Product> product= productRepository.findById(request.getProductId());
        final StockMvt entity = this.stockMvtMapper.toEntity(request, product);
        this.stockMvtRepository.save(entity) ;
    }

    private void checkIfProductExistsById(String productId) {
        final Optional<Product>  product= productRepository.findById(productId);
        if(product.isEmpty()){
            log.error("Product not found");
            throw new AppException(HttpStatus.NOT_FOUND,"PRODUCT_NOT_FOUND","Product not found");
        }
    }

    @Override
    public void update(String id, StockMvtRequest request) {
        final Optional<StockMvt> stockMvt = stockMvtRepository.findById(id);
        if(stockMvt.isEmpty()){
            log.error("StockMvt not found");
            throw new AppException(HttpStatus.NOT_FOUND,"STOCK_MVT_NOT_FOUND","StockMvt not found");
        }
        checkIfProductExistsById(request.getProductId());
        final StockMvt updatedStockMvt= this.stockMvtMapper.toEntity(request);
        updatedStockMvt.setId(id);
        this.stockMvtRepository.save(updatedStockMvt);
    }

    @Override
    public StockMvtResponse findByID(String id) {

        return this.stockMvtRepository.findById(id).map(this.stockMvtMapper::toResponse).orElseThrow(()->new AppException(HttpStatus.NOT_FOUND,"STOCK_MVT_NOT_FOUND","StockMvt not found"));
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
        final StockMvt stockMvt= this.stockMvtRepository.findById(id).orElseThrow(()->new AppException(HttpStatus.NOT_FOUND,"STOCK_MVT_NOT_FOUND","StockMvt not found"));
         this.stockMvtRepository.delete(stockMvt);
    }
}
