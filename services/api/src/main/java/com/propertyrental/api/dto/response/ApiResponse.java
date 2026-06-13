package com.propertyrental.api.dto.response;

import lombok.Builder;
import java.time.Instant;

@Builder
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    Instant timestamp
) {
    public ApiResponse {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
