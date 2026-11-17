package com.candela.inventory.controller;

import com.candela.common.dto.ApiResponse;
import com.candela.inventory.dto.InventoryDtos.*;
import com.candela.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/inventory", "/api/inventory", "/api/v1/admin/sales"})
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getAll()));
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<InventoryDto>> getByProduct(@PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getByProductId(productId)));
    }

    @PostMapping("/{productId}/restock")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<InventoryDto>> restock(
            @PathVariable String productId,
            @RequestBody RestockRequest request) {
        InventoryDto updated = inventoryService.restock(productId, request.getQuantity(), request.getNotes());
        return ResponseEntity.ok(ApiResponse.ok("Stock updated", updated));
    }

    @PostMapping("/{productId}/adjust")
    @PreAuthorize("hasAnyRole('INVENTORY_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<InventoryDto>> adjust(
            @PathVariable String productId,
            @RequestBody StockAdjustmentRequest request) {
        InventoryDto updated = inventoryService.adjustStock(
                productId,
                request.getQuantityChange(),
                request.getExpectedCurrentStock(),
                request.getNotes()
        );
        return ResponseEntity.ok(ApiResponse.ok("Stock adjusted with optimistic concurrency validation", updated));
    }
}
