package com.forkast.backend.auth;

import static com.forkast.backend.common.ValidationUtils.requireNonNull;
import static com.forkast.backend.common.ValidationUtils.requireText;
import static com.forkast.backend.common.ValidationUtils.trimToNull;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.forkast.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "auth_codes",
       uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "type" }))
public class AuthCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private AuthCodeType type;

    @Column(name = "code_hash", nullable = false, updatable = false, length = 64)
    private String codeHash;

    @Column(name = "new_email", updatable = false)
    private String newEmail;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    protected AuthCode() {
        // required by JPA
    }

    public AuthCode(User user, AuthCodeType type, String codeHash, Instant expiresAt, String newEmail) {
        this.user = requireNonNull(user, "user");
        this.type = requireNonNull(type, "type");
        this.codeHash = requireText(codeHash, "codeHash");
        this.expiresAt = requireNonNull(expiresAt, "expiresAt");
        this.newEmail = trimToNull(newEmail);
    }

    public boolean isExpired(Instant now) { return !expiresAt.isAfter(now); }
    public boolean isLocked(int maxAttempts) { return attempts >= maxAttempts; }
    public void recordFailedAttempt() { attempts++; }

    public UUID getId() { return id; }
    public AuthCodeType getType() { return type; }
    public String getCodeHash() { return codeHash; }
    public String getNewEmail() { return newEmail; }
    public int getAttempts() { return attempts; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public User getUser() { return user; }
}