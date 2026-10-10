package com.forkast.backend.mealplan.weekly;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every hour: makes next week's plan for users whose generate-at time (11pm
 * local, the night
 * before their plan start day) has come, then writes recipe_history for plans
 * whose week has
 * ended. Running hourly covers every timezone within the hour.
 *
 * forkast.planner.cron=0 20 * * * * (default: minute 20 of every hour; "-"
 * turns it off)
 *
 * Safe to run twice: a week that already has a plan is skipped, and a plan's
 * history is
 * written once. Each user and each plan is its own transaction, so one failure
 * is logged and
 * the rest go ahead. No notifications in v1: the app shows the plan next time
 * it opens.
 */
@Component
public class WeeklyPlanJob {

    private static final Logger log = LoggerFactory.getLogger(WeeklyPlanJob.class);

    private final WeeklyPlanWorker worker;
    private final Clock clock;

    public WeeklyPlanJob(WeeklyPlanWorker worker, Clock clock) {
        this.worker = worker;
        this.clock = clock;
    }

    @Scheduled(cron = "${forkast.planner.cron:0 20 * * * *}")
    public void run() {
        Instant now = Instant.now(clock);
        generatePlans(now);
        recordHistory(now);
    }

    void generatePlans(Instant now) {
        int made = 0;
        for (WeeklyPlanWorker.Due due : worker.dueUsers(now)) {
            try {
                if (worker.generateFor(due.userId(), due.weekStart())) {
                    made++;
                }
            } catch (DataIntegrityViolationException e) {
                // another run made it between the check and the save: nothing to do
                log.info("Plan for user {} week {} already exists", due.userId(), due.weekStart());
            } catch (RuntimeException e) {
                log.error("Weekly plan for user {} week {} failed", due.userId(), due.weekStart(), e);
            }
        }
        if (made > 0) {
            log.info("Weekly job made {} plans", made);
        }
    }

    void recordHistory(Instant now) {
        for (UUID planId : worker.endedPlansWithoutHistory(now)) {
            try {
                worker.recordHistory(planId);
            } catch (RuntimeException e) {
                log.error("Recipe history for plan {} failed", planId, e);
            }
        }
    }
}