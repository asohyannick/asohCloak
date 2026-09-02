package com.asohCloak.asohCloak.config.asyncScheduler.asyncTaskRunner;

import com.asohCloak.asohCloak.exception.backgroundTaskException.BackgroundTaskException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AsyncTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskRunner.class);

    private static final Duration DEFAULT_TASK_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_NOTIFY_DELAY = Duration.ofSeconds(30);

    /** Must match AsyncScheduler's retryPolicy() total-attempt budget. */
    private static final int MAX_TOTAL_ATTEMPTS = 5;

    private final Executor taskExecutor;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final RetryTemplate retryTemplate;

    /**
     * Runs `task` in the background. If it doesn't complete within `timeout`,
     * or throws, it's retried (up to 5 total attempts, per the shared RetryTemplate).
     * Once it finally succeeds or exhausts retries, `onSuccess`/`onFailure`
     * is invoked after `notifyDelay` (e.g. to send a "task completed" email
     * or push notification 30s later).
     */
    public <T> void runInBackground(Supplier<T> task,
                                    Duration timeout,
                                    Duration notifyDelay,
                                    Consumer<T> onSuccess,
                                    Consumer<Throwable> onFailure) {

        CompletableFuture
                .supplyAsync(() -> executeWithRetryAndTimeout(task, timeout), taskExecutor)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Background task failed after all retry attempts: {}", ex.getMessage(), ex);
                        scheduleAfterDelay(() -> onFailure.accept(ex), notifyDelay);
                    } else {
                        log.info("Background task completed successfully");
                        scheduleAfterDelay(() -> onSuccess.accept(result), notifyDelay);
                    }
                });
    }

    /** Overload using the default 30s timeout and 30s notify delay. */
    public <T> void runInBackground(Supplier<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        runInBackground(task, DEFAULT_TASK_TIMEOUT, DEFAULT_NOTIFY_DELAY, onSuccess, onFailure);
    }

    private <T> T executeWithRetryAndTimeout(Supplier<T> task, Duration timeout) {
        AtomicInteger attemptCounter = new AtomicInteger(0);

        return retryTemplate.invoke(() -> {
            int attempt = attemptCounter.incrementAndGet();
            log.info("Task attempt {} of {}", attempt, MAX_TOTAL_ATTEMPTS);

            CompletableFuture<T> future = CompletableFuture.supplyAsync(task, taskExecutor);
            try {
                return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                future.cancel(true);
                throw new BackgroundTaskException(
                        "Task timed out or failed on attempt " + attempt + ": " + e.getMessage(), e);
            }
        });
    }

    private void scheduleAfterDelay(Runnable action, Duration delay) {
        taskScheduler.schedule(() -> {
            try {
                action.run();
            } catch (Exception e) {
                log.error("Post-task notification failed: {}", e.getMessage(), e);
            }
        }, Instant.now().plus(delay));
    }
}