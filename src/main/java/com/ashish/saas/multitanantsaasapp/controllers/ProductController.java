package com.ashish.saas.multitanantsaasapp.controllers;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.dto.request.ProductRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.ProductResponse;
import com.ashish.saas.multitanantsaasapp.services.ProductService;
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

@RestController
@RequestMapping("api/v1/products")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Products", description = "CRUD operations for managing products within a tenant")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a new product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body — validation failed"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "409", description = "Product with the given reference already exists")
    })
    public ResponseEntity<Void> create(@Valid @RequestBody ProductRequest request) {
        this.productService.create(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{product-id}")
    @Operation(summary = "Update an existing product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body — validation failed"),
            @ApiResponse(responseCode = "404", description = "Product or Category not found"),
            @ApiResponse(responseCode = "409", description = "Product with the given reference already exists")
    })
    public ResponseEntity<Void> update(
            @Valid @RequestBody ProductRequest request,
            @Parameter(description = "UUID of the product to update")
            @PathVariable("product-id") final String id
    ) {
        this.productService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{product-id}")
    @Operation(summary = "Get a product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getById(
            @Parameter(description = "UUID of the product to retrieve")
            @PathVariable("product-id") final String id
    ) {
        return ResponseEntity.ok(this.productService.findByID(id));
    }

    @GetMapping
    @Operation(summary = "Get products (paginated)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products found",
                    content = @Content(schema = @Schema(implementation = PageResponse.class)))
    })
    public ResponseEntity<PageResponse<ProductResponse>> getAll(
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "10") final int size
    ) {
        return ResponseEntity.ok(this.productService.findAll(page, size));
    }

    @DeleteMapping("/{product-id}")
    @Operation(summary = "Delete a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID of the product to delete")
            @PathVariable("product-id") final String id
    ) {
        this.productService.delete(id);
        return ResponseEntity.ok().build();
    }
}

