package com.forkast.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class UserTimezoneTest {

    private final User user = new User("Test", "User", "tz@example.com", "hash");

    @Test
    void defaultsToNewYork() {
        assertThat(user.getTimezone()).isEqualTo("America/New_York");
        assertThat(user.getZoneId()).isEqualTo(ZoneId.of("America/New_York"));
    }

    @Test
    void acceptsIanaNames() {
        user.setTimezone(" Europe/London ");

        assertThat(user.getTimezone()).isEqualTo("Europe/London");
    }

    @Test
    void rejectsUnknownZones() {
        assertThatThrownBy(() -> user.setTimezone("Mars/Olympus_Mons")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> user.setTimezone(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}