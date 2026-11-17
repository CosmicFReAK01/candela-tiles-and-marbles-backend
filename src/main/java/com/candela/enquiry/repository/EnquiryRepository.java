package com.candela.enquiry.repository;

import com.candela.enquiry.model.Enquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, UUID> {
    Optional<Enquiry> findByReferenceNo(String referenceNo);
    Page<Enquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
