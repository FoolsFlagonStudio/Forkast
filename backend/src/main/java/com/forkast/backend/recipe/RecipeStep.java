package com.forkast.backend.recipe;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "recipe_steps", uniqueConstraints = @UniqueConstraint(columnNames = { "recipe_id", "step_number" }))
public class RecipeStep {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(name = "instruction_text", nullable = false, columnDefinition = "text")
    private String instructionText;

    protected RecipeStep() {
    }

    public RecipeStep(int stepNumber, String instructionText) {
        setStepNumber(stepNumber);
        setInstructionText(instructionText);
    }

    public UUID getId() {
        return id;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        if (stepNumber < 1) {
            throw new IllegalArgumentException("stepNumber must be at least 1");
        }
        this.stepNumber = stepNumber;
    }

    public String getInstructionText() {
        return instructionText;
    }

    public void setInstructionText(String instructionText) {
        this.instructionText = instructionText.trim();
    }

    public Recipe getRecipe() {
        return recipe;
    }

    void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    } 
}
