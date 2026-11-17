package com.candela.product.repository;

import com.candela.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySku(String sku);

    Page<Product> findByStatus(String status, Pageable pageable);

    List<Product> findByIsFeaturedTrueAndStatus(String status);

    @Query("""
        SELECT p FROM Product p
        WHERE p.status = 'published'
        AND (
            LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(COALESCE(p.category, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(COALESCE(p.masterCategory, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(COALESCE(p.subcategory, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(COALESCE(p.material, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(COALESCE(p.colour, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(COALESCE(p.shape, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(COALESCE(p.finish, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(COALESCE(p.origin, '')) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        ORDER BY p.isFeatured DESC, p.updatedAt DESC
    """)
    List<Product> searchCatalogue(@Param("query") String query, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.collectionSlug = :slug")
    long countByCollectionSlug(@Param("slug") String slug);
}
