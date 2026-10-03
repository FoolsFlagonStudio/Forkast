package com.forkast.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "forkast.admin")
public record AdminProperties(@Size(min = 32) String apiKey) {
}