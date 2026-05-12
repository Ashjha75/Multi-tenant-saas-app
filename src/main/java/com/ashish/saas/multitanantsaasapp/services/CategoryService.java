package com.ashish.saas.multitanantsaasapp.services;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.dto.request.CategoryRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.CategoryResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryService extends BasicService<CategoryRequest, CategoryResponse>{
    void create(CategoryRequest request);

}
