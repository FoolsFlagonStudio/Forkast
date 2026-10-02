package com.forkast.backend.history;

import static com.forkast.backend.common.ValidationUtils.requireNonNull;

import java.time.Instant;
import java.util.UUID;
import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

@Entity
@Table(name = "recipe_history", indexes = @Index(name = "idx_history_user_served", columnList = "user_id, served_at"))
public class RecipeHistory {

    // ---------- id ----------

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---------- columns ----------

    @Column(name = "served_at", nullable = false, updatable = false)
    private Instant servedAt;

    // ---------- relationships ----------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false, updatable = false)
    private Recipe recipe;

    // ---------- constructors ----------

    protected RecipeHistory() {
    }

    public RecipeHistory(User user, Recipe recipe, Instant servedAt) {
        this.user = requireNonNull(user, "user");
        this.recipe = requireNonNull(recipe, "recipe");
        this.servedAt = requireNonNull(servedAt, "servedAt");
    }

    // ---------- getters only (immutable) ----------

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public Instant getServedAt() {
        return servedAt;
    }
}
