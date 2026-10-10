package com.forkast.backend.mealplan.weekly;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.history.RecipeHistoryRepository;
import com.forkast.backend.mealplan.MealPlan;
import com.forkast.backend.mealplan.MealPlanRepository;
import com.forkast.backend.mealplan.MealSlot;
import com.forkast.backend.mealplan.PlanSource;
import com.forkast.backend.user.User;
import com.forkast.backend.user.UserPreferences;
import com.forkast.backend.user.UserPreferencesRepository;
import com.forkast.backend.user.UserRepository;

/**
 * The weekly job's pieces against the real database, with a throwaway user in
 * UTC who plans
 * 3 dinners a week from Sunday. Only this user's plans are generated or
 * recorded; the "who's
 * due" and "which plans ended" lists are checked for containing them. Rolls
 * back.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class WeeklyPlanWorkerIntegrationTest {

    private static final LocalDate SATURDAY = LocalDate.parse("2030-01-05");
    private static final LocalDate SUNDAY = SATURDAY.plusDays(1);

    @Autowired
    private WeeklyPlanWorker worker;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserPreferencesRepository preferencesRepository;
    @Autowired
    private MealPlanRepository planRepository;
    @Autowired
    private RecipeHistoryRepository historyRepository;

    private User user;

    private User newUser(String prefix) {
        User created = new User("Job", "Tester", prefix + UUID.randomUUID() + "@example.com", "not-a-real-hash");
        created.setTimezone("UTC");
        return userRepository.saveAndFlush(created);
    }

    @BeforeEach
    void setUp() {
        user = newUser("job-test-");
        UserPreferences preferences = new UserPreferences(user, new BigDecimal("100.00"));
        preferences.setPlanStartDay(DayOfWeek.SUNDAY);
        preferences.replaceMealSlots(Map.of(MealSlot.DINNER, 3));
        preferencesRepository.saveAndFlush(preferences);
    }

    private static Instant utc(LocalDate date, String time) {
        return Instant.parse(date + "T" + time + ":00Z");
    }

    // ---------- who's due ----------

    @Test
    void aUserIsDueFromElevenTheirTimeTheNightBefore() {
        assertThat(worker.dueUsers(utc(SATURDAY, "23:20")))
                .contains(new WeeklyPlanWorker.Due(user.getId(), SUNDAY));
        assertThat(worker.dueUsers(utc(SATURDAY, "22:20")))
                .extracting(WeeklyPlanWorker.Due::userId).doesNotContain(user.getId());
    }

    @Test
    void aUserWithThatWeeksPlanIsNotDue() {
        worker.generateFor(user.getId(), SUNDAY);

        assertThat(worker.dueUsers(utc(SATURDAY, "23:20")))
                .extracting(WeeklyPlanWorker.Due::userId).doesNotContain(user.getId());
    }

    @Test
    void aUserWithNoMealsIsNeverDue() {
        User bare = newUser("job-test-bare-");
        preferencesRepository.saveAndFlush(new UserPreferences(bare, new BigDecimal("50.00")));

        assertThat(worker.dueUsers(utc(SATURDAY, "23:20")))
                .extracting(WeeklyPlanWorker.Due::userId).doesNotContain(bare.getId());
        assertThat(worker.generateFor(bare.getId(), SUNDAY)).isFalse();
    }

    // ---------- making the plan ----------

    @Test
    void runningTwiceMakesOnePlan() {
        assertThat(worker.generateFor(user.getId(), SUNDAY)).isTrue();
        assertThat(worker.generateFor(user.getId(), SUNDAY)).isFalse();

        MealPlan plan = planRepository.findByUserIdAndWeekStartDate(user.getId(), SUNDAY).orElseThrow();
        assertThat(plan.getSource()).isEqualTo(PlanSource.AUTO);
        assertThat(plan.getSelections()).hasSize(3);
        assertThat(plan.getGroceryCost()).isNotNull();
    }

    // ---------- history ----------

    @Test
    void anEndedPlanIsWrittenToHistoryOnce() {
        LocalDate past = LocalDate.parse("2020-01-05");
        worker.generateFor(user.getId(), past);
        UUID planId = planRepository.findByUserIdAndWeekStartDate(user.getId(), past).orElseThrow().getId();
        Instant now = Instant.parse("2020-02-01T12:00:00Z");

        assertThat(worker.endedPlansWithoutHistory(now)).contains(planId);
        assertThat(worker.recordHistory(planId)).isEqualTo(3);
        assertThat(worker.recordHistory(planId)).isZero();
        assertThat(worker.endedPlansWithoutHistory(now)).doesNotContain(planId);

        assertThat(historyRepository.findTop20ByUserIdOrderByServedAtDesc(user.getId()))
                .hasSize(3)
                .allSatisfy(h -> assertThat(LocalDate.ofInstant(h.getServedAt(), user.getZoneId()))
                        .isBetween(past, past.plusDays(6)));
    }

    @Test
    void aPlanStillRunningSomewhereIsntEnded() {
        worker.generateFor(user.getId(), SUNDAY);
        UUID planId = planRepository.findByUserIdAndWeekStartDate(user.getId(), SUNDAY).orElseThrow().getId();

        // the week's last day, Saturday the 12th: not over everywhere yet
        assertThat(worker.endedPlansWithoutHistory(utc(SUNDAY.plusDays(6), "12:00"))).doesNotContain(planId);
        assertThat(worker.endedPlansWithoutHistory(utc(SUNDAY.plusDays(8), "12:00"))).contains(planId);
    }
}