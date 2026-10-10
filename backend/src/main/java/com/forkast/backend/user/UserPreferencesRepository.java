package com.forkast.backend.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {

    Optional<UserPreferences> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    /** Every user's preferences with the user, for the weekly plan job. */
    @Query("select p from UserPreferences p join fetch p.user")
    List<UserPreferences> findAllWithUser();
}