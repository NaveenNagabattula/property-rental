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
public class ReviewResponse {

    private UUID id;
    private UUID bookingId;
    private UUID propertyId;
    private String propertyTitle;
    private UUID guestId;
    private String guestFirstName;
    private String guestLastName;
    private Integer rating;
    private String comment;
    private Instant createdDate;
}
