package com.ashish.saas.multitanantsaasapp.services.impl;

import com.ashish.saas.multitanantsaasapp.dto.request.CategoryRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.CategoryResponse;
import com.ashish.saas.multitanantsaasapp.entities.Category;
import com.ashish.saas.multitanantsaasapp.exception.AppException;
import com.ashish.saas.multitanantsaasapp.mapper.CategoryMapper;
import com.ashish.saas.multitanantsaasapp.repositories.CategoryRepo;
import com.ashish.saas.multitanantsaasapp.services.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepo categoryRepo;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepo categoryRepo, CategoryMapper categoryMapper) {
        this.categoryRepo = categoryRepo;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public void create(CategoryRequest request) {
//        Check if Category exists or not
        checkIfCategoryExitsByName(request.getName());
        final Category category = categoryMapper.toEntity(request);
        categoryRepo.save(category);

    }

    @Override
    public void update(String id, CategoryRequest request) {
        final Optional<Category> existingCategory = this.categoryRepo.findById(id);
        if (existingCategory.isEmpty()) {
            log.debug("Category with id {} does not exist", id);
            throw new AppException(HttpStatus.NOT_FOUND, "Category with id " + id + " does not exist");
        }
        final Category category = existingCategory.get();
//        Check if category already exists
        if (!category.getName().equalsIgnoreCase(request.getName())) {
            checkIfCategoryExitsByName(request.getName());
        }

        final Category updatedCategory = categoryMapper.toEntity(request);
        updatedCategory.setId(category.getId());
        updatedCategory.setTenantId(category.getTenantId()); // preserve tenant — @PrePersist does NOT fire on update
        this.categoryRepo.save(updatedCategory);
    }

    @Override
    public CategoryResponse findByID(String id) {

        return this.categoryRepo.findById(id)
                .map(this.categoryMapper::toResponse)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Category with id " + id + " does not exist"));
    }

    @Override
    public void delete(String id) {
        final Category category = this.categoryRepo.findById(id).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Category with id " + id + " does not exist"));
        this.categoryRepo.delete(category);
//        or soft Delete
//        category.setDeleted(true);
    }

    private void checkIfCategoryExitsByName(String name) {

        final Optional<Category> category = this.categoryRepo.findByNameIgnoreCase(name);
        if (category.isPresent()) {
            log.debug("{} Category Already Exists", category.get().getName());
            throw new AppException(HttpStatus.CONFLICT, "CATEGORY_EXISTS", "Category already exists");
        }
    }
}
