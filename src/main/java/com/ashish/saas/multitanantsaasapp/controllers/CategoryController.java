package com.ashish.saas.multitanantsaasapp.controllers;

import com.ashish.saas.multitanantsaasapp.dto.request.CategoryRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.CategoryResponse;
import com.ashish.saas.multitanantsaasapp.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/categories")
@Slf4j
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody CategoryRequest request) {
        this.categoryService.create(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{category-id}")
    public ResponseEntity<Void> update(@Valid @RequestBody CategoryRequest request, @PathVariable("category-id") final String id) {
        this.categoryService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("category-id")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable("category-id") final String id) {
        return ResponseEntity.ok(this.categoryService.findByID(id));
    }


}
