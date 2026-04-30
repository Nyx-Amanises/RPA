package com.rpa.manage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * RPA 任务后台执行线程池配置。
 */
@Configuration
public class TaskExecutorConfig {

    @Bean(name = "rpaTaskExecutor")
    public ThreadPoolTaskExecutor rpaTaskExecutor(
            @Value("${app.task-executor.core-pool-size:2}") int corePoolSize,
            @Value("${app.task-executor.max-pool-size:4}") int maxPoolSize,
            @Value("${app.task-executor.queue-capacity:100}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("rpa-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
