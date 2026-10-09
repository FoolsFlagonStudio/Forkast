package com.forkast.backend.pricing.kroger;

import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Calls Kroger's product API with an app token (client credentials, no user
 * login).
 *
 * The token lasts 30 minutes and is reused until a minute before it expires. A
 * 401 means it
 * was revoked early, so the token is dropped and the call tried once more.
 * Neither the
 * credentials nor the token are ever logged.
 */
@Component
public class KrogerClient {

    private static final String SCOPE = "product.compact";

    private final KrogerProperties properties;
    private final RestClient http;

    private String token;
    private Instant tokenExpires = Instant.EPOCH;

    public KrogerClient(KrogerProperties properties) {
        this.properties = properties;
        this.http = RestClient.builder().baseUrl(properties.baseUrl()).build();
    }

    public boolean configured() {
        return properties.configured();
    }

    public String locationId() {
        return properties.locationId();
    }

    /**
     * The product as priced at the configured store; empty when Kroger doesn't know
     * the id.
     */
    public Optional<KrogerProduct> product(String productId) {
        try {
            return Optional.ofNullable(fetchProduct(productId));
        } catch (HttpClientErrorException.Unauthorized e) {
            clearToken();
            return Optional.ofNullable(fetchProduct(productId));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    private KrogerProduct fetchProduct(String productId) {
        KrogerProduct.Response response = http.get()
                .uri(uri -> uri.path("/v1/products/{id}")
                        .queryParam("filter.locationId", properties.locationId())
                        .build(productId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve()
                .body(KrogerProduct.Response.class);
        return response == null ? null : response.data();
    }

    private synchronized String token() {
        if (token == null || Instant.now().isAfter(tokenExpires)) {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("scope", SCOPE);

            TokenResponse response = http.post()
                    .uri("/v1/connect/oauth2/token")
                    .headers(h -> h.setBasicAuth(properties.clientId(), properties.clientSecret()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new IllegalStateException("Kroger returned no access token");
            }
            token = response.accessToken();
            tokenExpires = Instant.now().plusSeconds(Math.max(60, response.expiresIn() - 60));
        }
        return token;
    }

    private synchronized void clearToken() {
        token = null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(@JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {
    }
}