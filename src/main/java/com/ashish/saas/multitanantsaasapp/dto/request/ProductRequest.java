package com.ashish.saas.multitanantsaasapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Reference is required")
    private String reference;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Alert threshold is required")
    @PositiveOrZero(message = "Alert threshold must be zero or positive")
    private Integer alertThreshold;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be zero or positive")
    private BigDecimal price;

    @NotBlank(message = "Category ID is required")
    private String categoryId;
}

