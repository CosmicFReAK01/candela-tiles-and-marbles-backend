package com.candela.product.service;

import com.candela.product.dto.ProductDtos.*;
import com.candela.product.model.Category;
import com.candela.product.model.Collection;
import com.candela.product.model.Product;
import com.candela.product.repository.CategoryRepository;
import com.candela.product.repository.CollectionRepository;
import com.candela.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CollectionRepository collectionRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ProductDto> getPublishedProducts() {
        return productRepository.findByStatus("published", PageRequest.of(0, 100))
                .stream().map(this::toProductDto).toList();
    }

    @Transactional(readOnly = true)
    public ProductDto getProductBySlug(String slug) {
        Product p = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + slug));
        return toProductDto(p);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getFeaturedProducts() {
        return productRepository.findByIsFeaturedTrueAndStatus("published")
                .stream().map(this::toProductDto).toList();
    }

    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        String id = dto.getId() != null && !dto.getId().isBlank() ? dto.getId() : "cnd-" + UUID.randomUUID().toString().substring(0, 8);
        String sku = dto.getSku() != null && !dto.getSku().isBlank() ? dto.getSku() : "SKU-" + id.toUpperCase();
        String slug = dto.getSlug() != null && !dto.getSlug().isBlank() ? dto.getSlug() : dto.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-");

        Product p = Product.builder()
                .id(id)
                .sku(sku)
                .name(dto.getName())
                .slug(slug)
                .category(dto.getCategory())
                .masterCategory(dto.getMasterCategory())
                .subcategory(dto.getSubcategory())
                .collectionName(dto.getCollectionName())
                .collectionSlug(dto.getCollectionSlug())
                .brand(dto.getBrand())
                .material(dto.getMaterial())
                .finish(dto.getFinish())
                .colour(dto.getColour())
                .shape(dto.getShape())
                .origin(dto.getOrigin())
                .pricePerSqft(dto.getPricePerSqft())
                .salePrice(dto.getSalePrice())
                .stockSqftAvailable(dto.getStockSqftAvailable())
                .inStock(dto.getInStock() != null ? dto.getInStock() : true)
                .isFeatured(dto.getIsFeatured() != null ? dto.getIsFeatured() : false)
                .mainImageUrl(dto.getMainImageUrl())
                .status(dto.getStatus() != null ? dto.getStatus() : "published")
                .shortDescription(dto.getShortDescription())
                .description(dto.getDescription())
                .version(dto.getVersion() != null ? dto.getVersion() : 1)
                .build();

        return toProductDto(productRepository.save(p));
    }

    @Transactional
    public ProductDto updateProduct(String id, ProductDto dto) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));

        // Optimistic Locking Check: If client provided a version and it differs, throw Conflict
        if (dto.getVersion() != null && !dto.getVersion().equals(existing.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Conflict: Resource version mismatch. Expected " + existing.getVersion() + ", got " + dto.getVersion());
        }

        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getCategory() != null) existing.setCategory(dto.getCategory());
        if (dto.getMasterCategory() != null) existing.setMasterCategory(dto.getMasterCategory());
        if (dto.getSubcategory() != null) existing.setSubcategory(dto.getSubcategory());
        if (dto.getPricePerSqft() != null) existing.setPricePerSqft(dto.getPricePerSqft());
        if (dto.getSalePrice() != null) existing.setSalePrice(dto.getSalePrice());
        if (dto.getStockSqftAvailable() != null) existing.setStockSqftAvailable(dto.getStockSqftAvailable());
        if (dto.getMainImageUrl() != null) existing.setMainImageUrl(dto.getMainImageUrl());
        if (dto.getShortDescription() != null) existing.setShortDescription(dto.getShortDescription());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getInStock() != null) existing.setInStock(dto.getInStock());
        if (dto.getIsFeatured() != null) existing.setIsFeatured(dto.getIsFeatured());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());

        return toProductDto(productRepository.save(existing));
    }

    @Transactional
    public void deleteProduct(String id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CollectionDto> getCollectionsWithMetrics() {
        List<Object[]> rows = collectionRepository.findCollectionsWithProductCount();
        return rows.stream().map(row -> {
            Collection c = (Collection) row[0];
            Long count = (Long) row[1];
            return CollectionDto.builder()
                    .id(c.getId().toString())
                    .name(c.getName())
                    .slug(c.getSlug())
                    .description(c.getDescription())
                    .coverImageUrl(c.getCoverImageUrl())
                    .displayOrder(c.getDisplayOrder())
                    .isPublished(c.getIsPublished())
                    .productCount(count != null ? count : 0L)
                    .version(c.getVersion())
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toCategoryDto)
                .toList();
    }

    @Transactional
    public CategoryDto updateCategory(UUID id, CategoryDto dto) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: " + id));

        if (dto.getVersion() != null && !dto.getVersion().equals(existing.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Conflict: Category version mismatch. Expected " + existing.getVersion() + ", got " + dto.getVersion());
        }

        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getSlug() != null) existing.setSlug(dto.getSlug());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getImageUrl() != null) existing.setImageUrl(dto.getImageUrl());
        if (dto.getDisplayOrder() != null) existing.setDisplayOrder(dto.getDisplayOrder());
        if (dto.getIsActive() != null) existing.setIsActive(dto.getIsActive());

        return toCategoryDto(categoryRepository.save(existing));
    }

    private CategoryDto toCategoryDto(Category c) {
        return CategoryDto.builder()
                .id(c.getId().toString())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .displayOrder(c.getDisplayOrder())
                .isActive(c.getIsActive())
                .version(c.getVersion())
                .build();
    }

    @Transactional(readOnly = true)
    public SearchResponse search(String query) {
        if (query == null || query.isBlank()) {
            return SearchResponse.builder()
                    .query("")
                    .products(List.of())
                    .collections(List.of())
                    .count(0)
                    .timestamp(Instant.now().toString())
                    .build();
        }

        List<ProductDto> products = productRepository.searchCatalogue(query.trim(), PageRequest.of(0, 50))
                .stream().map(this::toProductDto).toList();

        List<CollectionDto> collections = collectionRepository.searchCollections(query.trim(), PageRequest.of(0, 20))
                .stream().map(c -> CollectionDto.builder()
                        .id(c.getId().toString())
                        .name(c.getName())
                        .slug(c.getSlug())
                        .description(c.getDescription())
                        .coverImageUrl(c.getCoverImageUrl())
                        .build()).toList();

        return SearchResponse.builder()
                .query(query)
                .products(products)
                .collections(collections)
                .count(products.size())
                .timestamp(Instant.now().toString())
                .build();
    }

    private ProductDto toProductDto(Product p) {
        return ProductDto.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .slug(p.getSlug())
                .category(p.getCategory())
                .masterCategory(p.getMasterCategory())
                .subcategory(p.getSubcategory())
                .collectionName(p.getCollectionName())
                .collectionSlug(p.getCollectionSlug())
                .brand(p.getBrand())
                .material(p.getMaterial())
                .finish(p.getFinish())
                .colour(p.getColour())
                .shape(p.getShape())
                .origin(p.getOrigin())
                .pricePerSqft(p.getPricePerSqft())
                .salePrice(p.getSalePrice())
                .stockSqftAvailable(p.getStockSqftAvailable())
                .inStock(p.getInStock())
                .isFeatured(p.getIsFeatured())
                .mainImageUrl(p.getMainImageUrl())
                .status(p.getStatus())
                .shortDescription(p.getShortDescription())
                .description(p.getDescription())
                .version(p.getVersion())
                .build();
    }
}
