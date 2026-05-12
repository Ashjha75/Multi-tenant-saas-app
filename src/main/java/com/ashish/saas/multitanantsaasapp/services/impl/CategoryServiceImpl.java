package com.ashish.saas.multitanantsaasapp.services.impl;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.config.TenantContext;
import com.ashish.saas.multitanantsaasapp.dto.request.CategoryRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.CategoryResponse;
import com.ashish.saas.multitanantsaasapp.entities.Category;
import com.ashish.saas.multitanantsaasapp.exception.AppException;
import com.ashish.saas.multitanantsaasapp.mapper.CategoryMapper;
import com.ashish.saas.multitanantsaasapp.repositories.CategoryRepo;
import com.ashish.saas.multitanantsaasapp.services.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepo categoryRepo;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepo categoryRepo, CategoryMapper categoryMapper) {
        this.categoryRepo = categoryRepo;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public void create(CategoryRequest request) {
        // Name check is tenant-scoped — no cross-tenant collision possible
        checkIfCategoryExistsByName(request.getName());
        final Category category = categoryMapper.toEntity(request); // tenantId injected by mapper via TenantContext
        categoryRepo.save(category);
    }

    @Override
    public void update(String id, CategoryRequest request) {
        final Category category = this.categoryRepo.findById(id)
                .orElseThrow(() -> {
                    log.debug("Category with id {} does not exist", id);
                    return new AppException(HttpStatus.NOT_FOUND, "Category with id " + id + " does not exist");
                });

        // IDOR (Insecure Direct Object Reference) protection — verify tenant ownership
        assertTenantOwnership(category.getTenantId(), id);

        // Only check name uniqueness if the name is actually changing
        if (!category.getName().equalsIgnoreCase(request.getName())) {
            checkIfCategoryExistsByName(request.getName());
        }

        // Update in-place — preserves id, tenantId, createdAt automatically
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        this.categoryRepo.save(category);
    }

    @Override
    public CategoryResponse findByID(String id) {
        return this.categoryRepo.findById(id)
                .map(category -> {
                    // IDOR protection on read
                    assertTenantOwnership(category.getTenantId(), id);
                    return this.categoryMapper.toResponse(category);
                })
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Category with id " + id + " does not exist"));
    }

    @Override
    public PageResponse<CategoryResponse> findAll() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Category> page = this.categoryRepo.findAll(pageable);

        List<CategoryResponse> content = page.getContent()
                .stream()
                .map(this.categoryMapper::toResponse)
                .toList();

        return PageResponse.<CategoryResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    public void delete(String id) {
        final Category category = this.categoryRepo.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Category with id " + id + " does not exist"));

        // IDOR protection on delete
        assertTenantOwnership(category.getTenantId(), id);

        // Soft-delete — preserves audit trail; Hibernate filter (deleted = false) hides it from all future queries
        category.setDeleted(true);
        this.categoryRepo.save(category);
        log.debug("Soft-deleted category id='{}' for tenant='{}'", id, category.getTenantId());
    }

    // ── private helpers ───────────────────────────────────────────────────

    /**
     * Verifies that the current tenant in context matches the entity's owning tenant.
     * Returns 404 instead of 403 to avoid leaking the existence of another tenant's resource.
     */
    private void assertTenantOwnership(String entityTenantId, String resourceId) {
        final String currentTenant = TenantContext.getCurrentTenant();
        if (!currentTenant.equals(entityTenantId)) {
            log.warn("[TENANT VIOLATION] Tenant '{}' attempted to access resource '{}' owned by tenant '{}'",
                    currentTenant, resourceId, entityTenantId);
            // Intentional 404 — do NOT reveal that the resource exists for a different tenant
            throw new AppException(HttpStatus.NOT_FOUND, "Category with id " + resourceId + " does not exist");
        }
    }

    /**
     * Checks if a category with the given name already exists within the current tenant.
     * Uses explicit JPQL query in repo to guarantee tenant isolation.
     */
    private void checkIfCategoryExistsByName(String name) {
        final String tenantId = TenantContext.getCurrentTenant();
        final Optional<Category> existing = this.categoryRepo.findByNameIgnoreCaseAndTenant(name, tenantId);
        if (existing.isPresent()) {
            log.debug("[TENANT={}] Category '{}' already exists", tenantId, name);
            throw new AppException(HttpStatus.CONFLICT, "CATEGORY_EXISTS", "Category already exists");
        }
    }
}
