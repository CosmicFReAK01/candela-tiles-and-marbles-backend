package com.candela.product.controller;

import com.candela.common.dto.ApiResponse;
import com.candela.product.dto.ProductDtos.CollectionDto;
import com.candela.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/collections", "/api/collections", "/api/v1/admin/collections"})
@RequiredArgsConstructor
public class CollectionController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CollectionDto>>> getCollections() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getCollectionsWithMetrics()));
    }
}
