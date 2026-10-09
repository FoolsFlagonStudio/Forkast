package com.forkast.backend.pricing.kroger;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * forkast.pricing.kroger.* settings. Credentials go in
 * application-local.properties (never
 * committed); everything else has a default:
 *
 * forkast.pricing.kroger.client-id=...
 * forkast.pricing.kroger.client-secret=...
 * forkast.pricing.kroger.base-url=https://api-ce.kroger.com certification;
 * api.kroger.com
 * once the app is in production
 * forkast.pricing.kroger.location-id=01400513 Kroger On the Rhine, Cincinnati
 * forkast.pricing.kroger.request-delay-ms=250 pause between product calls
 *
 * Without credentials the refresh job logs that Kroger is off and skips it, so
 * the app still
 * runs (and prices from the other sources).
 */
@ConfigurationProperties(prefix = "forkast.pricing.kroger")
public record KrogerProperties(
        String clientId,
        String clientSecret,
        @DefaultValue("https://api-ce.kroger.com") String baseUrl,
        @DefaultValue("01400513") String locationId,
        @DefaultValue("250") long requestDelayMs) {

    public boolean configured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }
}