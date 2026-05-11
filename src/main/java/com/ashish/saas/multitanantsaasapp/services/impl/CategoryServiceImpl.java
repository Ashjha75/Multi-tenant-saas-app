package com.ashish.saas.multitanantsaasapp.services.impl;

import com.ashish.saas.multitanantsaasapp.services.CategoryService;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Override
    public void create(CategoryRequest request) {

    }

    @Override
    public void update(String id, CategoryRequest request) {

    }

    @Override
    public CategoryResponse findByID(String id) {
        return null;
    }

    @Override
    public void delete(String id) {

    }
}
