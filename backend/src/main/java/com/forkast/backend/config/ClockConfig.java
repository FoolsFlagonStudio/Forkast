package com.forkast.backend.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One Clock for "now", so services can ask for today in a user's timezone and
 * tests can pin
 * the date. Code reads it as LocalDate.now(clock.withZone(user.getZoneId())).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}