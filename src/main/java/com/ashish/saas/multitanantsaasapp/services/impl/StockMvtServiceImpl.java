package com.ashish.saas.multitanantsaasapp.services.impl;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.dto.request.StockMvtRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.StockMvtResponse;
import com.ashish.saas.multitanantsaasapp.services.StockMvtService;
import org.springframework.stereotype.Service;

@Service
public class StockMvtServiceImpl implements StockMvtService {
    @Override
    public void create(StockMvtRequest request) {

    }

    @Override
    public void update(String id, StockMvtRequest request) {

    }

    @Override
    public StockMvtResponse findByID(String id) {
        return null;
    }

    @Override
    public PageResponse<StockMvtResponse> findAll(int page, int size) {
        return null;
    }

    @Override
    public void delete(String id) {

    }
}
