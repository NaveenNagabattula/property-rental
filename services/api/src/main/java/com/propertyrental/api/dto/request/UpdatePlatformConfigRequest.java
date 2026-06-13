package com.propertyrental.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class UpdatePlatformConfigRequest {

    @Min(value = 0, message = "Service fee cannot be negative")
    @Max(value = 100, message = "Service fee cannot exceed 100%")
    private BigDecimal serviceFeePercent;

    @Min(value = 0, message = "Tax rate cannot be negative")
    @Max(value = 100, message = "Tax rate cannot exceed 100%")
    private BigDecimal taxRatePercent;

    @Min(value = 0, message = "Payout delay cannot be negative")
    private Integer payoutDelayDays;

    private String cancellationPolicy;
}
