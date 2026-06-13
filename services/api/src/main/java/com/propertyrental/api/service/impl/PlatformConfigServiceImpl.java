package com.propertyrental.api.service.impl;

import com.propertyrental.api.dto.request.UpdatePlatformConfigRequest;
import com.propertyrental.api.dto.response.PlatformConfigResponse;
import com.propertyrental.api.entity.PlatformConfig;
import com.propertyrental.api.repository.PlatformConfigRepository;
import com.propertyrental.api.service.PlatformConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformConfigServiceImpl implements PlatformConfigService {

    private final PlatformConfigRepository platformConfigRepository;

    private static final String KEY_FEE = "platform.fee.percentage";
    private static final String KEY_TAX = "platform.tax.rate.percentage";
    private static final String KEY_PAYOUT = "platform.payout.delay.days";
    private static final String KEY_POLICY = "platform.cancellation.policy";

    @Override
    public PlatformConfigResponse getConfig() {
        BigDecimal fee = new BigDecimal(getValue(KEY_FEE, "10"));
        BigDecimal tax = new BigDecimal(getValue(KEY_TAX, "18"));
        int payout = Integer.parseInt(getValue(KEY_PAYOUT, "3"));
        String policy = getValue(KEY_POLICY, "FLEXIBLE");

        return PlatformConfigResponse.builder()
                .serviceFeePercent(fee)
                .taxRatePercent(tax)
                .payoutDelayDays(payout)
                .cancellationPolicy(policy)
                .build();
    }

    @Override
    @Transactional
    public PlatformConfigResponse updateConfig(UpdatePlatformConfigRequest request) {
        log.info("Updating platform configuration settings");

        if (request.getServiceFeePercent() != null) {
            saveValue(KEY_FEE, request.getServiceFeePercent().toString(), "Platform fee as % of booking total");
        }
        if (request.getTaxRatePercent() != null) {
            saveValue(KEY_TAX, request.getTaxRatePercent().toString(), "Regional tax as % of booking total");
        }
        if (request.getPayoutDelayDays() != null) {
            saveValue(KEY_PAYOUT, request.getPayoutDelayDays().toString(), "Host payout delay in days after checkout");
        }
        if (request.getCancellationPolicy() != null) {
            saveValue(KEY_POLICY, request.getCancellationPolicy().toUpperCase(), "Platform cancellation policy: FLEXIBLE, MODERATE, STRICT");
        }

        return getConfig();
    }

    private String getValue(String key, String defaultValue) {
        return platformConfigRepository.findByConfigKey(key)
                .map(PlatformConfig::getConfigValue)
                .orElse(defaultValue);
    }

    private void saveValue(String key, String value, String description) {
        PlatformConfig config = platformConfigRepository.findByConfigKey(key)
                .orElseGet(() -> PlatformConfig.builder()
                        .configKey(key)
                        .description(description)
                        .build());
        config.setConfigValue(value);
        platformConfigRepository.save(config);
    }
}
