package com.candela.product.repository;

import com.candela.product.model.Collection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, UUID> {

    Optional<Collection> findBySlug(String slug);

    List<Collection> findByIsPublishedTrueOrderByDisplayOrderAsc();

    @Query("""
        SELECT c, (SELECT COUNT(p) FROM Product p WHERE p.collectionSlug = c.slug) as productCount
        FROM Collection c
        WHERE c.isPublished = true
        ORDER BY c.displayOrder ASC
    """)
    List<Object[]> findCollectionsWithProductCount();

    @Query("""
        SELECT c FROM Collection c
        WHERE c.isPublished = true
        AND (
            LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        ORDER BY c.displayOrder ASC
    """)
    List<Collection> searchCollections(@Param("query") String query, Pageable pageable);
}
