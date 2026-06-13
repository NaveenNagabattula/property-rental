package com.propertyrental.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreatePropertyRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal pricePerNight;

    @NotNull(message = "Guest capacity is required")
    @Min(value = 1, message = "Guest capacity must be at least 1")
    private Integer guestCapacity;

    @NotNull(message = "Bedroom count is required")
    @Min(value = 0, message = "Bedroom count cannot be negative")
    private Integer bedroomCount;

    @NotNull(message = "Bathroom count is required")
    @Min(value = 0, message = "Bathroom count cannot be negative")
    private Integer bathroomCount;

    @NotBlank(message = "Property type is required")
    private String propertyType;

    private List<String> amenities;

    @Size(max = 20, message = "Maximum 20 photos allowed")
    private List<String> photoUrls;
}
