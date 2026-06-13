package com.propertyrental.api.service.impl;

import com.propertyrental.api.dto.request.CreatePropertyRequest;
import com.propertyrental.api.dto.request.PropertySearchRequest;
import com.propertyrental.api.dto.request.UpdatePropertyRequest;
import com.propertyrental.api.dto.response.PropertyDetailResponse;
import com.propertyrental.api.dto.response.PropertySummaryResponse;
import com.propertyrental.api.entity.Property;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.entity.enums.PropertyStatus;
import com.propertyrental.api.entity.enums.PropertyType;
import com.propertyrental.api.entity.enums.Role;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.exception.ResourceNotFoundException;
import com.propertyrental.api.exception.UnauthorizedException;
import com.propertyrental.api.mapper.PropertyMapper;
import com.propertyrental.api.repository.PropertyRepository;
import com.propertyrental.api.repository.UserRepository;
import com.propertyrental.api.repository.specification.PropertySpecification;
import com.propertyrental.api.service.PropertyService;
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
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyMapper propertyMapper;

    @Override
    public Page<PropertySummaryResponse> searchProperties(PropertySearchRequest request) {
        PropertyType type = null;
        if (request.getPropertyType() != null && !request.getPropertyType().isBlank()) {
            type = PropertyType.valueOf(request.getPropertyType().toUpperCase());
        }

        Specification<Property> spec = Specification
                .where(PropertySpecification.isActive())
                .and(PropertySpecification.hasLocation(request.getLocation()))
                .and(PropertySpecification.hasGuestCapacity(request.getGuests()))
                .and(PropertySpecification.hasMinPrice(request.getMinPrice()))
                .and(PropertySpecification.hasMaxPrice(request.getMaxPrice()))
                .and(PropertySpecification.hasPropertyType(type));

        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(request.getSortDir())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC,
                request.getSortBy()
        );

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        return propertyRepository.findAll(spec, pageable).map(propertyMapper::toSummaryDto);
    }

    @Override
    public PropertyDetailResponse getPropertyById(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
        return propertyMapper.toDetailDto(property);
    }

    @Override
    @Transactional
    public PropertyDetailResponse createProperty(CreatePropertyRequest request, UUID hostId) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new ResourceNotFoundException("Host not found: " + hostId));

        if (host.getRole() != Role.HOST) {
            throw new UnauthorizedException("Only users with HOST role can create listings");
        }

        Property property = propertyMapper.toEntity(request);
        property.setHost(host);
        property.setStatus(PropertyStatus.DRAFT);
        if (request.getAmenities() != null) property.setAmenities(request.getAmenities());
        if (request.getPhotoUrls() != null) property.setPhotoUrls(request.getPhotoUrls());

        Property saved = propertyRepository.save(property);
        log.info("Property created with id={} by host={}", saved.getId(), hostId);
        return propertyMapper.toDetailDto(saved);
    }

    @Override
    @Transactional
    public PropertyDetailResponse updateProperty(UUID id, UpdatePropertyRequest request, UUID requesterId) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requesterId));

        boolean isOwner = property.getHost().getId().equals(requesterId);
        boolean isAdmin = requester.getRole() == Role.SUPER_ADMIN || requester.getRole() == Role.PROPERTY_MANAGER;

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You do not have permission to update this property");
        }

        // Hosts can edit their own listings in any status except SUSPENDED.
        // Admins (SUPER_ADMIN / PROPERTY_MANAGER) can edit regardless of status.
        if (isOwner && !isAdmin) {
            if (property.getStatus() == PropertyStatus.SUSPENDED) {
                throw new BusinessRuleException(
                        "Suspended listings cannot be edited. " +
                        "Please contact support to lift the suspension before making changes."
                );
            }
        }

        if (request.getTitle() != null) property.setTitle(request.getTitle());
        if (request.getDescription() != null) property.setDescription(request.getDescription());
        if (request.getAddress() != null) property.setAddress(request.getAddress());
        if (request.getLatitude() != null) property.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) property.setLongitude(request.getLongitude());
        if (request.getPricePerNight() != null) property.setPricePerNight(request.getPricePerNight());
        if (request.getGuestCapacity() != null) property.setGuestCapacity(request.getGuestCapacity());
        if (request.getBedroomCount() != null) property.setBedroomCount(request.getBedroomCount());
        if (request.getBathroomCount() != null) property.setBathroomCount(request.getBathroomCount());
        if (request.getPropertyType() != null) property.setPropertyType(PropertyType.valueOf(request.getPropertyType()));
        if (request.getAmenities() != null) property.setAmenities(request.getAmenities());
        if (request.getPhotoUrls() != null) property.setPhotoUrls(request.getPhotoUrls());

        return propertyMapper.toDetailDto(propertyRepository.save(property));
    }

    @Override
    @Transactional
    public PropertyDetailResponse submitForReview(UUID id, UUID hostId) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));

        if (!property.getHost().getId().equals(hostId)) {
            throw new UnauthorizedException("You can only submit your own properties");
        }

        if (property.getStatus() != PropertyStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT properties can be submitted for review (current status: " + property.getStatus() + ")");
        }

        property.setStatus(PropertyStatus.PENDING_REVIEW);
        return propertyMapper.toDetailDto(propertyRepository.save(property));
    }

    @Override
    public Page<PropertySummaryResponse> getHostListings(UUID hostId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return propertyRepository.findByHostId(hostId, pageable).map(propertyMapper::toSummaryDto);
    }
}
