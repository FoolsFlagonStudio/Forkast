package com.forkast.backend.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class MealPrepClassifierTest {

    private final MealPrepClassifier classifier = new MealPrepClassifier();

    @Test
    void recipeWithNoSignalsGetsTheBaseScore() {
        MealPrepInput recipe = new MealPrepInput("Lemon Chicken", null, null,
                List.of("Cook the chicken."), 2, List.of("chicken breast", "lemon"));

        assertThat(classifier.score(recipe)).isEqualTo(MealPrepClassifier.BASE_SCORE);
    }

    @Test
    void mealPrepChiliIsCappedAt100() {
        // 40 base + 30 keywords + 15 storage + 15 dish type + 5 servings = 105, clamped
        MealPrepInput recipe = new MealPrepInput("Turkey Chili", "Dinner", "meal prep, freezer friendly",
                List.of("Simmer for 30 minutes.", "Store in an airtight container for up to 4 days."),
                6, List.of("ground turkey", "kidney beans"));

        assertThat(classifier.score(recipe)).isEqualTo(100);
    }

    @Test
    void soupWithFourServings() {
        // 40 + 15 dish type + 5 servings
        MealPrepInput recipe = new MealPrepInput("Chicken Enchilada Soup", null, null,
                List.of("Simmer until thick."), 4, List.of("chicken breast"));

        assertThat(classifier.score(recipe)).isEqualTo(60);
    }

    @Test
    void friedAndServeImmediatelyIsClampedAtZero() {
        // 40 - 25 serve immediately - 15 fried - 15 avocado = -15, clamped
        MealPrepInput recipe = new MealPrepInput("Crispy Fish Tacos", null, null,
                List.of("Fry the fish.", "Serve immediately."), 2, List.of("cod", "avocado"));

        assertThat(classifier.score(recipe)).isZero();
    }

    @Test
    void leafySaladIsPenalized() {
        // 40 + 5 servings - 15 leafy salad
        MealPrepInput recipe = new MealPrepInput("Caesar Salad", null, null,
                List.of("Toss with dressing."), 4, List.of("romaine lettuce", "parmesan cheese"));

        assertThat(classifier.score(recipe)).isEqualTo(30);
    }

    @Test
    void nullFieldsAreHandled() {
        MealPrepInput recipe = new MealPrepInput("Toast", null, null, null, 1, null);

        assertThat(classifier.score(recipe)).isEqualTo(MealPrepClassifier.BASE_SCORE);
    }
}