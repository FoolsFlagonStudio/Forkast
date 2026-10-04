package com.forkast.backend.ingest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/ingredients")
public class AdminIngredientController {

    private final IngredientImportService importService;

    public AdminIngredientController(IngredientImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/import")
    public IngredientImportResult importIngredients(@Valid @RequestBody IngredientImportBatch batch) {
        return importService.importAll(batch.ingredients());
    }
}