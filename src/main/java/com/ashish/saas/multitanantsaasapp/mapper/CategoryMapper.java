package com.ashish.saas.multitanantsaasapp.mapper;

import com.ashish.saas.multitanantsaasapp.config.TenantContext;
import com.ashish.saas.multitanantsaasapp.dto.request.CategoryRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.CategoryResponse;
import com.ashish.saas.multitanantsaasapp.entities.Category;
import org.springframework.stereotype.Service;

import static com.ashish.saas.multitanantsaasapp.dto.response.CategoryResponse.*;

@Service
public class CategoryMapper {

    public Category toEntity(final CategoryRequest request){
        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .deleted(false)
                .tenantId(TenantContext.getCurrentTenant()) // Always inject current tenant
                .build();
    }

    public CategoryResponse toResponse(final Category entity){
        return builder().
                id(entity.getId()).
                name(entity.getName()).
                description(entity.getDescription())
                .build();
    }

}
