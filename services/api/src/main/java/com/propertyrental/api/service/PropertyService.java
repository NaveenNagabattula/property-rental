package com.propertyrental.api.service;

import com.propertyrental.api.dto.request.CreatePropertyRequest;
import com.propertyrental.api.dto.request.PropertySearchRequest;
import com.propertyrental.api.dto.request.UpdatePropertyRequest;
import com.propertyrental.api.dto.response.PropertyDetailResponse;
import com.propertyrental.api.dto.response.PropertySummaryResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface PropertyService {

    Page<PropertySummaryResponse> searchProperties(PropertySearchRequest request);

    PropertyDetailResponse getPropertyById(UUID id);

    PropertyDetailResponse createProperty(CreatePropertyRequest request, UUID hostId);

    PropertyDetailResponse updateProperty(UUID id, UpdatePropertyRequest request, UUID requesterId);

    PropertyDetailResponse submitForReview(UUID id, UUID hostId);

    Page<PropertySummaryResponse> getHostListings(UUID hostId, int page, int size);
}
