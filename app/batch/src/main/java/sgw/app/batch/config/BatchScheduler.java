package sgw.app.batch.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Simple scheduler that logs messages periodically.
 * The cron expression is controlled via the {@code batch.schedule.cron} property.
 */
@Component
public class BatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatchScheduler.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Scheduled(cron = "${batch.schedule.cron:0 0/5 * * * ?}")
    public void scheduledTask() {
        String timestamp = LocalDateTime.now().format(formatter);
        log.info("=================================================");
        log.info("Batch Scheduler executed at: {}", timestamp);
        log.info("=================================================");
    }
}
