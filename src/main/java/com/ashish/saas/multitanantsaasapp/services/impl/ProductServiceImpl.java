package com.ashish.saas.multitanantsaasapp.services.impl;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.dto.request.ProductRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.ProductResponse;
import com.ashish.saas.multitanantsaasapp.services.ProductService;

public class ProductServiceImpl implements ProductService {
    @Override
    public void create(ProductRequest request) {

    }

    @Override
    public void update(String id, ProductRequest request) {

    }

    @Override
    public ProductResponse findByID(String id) {
        return null;
    }

    @Override
    public PageResponse<ProductResponse> findAll() {
        return null;
    }

    @Override
    public void delete(String id) {

    }
}
