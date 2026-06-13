package com.propertyrental.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertySearchRequest {

    private String location;

    @Min(value = 1, message = "Guest count must be at least 1")
    private Integer guests;

    @Positive(message = "Minimum price must be positive")
    private BigDecimal minPrice;

    @Positive(message = "Maximum price must be positive")
    private BigDecimal maxPrice;

    private String propertyType;

    @Min(0) @Builder.Default
    private int page = 0;

    @Min(1) @Max(50) @Builder.Default
    private int size = 12;

    @Builder.Default
    private String sortBy = "pricePerNight";

    @Builder.Default
    private String sortDir = "asc";
}
