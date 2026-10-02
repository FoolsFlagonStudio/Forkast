package com.forkast.backend.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AuthCodeRepository extends JpaRepository<AuthCode, UUID> {

    Optional<AuthCode> findByUserIdAndType(UUID userId, AuthCodeType type);

    @Modifying
    @Query("delete from AuthCode c where c.user.id = :userId and c.type = :type")
    int deleteByUserIdAndType(UUID userId, AuthCodeType type);
}