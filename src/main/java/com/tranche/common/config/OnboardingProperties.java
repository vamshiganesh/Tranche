package com.tranche.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tranche.onboarding")
public record OnboardingProperties(
        boolean exposeVerificationCode
) {
    public OnboardingProperties {
        // default false when unset
    }
}
