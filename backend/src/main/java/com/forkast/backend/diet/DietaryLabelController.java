package com.forkast.backend.diet;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dietary-labels")
public class DietaryLabelController {

    private final DietaryLabelRepository dietaryLabelRepository;

    public DietaryLabelController(DietaryLabelRepository dietaryLabelRepository) {
        this.dietaryLabelRepository = dietaryLabelRepository;
    }

    @GetMapping
    public List<DietaryLabelResponse> list() {
        return dietaryLabelRepository.findAllByOrderByNameAsc().stream().map(DietaryLabelResponse::from).toList();
    }

}
