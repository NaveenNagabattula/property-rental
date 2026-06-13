package com.propertyrental.api.controller;

import com.propertyrental.api.dto.request.CreatePropertyRequest;
import com.propertyrental.api.dto.request.PropertySearchRequest;
import com.propertyrental.api.dto.request.UpdatePropertyRequest;
import com.propertyrental.api.dto.response.ApiResponse;
import com.propertyrental.api.dto.response.PropertyDetailResponse;
import com.propertyrental.api.dto.response.PropertySummaryResponse;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@Tag(name = "Properties", description = "Property search, listing management and host operations")
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    @Operation(summary = "Search and filter properties", description = "Public endpoint — returns paginated ACTIVE listings matching filters")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> searchProperties(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false) String propertyType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "pricePerNight") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        PropertySearchRequest request = PropertySearchRequest.builder()
                .location(location)
                .guests(guests)
                .minPrice(minPrice != null ? new java.math.BigDecimal(minPrice) : null)
                .maxPrice(maxPrice != null ? new java.math.BigDecimal(maxPrice) : null)
                .propertyType(propertyType)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();

        Page<PropertySummaryResponse> results = propertyService.searchProperties(request);
        return ResponseEntity.ok(ApiResponse.<Page<PropertySummaryResponse>>builder()
                .success(true)
                .message("Properties retrieved successfully")
                .data(results)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get property details by ID", description = "Public endpoint — returns the full property listing detail")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> getPropertyById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<PropertyDetailResponse>builder()
                .success(true)
                .message("Property retrieved successfully")
                .data(propertyService.getPropertyById(id))
                .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Create a new property listing", description = "HOST only — creates a listing in DRAFT status",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> createProperty(
            @Valid @RequestBody CreatePropertyRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<PropertyDetailResponse>builder()
                        .success(true)
                        .message("Property listing created successfully")
                        .data(propertyService.createProperty(request, currentUser.getId()))
                        .build()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOST','PROPERTY_MANAGER','SUPER_ADMIN')")
    @Operation(summary = "Update a property listing", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> updateProperty(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePropertyRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.<PropertyDetailResponse>builder()
                .success(true)
                .message("Property updated successfully")
                .data(propertyService.updateProperty(id, request, currentUser.getId()))
                .build());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Submit listing for review", description = "Transitions property from DRAFT to PENDING_REVIEW",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> submitForReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.<PropertyDetailResponse>builder()
                .success(true)
                .message("Property submitted for review")
                .data(propertyService.submitForReview(id, currentUser.getId()))
                .build());
    }

    @GetMapping("/host/my-listings")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Get host's own listings", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> getMyListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.<Page<PropertySummaryResponse>>builder()
                .success(true)
                .message("Host listings retrieved successfully")
                .data(propertyService.getHostListings(currentUser.getId(), page, size))
                .build());
    }
}
