package com.forkast.backend.mealplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.recipe.Recipe;
import com.forkast.backend.recipe.RecipeRepository;
import com.forkast.backend.recipe.RecipeStep;
import com.forkast.backend.user.User;
import com.forkast.backend.user.UserRepository;

import jakarta.persistence.EntityManager;

/**
 * V9 and the entities agree: a plan with selections and dated meals saves and
 * reads back, the
 * calendar can't hold two meals in one slot on one day, and deleting a plan
 * removes the rest.
 * Uses the other@example.com test account and a throwaway recipe. Rolls back.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class MealPlanPersistenceIntegrationTest {

    private static final LocalDate START = LocalDate.parse("2030-01-06"); // far from any real plan

    @Autowired
    private MealPlanRepository planRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private EntityManager entityManager;

    private Recipe recipe() {
        Recipe recipe = new Recipe("Plan Test Chili", 6);
        recipe.addStep(new RecipeStep(1, "Simmer."));
        recipe.setSlotScores(5, 70, 90);
        return recipeRepository.saveAndFlush(recipe);
    }

    private MealPlan planWithDinner(Recipe recipe) {
        User user = userRepository.findByEmail("other@example.com").orElseThrow();
        MealPlan plan = new MealPlan(user, START, new BigDecimal("80.00"), PlanSource.AUTO);
        plan.recordGeneration(42L, Instant.now());
        PlanSelection dinner = new PlanSelection(MealSlot.DINNER, 1, START, 3);
        dinner.assignRecipe(recipe, 6, new BigDecimal("1.000"), new BigDecimal("12.50"));
        plan.addSelection(dinner);
        dinner.addMeal(START, MealSlot.DINNER, false);
        dinner.addMeal(START.plusDays(1), MealSlot.DINNER, true);
        dinner.addMeal(START.plusDays(1), MealSlot.LUNCH, true);
        plan.setCosts(new BigDecimal("12.50"), null, 0);
        return plan;
    }

    @Test
    void planRoundTrips() {
        Recipe recipe = recipe();
        MealPlan saved = planRepository.saveAndFlush(planWithDinner(recipe));
        entityManager.clear();

        MealPlan read = planRepository.findById(saved.getId()).orElseThrow();

        assertThat(read.getSource()).isEqualTo(PlanSource.AUTO);
        assertThat(read.getGenerationSeed()).isEqualTo(42L);
        assertThat(read.getUsageCost()).isEqualByComparingTo("12.50");
        assertThat(read.getSelections()).hasSize(1);
        PlanSelection dinner = read.getSelections().get(0);
        assertThat(dinner.getRecipe().getId()).isEqualTo(recipe.getId());
        assertThat(dinner.getPortionMultiplier()).isEqualByComparingTo("1.000");
        assertThat(read.getPlannedMeals())
                .extracting(PlannedMeal::getMealSlot)
                .containsExactly(MealSlot.DINNER, MealSlot.LUNCH, MealSlot.DINNER);
    }

    @Test
    void oneMealPerSlotPerDay() {
        MealPlan plan = planWithDinner(recipe());
        plan.getSelections().get(0).addMeal(START, MealSlot.DINNER, true); // Monday dinner twice

        assertThatThrownBy(() -> planRepository.saveAndFlush(plan))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void groceryLinesSaveTheirPackageAndPantryFields() {
        MealPlan plan = planWithDinner(recipe());
        com.forkast.backend.ingredient.Ingredient salt = new com.forkast.backend.ingredient.Ingredient(
                "plan test salt");
        salt.setPantryStaple(true);
        entityManager.persist(salt);
        GroceryListItem line = new GroceryListItem(salt, new BigDecimal("6.000"), "g");
        line.setPackage(1, "Kroger Salt 26 oz", new BigDecimal("1.29"));
        line.setPantry(true);
        line.setNeedToBuy(true);
        plan.addGroceryItem(line);
        MealPlan saved = planRepository.saveAndFlush(plan);
        entityManager.clear();

        GroceryListItem read = planRepository.findById(saved.getId()).orElseThrow().getGroceryItems().get(0);

        assertThat(read.isPantry()).isTrue();
        assertThat(read.isNeedToBuy()).isTrue();
        assertThat(read.getPackageCount()).isEqualTo(1);
        assertThat(read.getPackagePrice()).isEqualByComparingTo("1.29");
    }

    @Test
    void deletingAPlanRemovesItsSelectionsAndMeals() {
        MealPlan saved = planRepository.saveAndFlush(planWithDinner(recipe()));

        planRepository.delete(saved);
        planRepository.flush();

        Long selections = entityManager.createQuery(
                "select count(s) from PlanSelection s where s.startDate >= :start", Long.class)
                .setParameter("start", START).getSingleResult();
        Long meals = entityManager.createQuery(
                "select count(m) from PlannedMeal m where m.mealDate >= :start", Long.class)
                .setParameter("start", START).getSingleResult();
        assertThat(selections).isZero();
        assertThat(meals).isZero();
    }
}