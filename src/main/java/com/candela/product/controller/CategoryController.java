package com.candela.product.controller;

import com.candela.common.dto.ApiResponse;
import com.candela.product.dto.ProductDtos.CategoryDto;
import com.candela.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/categories", "/api/categories", "/api/v1/admin/categories"})
@RequiredArgsConstructor
public class CategoryController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getActiveCategories()));
    }

    @PutMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategoryRoot(@RequestBody CategoryDto dto) {
        if (dto.getId() == null || dto.getId().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Category ID is required");
        }
        CategoryDto updated = productService.updateCategory(java.util.UUID.fromString(dto.getId()), dto);
        return ResponseEntity.ok(ApiResponse.ok("Category updated successfully", updated));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategoryPath(@PathVariable String id, @RequestBody CategoryDto dto) {
        CategoryDto updated = productService.updateCategory(java.util.UUID.fromString(id), dto);
        return ResponseEntity.ok(ApiResponse.ok("Category updated successfully", updated));
    }
}
