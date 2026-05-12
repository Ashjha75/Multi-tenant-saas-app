package com.ashish.saas.multitanantsaasapp.controllers;

import com.ashish.saas.multitanantsaasapp.common.PageResponse;
import com.ashish.saas.multitanantsaasapp.dto.request.StockMvtRequest;
import com.ashish.saas.multitanantsaasapp.dto.response.StockMvtResponse;
import com.ashish.saas.multitanantsaasapp.services.StockMvtService;
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
@RequestMapping("api/v1/stock-mvts")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Stock Movements", description = "CRUD operations for managing stock movements")
public class StockMvtController {

    private final StockMvtService stockMvtService;

    @PostMapping
    @Operation(summary = "Create a new stock movement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock movement created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body — validation failed"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> create(@Valid @RequestBody StockMvtRequest request) {
        this.stockMvtService.create(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{stock-mvt-id}")
    @Operation(summary = "Update an existing stock movement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock movement updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body — validation failed"),
            @ApiResponse(responseCode = "404", description = "Stock movement or product not found")
    })
    public ResponseEntity<Void> update(
            @Valid @RequestBody StockMvtRequest request,
            @Parameter(description = "UUID of the stock movement to update")
            @PathVariable("stock-mvt-id") final String id
    ) {
        this.stockMvtService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{stock-mvt-id}")
    @Operation(summary = "Get a stock movement by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock movement found",
                    content = @Content(schema = @Schema(implementation = StockMvtResponse.class))),
            @ApiResponse(responseCode = "404", description = "Stock movement not found")
    })
    public ResponseEntity<StockMvtResponse> getById(
            @Parameter(description = "UUID of the stock movement to retrieve")
            @PathVariable("stock-mvt-id") final String id
    ) {
        return ResponseEntity.ok(this.stockMvtService.findByID(id));
    }

    @GetMapping
    @Operation(summary = "Get stock movements (paginated)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock movements found",
                    content = @Content(schema = @Schema(implementation = PageResponse.class)))
    })
    public ResponseEntity<PageResponse<StockMvtResponse>> getAll(
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "10") final int size
    ) {
        return ResponseEntity.ok(this.stockMvtService.findAll(page, size));
    }

    @DeleteMapping("/{stock-mvt-id}")
    @Operation(summary = "Delete a stock movement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock movement deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Stock movement not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID of the stock movement to delete")
            @PathVariable("stock-mvt-id") final String id
    ) {
        this.stockMvtService.delete(id);
        return ResponseEntity.ok().build();
    }
}

