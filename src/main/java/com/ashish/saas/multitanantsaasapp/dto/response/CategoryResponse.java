package com.ashish.saas.multitanantsaasapp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Category details returned in API responses")
public class CategoryResponse {

    @Schema(description = "Unique identifier (UUID) of the category", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @Schema(description = "Display name of the category", example = "Electronics")
    private String name;

    @Schema(description = "Description providing more context about the category",
            example = "Consumer electronics including smartphones, laptops, and accessories")
    private String description;
}
