package com.propertyrental.api.repository;

import com.propertyrental.api.entity.Property;
import com.propertyrental.api.entity.enums.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

    Page<Property> findByHostId(UUID hostId, Pageable pageable);

    List<Property> findByHostIdAndStatus(UUID hostId, PropertyStatus status);
}
