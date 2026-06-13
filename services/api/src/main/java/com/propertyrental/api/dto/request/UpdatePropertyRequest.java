package com.propertyrental.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePropertyRequest {

    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private String address;
    private Double latitude;
    private Double longitude;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal pricePerNight;

    @Min(value = 1, message = "Guest capacity must be at least 1")
    private Integer guestCapacity;

    @Min(value = 0)
    private Integer bedroomCount;

    @Min(value = 0)
    private Integer bathroomCount;

    private String propertyType;
    private List<String> amenities;
    private List<String> photoUrls;
}
