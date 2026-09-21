package com.asohCloak.asohCloak.config.asyncScheduler.mediaCleanupScheduler;

import com.asohCloak.asohCloak.service.courseMediaService.CourseMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaCleanupScheduler {

    private static final Duration STALE_AFTER = Duration.ofHours(24);

    private final CourseMediaService courseMediaService;

    /** Unfinished multipart uploads keep their chunks in storage until aborted. */
    @Scheduled(cron = "0 30 3 * * *")
    public void abortStaleUploads() {
        try {
            int aborted = courseMediaService.abortStaleUploads(STALE_AFTER);
            if (aborted > 0) log.info("Aborted {} stale upload(s).", aborted);
        } catch (RuntimeException e) {
            log.error("Stale upload cleanup failed: {}", e.getMessage(), e);
        }
    }
}