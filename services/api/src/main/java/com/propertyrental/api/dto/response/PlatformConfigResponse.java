package com.propertyrental.api.dto.response;

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
public class PlatformConfigResponse {

    private BigDecimal serviceFeePercent;
    private BigDecimal taxRatePercent;
    private Integer payoutDelayDays;
    private String cancellationPolicy;
}
