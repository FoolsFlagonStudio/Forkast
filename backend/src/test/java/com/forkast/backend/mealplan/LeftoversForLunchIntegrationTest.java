package com.forkast.backend.mealplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.mealplan.dto.MealPlanDetailResponse;
import com.forkast.backend.mealplan.dto.MealPlanDetailResponse.Meal;
import com.forkast.backend.mealplan.dto.MealPlanDetailResponse.Selection;
import com.forkast.backend.recipe.RecipeRepository;
import com.forkast.backend.user.User;
import com.forkast.backend.user.UserPreferences;
import com.forkast.backend.user.UserPreferencesRepository;
import com.forkast.backend.user.UserRepository;

/**
 * Leftovers for lunch end to end: a throwaway user with 3 dinners and 3 lunches
 * a week, a
 * household of 2 and leftovers_for_lunch on, planning a week in 2030. Each
 * dinner also feeds the
 * next day's lunch, lunch recipes fill the lunches left, and the dinner's
 * servings (so its
 * portion multiplier, which scales the grocery list) grow by one household.
 * Rolls back.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class LeftoversForLunchIntegrationTest {

    private static final LocalDate FUTURE_SUNDAY = LocalDate.parse("2030-01-06");
    private static final int HOUSEHOLD = 2;

    @Autowired
    private MealPlanService planService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserPreferencesRepository preferencesRepository;
    @Autowired
    private RecipeRepository recipeRepository;

    private MealPlanDetailResponse plan;

    @BeforeEach
    void setUp() {
        User user = userRepository.saveAndFlush(
                new User("Lunch", "Tester", "lunch-test-" + UUID.randomUUID() + "@example.com", "not-a-real-hash"));
        UserPreferences preferences = new UserPreferences(user, new BigDecimal("200.00"));
        preferences.setHouseholdServingSize(HOUSEHOLD);
        preferences.setPlanStartDay(DayOfWeek.SUNDAY);
        preferences.setLeftoversForLunch(true);
        preferences.replaceMealSlots(Map.of(MealSlot.DINNER, 3, MealSlot.LUNCH, 3));
        preferencesRepository.saveAndFlush(preferences);
        plan = planService.generate(user.getId(), FUTURE_SUNDAY);
    }

    private List<Selection> selections(MealSlot slot) {
        return plan.selections().stream().filter(s -> s.slot() == slot).toList();
    }

    @Test
    void everyDayHasOneLunchAndOneDinner() {
        assertThat(plan.days()).hasSize(7);
        assertThat(plan.days()).allSatisfy(day -> assertThat(day.meals()).extracting(Meal::slot)
                .containsExactly(MealSlot.LUNCH, MealSlot.DINNER));
        assertThat(plan.open()).isEmpty();
    }

    @Test
    void eachDinnerFeedsTheNextDaysLunch() {
        for (Selection dinner : selections(MealSlot.DINNER)) {
            LocalDate nextDay = dinner.startDate().plusDays(1);
            Meal lunch = plan.days().stream().filter(d -> d.date().equals(nextDay)).findFirst().orElseThrow()
                    .meals().stream().filter(m -> m.slot() == MealSlot.LUNCH).findFirst().orElseThrow();

            assertThat(lunch.selectionId()).isEqualTo(dinner.id());
            assertThat(lunch.leftover()).isTrue();
        }
    }

    @Test
    void aDinnerThatFeedsLunchCooksForOneMoreMeal() {
        for (Selection dinner : selections(MealSlot.DINNER)) {
            int baseServings = recipeRepository.findById(dinner.recipe().id()).orElseThrow().getBaseServings();

            assertThat(dinner.servings()).isEqualTo(HOUSEHOLD * (dinner.daysCovered() + 1));
            assertThat(dinner.portionMultiplier()).isEqualByComparingTo(BigDecimal.valueOf(dinner.servings())
                    .divide(BigDecimal.valueOf(baseServings), 3, RoundingMode.HALF_UP));
            assertThat(dinner.lastMealDate()).isAfterOrEqualTo(dinner.startDate().plusDays(1));
        }
    }

    @Test
    void lunchRecipesOnlyFillTheLunchesLeft() {
        int lunchesFromDinners = selections(MealSlot.DINNER).size();
        int lunchDays = selections(MealSlot.LUNCH).stream().mapToInt(Selection::daysCovered).sum();

        assertThat(lunchesFromDinners + lunchDays).isEqualTo(7);
        assertThat(plan.selections()).extracting(s -> s.recipe().id()).doesNotHaveDuplicates();
    }
}