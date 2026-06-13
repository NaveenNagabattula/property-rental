package com.propertyrental.api.service;

import com.propertyrental.api.dto.request.UpdatePlatformConfigRequest;
import com.propertyrental.api.dto.response.PlatformConfigResponse;
import com.propertyrental.api.entity.PlatformConfig;
import com.propertyrental.api.repository.PlatformConfigRepository;
import com.propertyrental.api.service.impl.PlatformConfigServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformConfigServiceImpl Unit Tests")
class PlatformConfigServiceImplTest {

    @Mock private PlatformConfigRepository platformConfigRepository;

    @InjectMocks
    private PlatformConfigServiceImpl platformConfigService;

    @Test
    @DisplayName("getConfig — returns default values when no config exists in db")
    void getConfig_defaultValues() {
        given(platformConfigRepository.findByConfigKey(any(String.class))).willReturn(Optional.empty());

        PlatformConfigResponse config = platformConfigService.getConfig();

        assertThat(config.getServiceFeePercent()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(config.getTaxRatePercent()).isEqualByComparingTo(BigDecimal.valueOf(18));
        assertThat(config.getPayoutDelayDays()).isEqualTo(3);
        assertThat(config.getCancellationPolicy()).isEqualTo("FLEXIBLE");
    }

    @Test
    @DisplayName("getConfig — returns db values when they are present")
    void getConfig_fromDatabase() {
        given(platformConfigRepository.findByConfigKey("platform.fee.percentage")).willReturn(
                Optional.of(PlatformConfig.builder().configValue("12").build())
        );
        given(platformConfigRepository.findByConfigKey("platform.tax.rate.percentage")).willReturn(
                Optional.of(PlatformConfig.builder().configValue("15").build())
        );
        given(platformConfigRepository.findByConfigKey("platform.payout.delay.days")).willReturn(
                Optional.of(PlatformConfig.builder().configValue("5").build())
        );
        given(platformConfigRepository.findByConfigKey("platform.cancellation.policy")).willReturn(
                Optional.of(PlatformConfig.builder().configValue("STRICT").build())
        );

        PlatformConfigResponse config = platformConfigService.getConfig();

        assertThat(config.getServiceFeePercent()).isEqualByComparingTo(BigDecimal.valueOf(12));
        assertThat(config.getTaxRatePercent()).isEqualByComparingTo(BigDecimal.valueOf(15));
        assertThat(config.getPayoutDelayDays()).isEqualTo(5);
        assertThat(config.getCancellationPolicy()).isEqualTo("STRICT");
    }

    @Test
    @DisplayName("updateConfig — saves new values and returns updated config")
    void updateConfig_savesValues() {
        UpdatePlatformConfigRequest request = UpdatePlatformConfigRequest.builder()
                .serviceFeePercent(BigDecimal.valueOf(15))
                .cancellationPolicy("STRICT")
                .build();

        given(platformConfigRepository.findByConfigKey("platform.fee.percentage")).willReturn(Optional.empty());
        given(platformConfigRepository.findByConfigKey("platform.cancellation.policy")).willReturn(Optional.empty());
        given(platformConfigRepository.save(any(PlatformConfig.class))).willAnswer(inv -> inv.getArgument(0));

        platformConfigService.updateConfig(request);

        verify(platformConfigRepository, times(2)).save(any(PlatformConfig.class));
    }
}
