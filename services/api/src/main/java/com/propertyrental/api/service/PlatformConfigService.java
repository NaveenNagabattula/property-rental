package com.propertyrental.api.service;

import com.propertyrental.api.dto.request.UpdatePlatformConfigRequest;
import com.propertyrental.api.dto.response.PlatformConfigResponse;

public interface PlatformConfigService {

    PlatformConfigResponse getConfig();

    PlatformConfigResponse updateConfig(UpdatePlatformConfigRequest request);
}
