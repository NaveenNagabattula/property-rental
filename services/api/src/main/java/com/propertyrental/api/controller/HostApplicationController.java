package com.propertyrental.api.controller;

import com.propertyrental.api.dto.response.ApiResponse;
import com.propertyrental.api.dto.response.HostApplicationResponse;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.service.HostApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/host-applications")
@RequiredArgsConstructor
@Tag(name = "Host Applications", description = "Endpoints for users to apply for host status")
public class HostApplicationController {

    private final HostApplicationService hostApplicationService;

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Apply to become a Host (upgrade role)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<HostApplicationResponse>> applyForHost(
            @AuthenticationPrincipal User currentUser
    ) {
        HostApplicationResponse response = hostApplicationService.applyForHost(currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<HostApplicationResponse>builder()
                        .success(true)
                        .message("Host application submitted successfully. Pending administrator review.")
                        .data(response)
                        .build()
        );
    }
}
