package com.propertyrental.api.repository;

import com.propertyrental.api.entity.RefundRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {

    Page<RefundRequest> findByStatus(String status, Pageable pageable);

    boolean existsByBookingIdAndStatus(UUID bookingId, String status);
}
