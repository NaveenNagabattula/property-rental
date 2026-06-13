package com.propertyrental.api.service;

import com.propertyrental.api.dto.response.PropertyDetailResponse;
import com.propertyrental.api.dto.response.PropertySummaryResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface AdminPropertyService {

    Page<PropertySummaryResponse> getPendingListings(int page, int size);

    PropertyDetailResponse approveListing(UUID id);

    PropertyDetailResponse rejectListing(UUID id, String reason);

    PropertyDetailResponse suspendListing(UUID id, String reason);
}
