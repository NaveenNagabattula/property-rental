package com.propertyrental.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertySummaryResponse {

    private UUID id;
    private String title;
    private String address;
    private Double latitude;
    private Double longitude;
    private BigDecimal pricePerNight;
    private Integer guestCapacity;
    private Integer bedroomCount;
    private Integer bathroomCount;
    private String propertyType;
    private String status;
    private String thumbnailUrl;
    private Double averageRating;
    private Integer reviewCount;
}
