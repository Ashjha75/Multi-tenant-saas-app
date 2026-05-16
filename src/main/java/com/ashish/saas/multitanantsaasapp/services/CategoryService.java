package com.ashish.saas.multitanantsaasapp.services;

import com.ashish.saas.multitanantsaasapp.dto.request.CategoryRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.CategoryResponse;

public interface CategoryService extends BasicService<CategoryRequest, CategoryResponse> {
    void create(CategoryRequest request);

}
