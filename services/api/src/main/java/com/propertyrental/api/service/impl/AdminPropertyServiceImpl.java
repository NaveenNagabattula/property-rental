package com.propertyrental.api.service.impl;

import com.propertyrental.api.dto.response.PropertyDetailResponse;
import com.propertyrental.api.dto.response.PropertySummaryResponse;
import com.propertyrental.api.entity.Property;
import com.propertyrental.api.entity.enums.PropertyStatus;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.exception.ResourceNotFoundException;
import com.propertyrental.api.mapper.PropertyMapper;
import com.propertyrental.api.repository.PropertyRepository;
import com.propertyrental.api.repository.specification.PropertySpecification;
import com.propertyrental.api.service.AdminPropertyService;
import com.propertyrental.api.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPropertyServiceImpl implements AdminPropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;
    private final EmailService emailService;

    @Override
    public Page<PropertySummaryResponse> getPendingListings(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        Specification<Property> spec = Specification.where(PropertySpecification.hasStatus(PropertyStatus.PENDING_REVIEW));
        return propertyRepository.findAll(spec, pageable).map(propertyMapper::toSummaryDto);
    }

    @Override
    @Transactional
    public PropertyDetailResponse approveListing(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));

        if (property.getStatus() != PropertyStatus.PENDING_REVIEW) {
            throw new BusinessRuleException("Property is not in PENDING_REVIEW status (current: " + property.getStatus() + ")");
        }

        property.setStatus(PropertyStatus.ACTIVE);
        Property saved = propertyRepository.save(property);
        
        emailService.sendPropertyModerationDecisionEmail(
                property.getHost().getEmail(),
                property.getHost().getFirstName(),
                property.getTitle(),
                true,
                null
        );
        log.info("Property {} approved and set to ACTIVE by moderator", id);
        return propertyMapper.toDetailDto(saved);
    }

    @Override
    @Transactional
    public PropertyDetailResponse rejectListing(UUID id, String reason) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));

        if (property.getStatus() != PropertyStatus.PENDING_REVIEW) {
            throw new BusinessRuleException("Property is not in PENDING_REVIEW status (current: " + property.getStatus() + ")");
        }

        property.setStatus(PropertyStatus.DRAFT);
        Property saved = propertyRepository.save(property);

        emailService.sendPropertyModerationDecisionEmail(
                property.getHost().getEmail(),
                property.getHost().getFirstName(),
                property.getTitle(),
                false,
                reason
        );
        log.info("Property {} rejected with reason: {}. Moved back to DRAFT.", id, reason);
        return propertyMapper.toDetailDto(saved);
    }

    @Override
    @Transactional
    public PropertyDetailResponse suspendListing(UUID id, String reason) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));

        if (property.getStatus() != PropertyStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE properties can be suspended (current: " + property.getStatus() + ")");
        }

        property.setStatus(PropertyStatus.SUSPENDED);
        Property saved = propertyRepository.save(property);

        emailService.sendPropertyModerationDecisionEmail(
                property.getHost().getEmail(),
                property.getHost().getFirstName(),
                property.getTitle(),
                false,
                "Suspended: " + reason
        );
        log.info("Property {} suspended by moderator. Reason: {}", id, reason);
        return propertyMapper.toDetailDto(saved);
    }
}
