package com.forkast.backend.pricing;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/**
 * forkast.pricing.* settings. Both have defaults, so nothing needs adding to
 * the properties
 * files until a value should change:
 *
 * forkast.pricing.live-max-age=14d how long a Kroger price counts as current
 * forkast.pricing.bls-max-age=60d BLS publishes monthly, about two weeks after
 * the month
 * ends; import_prices.py dates each price at its month's
 * end, so the newest one is at most about 47 days old
 */
@Validated
@ConfigurationProperties(prefix = "forkast.pricing")
public record PricingProperties(
        @NotNull @DefaultValue("14d") Duration liveMaxAge,
        @NotNull @DefaultValue("60d") Duration blsMaxAge) {
}