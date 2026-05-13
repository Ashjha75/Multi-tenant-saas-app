package com.ashish.saas.multitanantsaasapp.services.impl;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.dto.request.ProductRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.ProductResponse;
import com.ashish.saas.multitanantsaasapp.entities.Category;
import com.ashish.saas.multitanantsaasapp.entities.Product;
import com.ashish.saas.multitanantsaasapp.exception.AppException;
import com.ashish.saas.multitanantsaasapp.mapper.ProductMapper;
import com.ashish.saas.multitanantsaasapp.repositories.CategoryRepo;
import com.ashish.saas.multitanantsaasapp.repositories.ProductRepo;
import com.ashish.saas.multitanantsaasapp.services.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    private final ProductRepo productRepository;
    private final CategoryRepo categoryRepository;
    private final ProductMapper productMapper;

    @Override
    public void create(ProductRequest request) {
//        Check if Product Already Exists 
        checkProductExistance(request, null);
//        Check if Category Exist or not
        final Category category = checkIfCategoryExistsById(request.getCategoryId());
        final Product product = productMapper.toEntity(request, category);
        productRepository.save(product);
    }


    @Override
    public void update(String id, ProductRequest request) {
        final Product existing = this.productRepository.findById(id)
                .orElseThrow(() -> {
                    log.debug("Product not found with id {}", id);
                    return new AppException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found");
                });

        // Check if Product Already Exists (by reference) excluding this product
        checkProductExistance(request, existing.getId());

        // Check if Category Exist or not
        final Category category = checkIfCategoryExistsById(request.getCategoryId());

        // Update in-place to preserve id, tenantId, createdAt
        existing.setName(request.getName());
        existing.setReference(request.getReference());
        existing.setDescription(request.getDescription());
        existing.setAlertThreshold(request.getAlertThreshold());
        existing.setPrice(request.getPrice());
        existing.setCategory(category);

        productRepository.save(existing);
    }

    @Override
    public ProductResponse findByID(String id) {
        return productRepository.findById(id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found"));
    }

    @Override
    public PageResponse<ProductResponse> findAll(int page, int size) {
        final PageRequest pageRequest = PageRequest.of(page, size);
        return PageResponse.of(
                productRepository.findAll(pageRequest)
                        .map(productMapper::toResponse)
        );
    }

    @Override
    public void delete(String id) {
        final Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found"));

        // Soft-delete — preserves audit trail; Hibernate filter (deleted = false) hides it from all future queries
        product.setDeleted(true);
        this.productRepository.save(product);
        log.debug("Soft-deleted product id='{}' for tenant='{}'", id, product.getTenantId());
    }

    private void checkProductExistance(ProductRequest request, String currentProductId) {
        final Optional<Product> product = this.productRepository.findByReferenceIgnoreCase(request.getReference());
        if (product.isPresent() && (currentProductId == null || !product.get().getId().equals(currentProductId))) {
            log.debug("{} already exists", request.getReference());
            throw new AppException(HttpStatus.CONFLICT, "PRODUCT_EXISTS", "Product already exists");
        }
    }

    private Category checkIfCategoryExistsById(String categoryId) {
        final Optional<Category> category = this.categoryRepository.findById(categoryId);
        if (category.isEmpty()) {
            log.debug("{} does not  exists", categoryId);
            throw new AppException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_EXISTS", "Category does not  exists");
        }
        return category.get();
    }

}
