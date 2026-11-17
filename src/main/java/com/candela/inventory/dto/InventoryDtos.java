package com.candela.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class InventoryDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryDto {
        private String id;
        private String productId;
        private Integer currentStock;
        private Integer reservedStock;
        private Integer minimumStockLevel;
        private String stockUnit;
        private String stockStatus;
        private Instant lastRestockedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestockRequest {
        private int quantity;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockAdjustmentRequest {
        private int quantityChange;
        private Integer expectedCurrentStock;
        private String notes;
    }
}
