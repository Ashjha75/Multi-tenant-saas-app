package com.ashish.saas.multitanantsaasapp.controllers;

import com.ashish.saas.multitanantsaasapp.dto.request.CategoryRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.CategoryResponse;
import com.ashish.saas.multitanantsaasapp.services.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/categories")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Categories", description = "CRUD operations for managing product/service categories within a tenant")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(
            summary = "Create a new category",
            description = "Creates a new category under the current tenant. The category name must be unique within the tenant scope."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body — validation failed"),
            @ApiResponse(responseCode = "409", description = "Category with the given name already exists")
    })
    public ResponseEntity<Void> create(@Valid @RequestBody CategoryRequest request) {
        this.categoryService.create(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{category-id}")
    @Operation(
            summary = "Update an existing category",
            description = "Updates the name and/or description of an existing category identified by its UUID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body — validation failed"),
            @ApiResponse(responseCode = "404", description = "Category not found for the given ID")
    })
    public ResponseEntity<Void> update(
            @Valid @RequestBody CategoryRequest request,
            @Parameter(description = "UUID of the category to update", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable("category-id") final String id) {
        this.categoryService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{category-id}")
    @Operation(
            summary = "Get a category by ID",
            description = "Retrieves the details of a single category identified by its UUID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category found",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found for the given ID")
    })
    public ResponseEntity<CategoryResponse> getCategory(
            @Parameter(description = "UUID of the category to retrieve", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable("category-id") final String id) {
        return ResponseEntity.ok(this.categoryService.findByID(id));
    }
    @GetMapping
    @Operation(
            summary = "Get all category ",
            description = "Retrieves the details of a all categories ."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category found",
                    content = @Content(schema = @Schema(implementation = com.ashish.saas.multitanantsaasapp.common.PageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category is Empty")
    })
    public ResponseEntity<com.ashish.saas.multitanantsaasapp.common.PageResponse<CategoryResponse>> getAllCategory(org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(this.categoryService.findAll());
    }

    @DeleteMapping("/{category-id}")
    @Operation(
            summary = "Soft-delete a category",
            description = "Marks a category as deleted (soft-delete). The record is retained in the database but excluded from queries."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found for the given ID")
    })
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "UUID of the category to delete", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable("category-id") final String id) {
        this.categoryService.delete(id);
        return ResponseEntity.ok().build();
    }
}
