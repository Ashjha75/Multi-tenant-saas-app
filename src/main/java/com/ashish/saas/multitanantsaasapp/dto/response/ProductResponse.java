package com.ashish.saas.multitanantsaasapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private String id;
    private String name;
    private String reference;
    private String description;
    private Integer alertThreshold;
    private BigDecimal price;
    private String category;
}

