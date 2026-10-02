package com.forkast.backend.diet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DietaryLabelRepository extends JpaRepository<DietaryLabel, UUID> {

    List<DietaryLabel> findAllByOrderByNameAsc();

    Optional<DietaryLabel> findByNameIgnoreCase(String name);
}