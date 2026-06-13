package com.propertyrental.api.service;

import com.propertyrental.api.dto.response.HostApplicationResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface HostApplicationService {

    HostApplicationResponse applyForHost(UUID userId);

    Page<HostApplicationResponse> getPendingApplications(int page, int size);

    HostApplicationResponse approveApplication(UUID id);

    HostApplicationResponse rejectApplication(UUID id, String reason);
}
