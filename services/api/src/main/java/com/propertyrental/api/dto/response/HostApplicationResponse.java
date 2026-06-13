package com.propertyrental.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostApplicationResponse {

    private UUID id;
    private UUID userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private String status;
    private String reason;
    private Instant createdDate;
}
