package com.propertyrental.api.controller;

import com.propertyrental.api.dto.request.ChangeRoleRequest;
import com.propertyrental.api.dto.request.ModerationActionRequest;
import com.propertyrental.api.dto.request.UpdatePlatformConfigRequest;
import com.propertyrental.api.dto.response.ApiResponse;
import com.propertyrental.api.dto.response.HostApplicationResponse;
import com.propertyrental.api.dto.response.PlatformConfigResponse;
import com.propertyrental.api.dto.response.PropertyDetailResponse;
import com.propertyrental.api.dto.response.PropertySummaryResponse;
import com.propertyrental.api.dto.response.UserResponse;
import com.propertyrental.api.service.AdminPropertyService;
import com.propertyrental.api.service.HostApplicationService;
import com.propertyrental.api.service.PlatformConfigService;
import com.propertyrental.api.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administration and platform moderation operations")
public class AdminController {

    private final AdminPropertyService adminPropertyService;
    private final HostApplicationService hostApplicationService;
    private final UserManagementService userManagementService;
    private final PlatformConfigService platformConfigService;

    // --- PROPERTY MODERATION ---

    @GetMapping("/listings/pending")
    @PreAuthorize("hasAnyRole('PROPERTY_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Get paginated listing moderation queue (PENDING_REVIEW)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>> getPendingListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Admin portal access verified for user={} via listings/pending", principal);
        Page<PropertySummaryResponse> listings = adminPropertyService.getPendingListings(page, size);
        return ResponseEntity.ok(ApiResponse.<Page<PropertySummaryResponse>>builder()
                .success(true)
                .message("Pending listings retrieved successfully")
                .data(listings)
                .build());
    }

    @PostMapping("/listings/{id}/approve")
    @PreAuthorize("hasAnyRole('PROPERTY_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Approve a pending listing", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> approveListing(@PathVariable UUID id) {
        PropertyDetailResponse response = adminPropertyService.approveListing(id);
        return ResponseEntity.ok(ApiResponse.<PropertyDetailResponse>builder()
                .success(true)
                .message("Property approved successfully")
                .data(response)
                .build());
    }

    @PostMapping("/listings/{id}/reject")
    @PreAuthorize("hasAnyRole('PROPERTY_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Reject a pending listing", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> rejectListing(
            @PathVariable UUID id,
            @Valid @RequestBody ModerationActionRequest request
    ) {
        PropertyDetailResponse response = adminPropertyService.rejectListing(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.<PropertyDetailResponse>builder()
                .success(true)
                .message("Property rejected successfully")
                .data(response)
                .build());
    }

    @PostMapping("/listings/{id}/suspend")
    @PreAuthorize("hasAnyRole('PROPERTY_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Suspend an active listing", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> suspendListing(
            @PathVariable UUID id,
            @Valid @RequestBody ModerationActionRequest request
    ) {
        PropertyDetailResponse response = adminPropertyService.suspendListing(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.<PropertyDetailResponse>builder()
                .success(true)
                .message("Property suspended successfully")
                .data(response)
                .build());
    }

    // --- USER MANAGEMENT ---

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('PROPERTY_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Search and list all users (paginated)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<UserResponse> users = userManagementService.searchUsers(query, role, isActive, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<UserResponse>>builder()
                .success(true)
                .message("Users retrieved successfully")
                .data(users)
                .build());
    }

    @PostMapping("/users/{id}/deactivate")
    @PreAuthorize("hasAnyRole('PROPERTY_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Deactivate a user account and invalidate tokens", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable UUID id) {
        UserResponse response = userManagementService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User account deactivated and active sessions invalidated")
                .data(response)
                .build());
    }

    @PostMapping("/users/{id}/activate")
    @PreAuthorize("hasAnyRole('PROPERTY_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Reactivate a previously deactivated user account", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable UUID id) {
        UserResponse response = userManagementService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User account reactivated successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Permanently delete a user account (Super Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userManagementService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("User account permanently deleted")
                .data(null)
                .build());
    }

    @PatchMapping("/users/{id}/change-role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Change a user's role (Super Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        UserResponse response = userManagementService.changeRole(id, request);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User role updated successfully")
                .data(response)
                .build());
    }

    // --- HOST APPLICATIONS ---

    @GetMapping("/host-applications")
    @PreAuthorize("hasAnyRole('PROPERTY_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Get paginated host applications for review", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Page<HostApplicationResponse>>> getPendingHostApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<HostApplicationResponse> apps = hostApplicationService.getPendingApplications(page, size);
        return ResponseEntity.ok(ApiResponse.<Page<HostApplicationResponse>>builder()
                .success(true)
                .message("Pending host applications retrieved successfully")
                .data(apps)
                .build());
    }

    @PostMapping("/host-applications/{id}/approve")
    @PreAuthorize("hasAnyRole('PROPERTY_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Approve a host application and upgrade user to HOST role", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<HostApplicationResponse>> approveHostApplication(@PathVariable UUID id) {
        HostApplicationResponse response = hostApplicationService.approveApplication(id);
        return ResponseEntity.ok(ApiResponse.<HostApplicationResponse>builder()
                .success(true)
                .message("Host application approved successfully")
                .data(response)
                .build());
    }

    @PostMapping("/host-applications/{id}/reject")
    @PreAuthorize("hasAnyRole('PROPERTY_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Reject a host application with a justification", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<HostApplicationResponse>> rejectHostApplication(
            @PathVariable UUID id,
            @Valid @RequestBody ModerationActionRequest request
    ) {
        HostApplicationResponse response = hostApplicationService.rejectApplication(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.<HostApplicationResponse>builder()
                .success(true)
                .message("Host application rejected successfully")
                .data(response)
                .build());
    }

    // --- PLATFORM CONFIGURATION ---

    @GetMapping("/config")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get current platform configuration (Super Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PlatformConfigResponse>> getConfig() {
        PlatformConfigResponse config = platformConfigService.getConfig();
        return ResponseEntity.ok(ApiResponse.<PlatformConfigResponse>builder()
                .success(true)
                .message("Platform configuration retrieved successfully")
                .data(config)
                .build());
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update platform configuration (Super Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PlatformConfigResponse>> updateConfig(
            @Valid @RequestBody UpdatePlatformConfigRequest request
    ) {
        PlatformConfigResponse config = platformConfigService.updateConfig(request);
        return ResponseEntity.ok(ApiResponse.<PlatformConfigResponse>builder()
                .success(true)
                .message("Platform configuration updated successfully")
                .data(config)
                .build());
    }
}
