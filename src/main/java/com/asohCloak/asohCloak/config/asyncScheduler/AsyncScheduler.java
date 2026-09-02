package com.asohCloak.asohCloak.config.asyncScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncScheduler implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncScheduler.class);

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 16;
    private static final int QUEUE_CAPACITY = 200;

    private static final int SCHEDULER_POOL_SIZE = 8;

    /** Total attempts (1 initial + retries) shared by AsyncTaskRunner. */
    private static final int MAX_TOTAL_ATTEMPTS = 5;
    private static final long RETRY_BACKOFF_MS = 2000L;

    /**
     * Thread pool used by @Async-annotated methods and by AsyncTaskRunner
     * for background work (uploads, emails, notifications, media processing).
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("AsyncTask-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Used for delayed/scheduled work — e.g. firing a "task completed"
     * notification 30 seconds after a background job finishes.
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(SCHEDULER_POOL_SIZE);
        scheduler.setThreadNamePrefix("ScheduledTask-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Shared retry policy: up to 5 total attempts (1 initial + 4 retries),
     * 2s fixed delay between attempts. Uses Spring Framework 7's native
     * core.retry support — no spring-retry / spring-boot-starter-aop needed.
     * Used by AsyncTaskRunner and available for injection anywhere else.
     */
    @Bean
    public RetryPolicy retryPolicy() {
        return RetryPolicy.builder()
                .maxRetries(MAX_TOTAL_ATTEMPTS - 1) // total = 1 + maxRetries
                .delay(Duration.ofMillis(RETRY_BACKOFF_MS))
                .build();
    }

    @Bean
    public RetryTemplate retryTemplate(RetryPolicy retryPolicy) {
        return new RetryTemplate(retryPolicy);
    }

    /**
     * Catches exceptions from void @Async methods that would otherwise
     * vanish silently, since @Async methods can't propagate exceptions
     * back to the caller.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("Uncaught async exception in '{}' with params {}: {}",
                        method.getName(), Arrays.toString(params), ex.getMessage(), ex);
    }
}