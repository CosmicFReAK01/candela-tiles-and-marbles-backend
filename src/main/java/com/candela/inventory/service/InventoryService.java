package com.candela.inventory.service;

import com.candela.inventory.dto.InventoryDtos.InventoryDto;
import com.candela.inventory.model.Inventory;
import com.candela.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<InventoryDto> getAll() {
        return inventoryRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public InventoryDto getByProductId(String productId) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found for product: " + productId));
        return toDto(inv);
    }

    @Transactional
    public InventoryDto restock(String productId, int quantity, String notes) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> Inventory.builder().productId(productId).currentStock(0).build());

        inv.setCurrentStock(inv.getCurrentStock() + quantity);
        inv.setLastRestockedAt(Instant.now());
        inv.setStockStatus(inv.getCurrentStock() > 0 ? "IN_STOCK" : "OUT_OF_STOCK");

        return toDto(inventoryRepository.save(inv));
    }

    @Transactional
    public InventoryDto adjustStock(String productId, int quantityChange, Integer expectedCurrentStock, String notes) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found for product: " + productId));

        // Optimistic Locking Check: Compare-and-Swap on expected stock level
        if (expectedCurrentStock != null && !expectedCurrentStock.equals(inv.getCurrentStock())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Conflict: Inventory stock mismatch. Expected " + expectedCurrentStock + ", but current is " + inv.getCurrentStock());
        }

        int newStock = inv.getCurrentStock() + quantityChange;
        if (newStock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock. Current: " + inv.getCurrentStock() + ", Change: " + quantityChange);
        }

        inv.setCurrentStock(newStock);
        inv.setStockStatus(newStock > 0 ? "IN_STOCK" : "OUT_OF_STOCK");
        inv.setUpdatedAt(Instant.now());

        return toDto(inventoryRepository.save(inv));
    }

    private InventoryDto toDto(Inventory i) {
        return InventoryDto.builder()
                .id(i.getId().toString())
                .productId(i.getProductId())
                .currentStock(i.getCurrentStock())
                .reservedStock(i.getReservedStock())
                .minimumStockLevel(i.getMinimumStockLevel())
                .stockUnit(i.getStockUnit())
                .stockStatus(i.getStockStatus())
                .lastRestockedAt(i.getLastRestockedAt())
                .build();
    }
}
