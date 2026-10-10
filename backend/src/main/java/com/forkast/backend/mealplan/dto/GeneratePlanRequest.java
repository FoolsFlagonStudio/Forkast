package com.forkast.backend.mealplan.dto;

import java.time.LocalDate;

/** weekStart is optional: left out, the plan is for the next plan start day (today if it's that day). */
public record GeneratePlanRequest(LocalDate weekStart) {
}