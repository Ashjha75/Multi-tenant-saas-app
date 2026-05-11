package com.ashish.saas.multitanantsaasapp.services.impl;

import com.ashish.saas.multitanantsaasapp.dto.request.CategoryRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.CategoryResponse;
import com.ashish.saas.multitanantsaasapp.mapper.CategoryMapper;
import com.ashish.saas.multitanantsaasapp.repositories.CategoryRepo;
import com.ashish.saas.multitanantsaasapp.services.CategoryService;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl implements CategoryService {

    private  final CategoryRepo categoryRepo;
    private  final CategoryMapper categoryMapper;
    public CategoryServiceImpl(CategoryRepo categoryRepo) {
        this.categoryRepo = categoryRepo;
    }
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
