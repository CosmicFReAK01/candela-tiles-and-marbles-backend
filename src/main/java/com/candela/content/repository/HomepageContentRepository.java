package com.candela.content.repository;

import com.candela.content.model.HomepageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HomepageContentRepository extends JpaRepository<HomepageContent, UUID> {
    Optional<HomepageContent> findBySectionKey(String sectionKey);
}
