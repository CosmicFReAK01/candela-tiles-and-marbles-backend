package com.candela.media.repository;

import com.candela.media.model.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, String> {
    Optional<Media> findByFilename(String filename);
    Page<Media> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
