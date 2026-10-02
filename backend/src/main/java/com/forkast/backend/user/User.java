package com.forkast.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")

public class User {

    // ---------- id ----------

    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---------- columns ----------

    @Column(name="first_name", nullable=false)
    private String firstName;

    @Column(name="last_name", nullable=false)
    private String lastName;

    @Column(nullable = false, unique=true)
    private String email;

    @Column(name="password_hash", nullable=false)
    private String passwordHash;

    @Column(name="is_premium", nullable=false)
    private boolean premium = false;

    // ---------- timestamps ----------

    @CreationTimestamp 
    @Column(name = "created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @UpdateTimestamp 
    @Column(name = "updated_at", nullable=false)
    private Instant updatedAt;

    // ---------- constructors ----------

    protected User() {}

    public User(String firstName, String lastName, String email, String passwordHash) {
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
    }

    // ---------- getters / setters ----------

    public UUID getId() {return id;}
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName.trim(); }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName.trim(); }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isPremium() { return premium; }
    public void setPremium(boolean premium) { this.premium = premium; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
