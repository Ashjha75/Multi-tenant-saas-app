package com.ashish.saas.multitanantsaasapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for creating or updating a category")
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    @Schema(
            description = "Display name of the category",
            example = "Electronics",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(
            description = "Optional description providing more context about the category",
            example = "Consumer electronics including smartphones, laptops, and accessories"
    )
    private String description;
}
