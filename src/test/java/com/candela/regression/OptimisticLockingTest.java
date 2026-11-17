package com.candela.regression;

import com.candela.inventory.model.Inventory;
import com.candela.inventory.repository.InventoryRepository;
import com.candela.inventory.service.InventoryService;
import com.candela.product.dto.ProductDtos.ProductDto;
import com.candela.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class OptimisticLockingTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("Product update rejects stale version with 409 Conflict")
    void detectsStaleVersionConflict() {
        ProductDto initial = ProductDto.builder()
                .id("cnd-tile-occ-test")
                .name("Onyx Translucent")
                .version(1)
                .build();
        ProductDto created = productService.createProduct(initial);
        assertThat(created.getVersion()).isEqualTo(1);

        // Attempting update with stale version 99 must trigger Conflict
        ProductDto staleUpdate = ProductDto.builder()
                .name("Onyx Modified Stale")
                .version(99)
                .build();

        assertThatThrownBy(() -> productService.updateProduct(created.getId(), staleUpdate))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Conflict");
    }

    @Test
    @DisplayName("Inventory adjustment rejects stale expected stock level with 409 Conflict")
    void inventoryAdjustmentOptimisticLocking() {
        inventoryRepository.save(Inventory.builder()
                .productId("prod-occ-test-1")
                .currentStock(100)
                .build());

        // Concurrent edit 1: someone changes stock to 120
        inventoryService.restock("prod-occ-test-1", 20, "Batch restock");

        // Concurrent edit 2: client thought stock was 100, attempts to adjust -30 with expectedCurrentStock=100
        assertThatThrownBy(() -> inventoryService.adjustStock("prod-occ-test-1", -30, 100, "Sale reservation"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Conflict: Inventory stock mismatch. Expected 100, but current is 120");

        // When client provides updated expected stock (120), adjustment succeeds
        var adjusted = inventoryService.adjustStock("prod-occ-test-1", -30, 120, "Sale reservation confirmed");
        assertThat(adjusted.getCurrentStock()).isEqualTo(90);
    }
}
