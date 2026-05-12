package com.ashish.saas.multitanantsaasapp.dto.request;

import com.ashish.saas.multitanantsaasapp.entities.TypeMvt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockMvtRequest {

    @NotNull(message = "Type of movement is required")
    private TypeMvt typeMvt;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @NotNull(message = "Date is required")
    private LocalDate dateMvt;

    private String comment;

    @NotBlank(message = "Product ID is required")
    private String productId;
}

