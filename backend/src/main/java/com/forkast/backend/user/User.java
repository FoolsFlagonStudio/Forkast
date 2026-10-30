package com.forkast.backend.user;

import static com.forkast.backend.common.ValidationUtils.requireText;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "users")

public class User {

    public static final String DEFAULT_TIMEZONE = "America/New_York";

    // ---------- id ----------

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---------- columns ----------

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_premium", nullable = false)
    private boolean premium = false;

    @Column(name = "is_verified", nullable = false)
    private boolean verified = false;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * IANA zone (America/New_York): when "11pm before the plan start day" happens
     */
    @Column(nullable = false, length = 64)
    private String timezone = DEFAULT_TIMEZONE;

    // ---------- timestamps ----------

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ---------- constructors ----------

    protected User() {
    }

    public User(String firstName, String lastName, String email, String passwordHash) {
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
    }

    // ---------- getters / setters ----------

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName.trim();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName.trim();
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = requireText(newPasswordHash, "passwordHash");
        this.passwordChangedAt = Instant.now();
    }

    public void markVerified() {
        this.verified = true;
    }

    public void recordLogin() {
        this.lastLoginAt = Instant.now();
    }

    public boolean isVerified() {
        return verified;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public ZoneId getZoneId() {
        return ZoneId.of(timezone);
    }

    /** Must be a zone Java knows, like America/New_York or Europe/London. */
    public void setTimezone(String timezone) {
        String zone = requireText(timezone, "timezone");
        try {
            this.timezone = ZoneId.of(zone).getId();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Unknown timezone '" + zone + "'");
        }
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void changeEmail(String newEmail) {
        this.email = requireText(newEmail, "email").toLowerCase();
    }
}