package com.forkast.backend.mealplan.grocery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.common.exception.ApiException;
import com.forkast.backend.mealplan.MealPlanRepository;
import com.forkast.backend.mealplan.MealPlanService;
import com.forkast.backend.mealplan.MealSlot;
import com.forkast.backend.mealplan.dto.MealPlanDetailResponse;
import com.forkast.backend.user.User;
import com.forkast.backend.user.UserPreferences;
import com.forkast.backend.user.UserPreferencesRepository;
import com.forkast.backend.user.UserRepository;

/**
 * The grocery list of a real generated plan: a throwaway user (3 dinners and 3
 * lunches, household
 * of 2) and a week in 2030, so it uses the real recipes and prices. Rolls back.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class GroceryListServiceIntegrationTest {

    private static final LocalDate FUTURE_SUNDAY = LocalDate.parse("2030-01-06");

    @Autowired
    private MealPlanService planService;
    @Autowired
    private GroceryListService groceryService;
    @Autowired
    private MealPlanRepository planRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserPreferencesRepository preferencesRepository;

    private User user;
    private MealPlanDetailResponse plan;

    @BeforeEach
    void setUp() {
        user = userRepository.saveAndFlush(
                new User("Grocery", "Tester", "grocery-test-" + UUID.randomUUID() + "@example.com", "not-a-real-hash"));
        UserPreferences preferences = new UserPreferences(user, new BigDecimal("150.00"));
        preferences.setHouseholdServingSize(2);
        preferences.setPlanStartDay(DayOfWeek.SUNDAY);
        preferences.replaceMealSlots(Map.of(MealSlot.DINNER, 3, MealSlot.LUNCH, 3));
        preferencesRepository.saveAndFlush(preferences);
        plan = planService.generate(user.getId(), FUTURE_SUNDAY);
    }

    @Test
    void theListIsBuiltWithThePlanAndAddsUpToTheGroceryCost() {
        GroceryListResponse list = groceryService.list(user.getId(), plan.id());

        assertThat(list.toBuy()).isNotEmpty();
        assertThat(list.groceryCost()).isNotNull();
        BigDecimal sum = java.util.stream.Stream.concat(list.toBuy().stream(), list.pantry().stream())
                .map(GroceryListResponse.Item::cost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(list.groceryCost()).isEqualByComparingTo(sum);
        assertThat(list.pantry()).allSatisfy(item -> assertThat(item.needToBuy()).isFalse());
    }

    @Test
    void tickingALineOffIsKept() {
        GroceryListResponse.Item first = groceryService.list(user.getId(), plan.id()).toBuy().get(0);

        groceryService.update(user.getId(), first.id(), new UpdateGroceryItemRequest(true, null));

        GroceryListResponse.Item again = groceryService.list(user.getId(), plan.id()).toBuy().stream()
                .filter(i -> i.id().equals(first.id())).findFirst().orElseThrow();
        assertThat(again.purchased()).isTrue();
    }

    @Test
    void onlyPantryStaplesCanBeMarkedNeedToBuy() {
        GroceryListResponse.Item first = groceryService.list(user.getId(), plan.id()).toBuy().get(0);

        assertThatThrownBy(
                () -> groceryService.update(user.getId(), first.id(), new UpdateGroceryItemRequest(null, true)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void needToBuyChargesTheWholePackage() {
        GroceryListResponse before = groceryService.list(user.getId(), plan.id());
        GroceryListResponse.Item staple = before.pantry().stream()
                .filter(i -> i.packagePrice() != null && i.packageCount() != null)
                .findFirst().orElse(null);
        if (staple == null) {
            return; // this draw has no packaged pantry staple; nothing to check
        }

        groceryService.update(user.getId(), staple.id(), new UpdateGroceryItemRequest(null, true));
        GroceryListResponse after = groceryService.list(user.getId(), plan.id());

        GroceryListResponse.Item marked = after.pantry().stream().filter(i -> i.id().equals(staple.id()))
                .findFirst().orElseThrow();
        assertThat(marked.needToBuy()).isTrue();
        assertThat(marked.cost()).isEqualByComparingTo(
                marked.packagePrice().multiply(BigDecimal.valueOf(marked.packageCount())));
        assertThat(after.groceryCost()).isGreaterThanOrEqualTo(before.groceryCost());
    }

    @Test
    void aRebuildKeepsLineIdsAndTicks() {
        GroceryListResponse.Item first = groceryService.list(user.getId(), plan.id()).toBuy().get(0);
        groceryService.update(user.getId(), first.id(), new UpdateGroceryItemRequest(true, null));

        groceryService.rebuild(planRepository.findById(plan.id()).orElseThrow());

        assertThat(groceryService.list(user.getId(), plan.id()).toBuy())
                .anySatisfy(i -> {
                    assertThat(i.id()).isEqualTo(first.id());
                    assertThat(i.purchased()).isTrue();
                });
    }

    @Test
    void someoneElsesListIsNotFound() {
        User stranger = userRepository.saveAndFlush(
                new User("Other", "Tester", "grocery-stranger-" + UUID.randomUUID() + "@example.com", "x"));
        UUID itemId = groceryService.list(user.getId(), plan.id()).toBuy().get(0).id();

        assertThatThrownBy(() -> groceryService.list(stranger.getId(), plan.id()))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(
                () -> groceryService.update(stranger.getId(), itemId, new UpdateGroceryItemRequest(true, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}