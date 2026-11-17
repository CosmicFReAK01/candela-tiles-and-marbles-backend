package com.candela.product.controller;

import com.candela.product.dto.ProductDtos.SearchResponse;
import com.candela.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/search", "/api/search"})
@RequiredArgsConstructor
public class SearchController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<SearchResponse> search(@RequestParam(value = "q", required = false) String q) {
        SearchResponse response = productService.search(q);
        return ResponseEntity.ok(response);
    }
}
