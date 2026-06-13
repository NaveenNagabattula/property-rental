package com.propertyrental.api.repository;

import com.propertyrental.api.entity.HostApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HostApplicationRepository extends JpaRepository<HostApplication, UUID> {

    Optional<HostApplication> findByUserIdAndStatus(UUID userId, String status);

    Page<HostApplication> findByStatus(String status, Pageable pageable);

    boolean existsByUserIdAndStatus(UUID userId, String status);
}
