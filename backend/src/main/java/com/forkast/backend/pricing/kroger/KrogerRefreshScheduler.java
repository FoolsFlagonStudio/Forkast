package com.forkast.backend.pricing.kroger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Runs the Kroger refresh weekly. The schedule is a setting, so it can change once Kroger's
 * limits and the catalog size are known:
 *
 *   forkast.pricing.refresh-cron=0 0 6 * * MON     (default: Mondays at 6am Eastern)
 *
 * Spring cron has six fields: second minute hour day-of-month month day-of-week. Set it to
 * "-" to turn the schedule off. Without Kroger credentials a run logs and does nothing.
 */
@Configuration
@EnableScheduling
public class KrogerRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(KrogerRefreshScheduler.class);

    private final KrogerRefreshService refreshService;
    private final KrogerClient client;

    public KrogerRefreshScheduler(KrogerRefreshService refreshService, KrogerClient client) {
        this.refreshService = refreshService;
        this.client = client;
    }

    @Scheduled(cron = "${forkast.pricing.refresh-cron:0 0 6 * * MON}", zone = "America/New_York")
    public void weeklyRefresh() {
        if (!client.configured()) {
            log.info("Kroger refresh skipped: no credentials configured");
            return;
        }
        try {
            refreshService.refresh();
        } catch (RuntimeException e) {
            // logged, not rethrown: an exception here would only end up in the scheduler's log
            log.error("Scheduled Kroger refresh failed", e);
        }
    }
}