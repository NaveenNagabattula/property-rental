package com.propertyrental.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDetailResponse {

    private UUID id;
    private String title;
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
    private BigDecimal pricePerNight;
    private Integer guestCapacity;
    private Integer bedroomCount;
    private Integer bathroomCount;
    private String propertyType;
    private String status;
    private List<String> amenities;
    private List<String> photoUrls;
    private UUID hostId;
    private String hostFirstName;
    private String hostLastName;
    private Double averageRating;
    private Integer reviewCount;
}
