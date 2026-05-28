package com.regionalai.floatingball.server.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AiStreamExecutorConfig {

    @Bean(name = "aiStreamExecutor")
    public Executor aiStreamExecutor(@Value("${floating-ball.ai.stream.core-pool-size:4}") int corePoolSize,
                                     @Value("${floating-ball.ai.stream.max-pool-size:16}") int maxPoolSize,
                                     @Value("${floating-ball.ai.stream.queue-capacity:32}") int queueCapacity,
                                     @Value("${floating-ball.ai.stream.keep-alive-seconds:60}") long keepAliveSeconds) {
        int core = Math.max(1, corePoolSize);
        int max = Math.max(core, maxPoolSize);
        int capacity = Math.max(1, queueCapacity);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            core,
            max,
            Math.max(1L, keepAliveSeconds),
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(capacity),
            new NamedThreadFactory("ai-chat-stream-"),
            new ThreadPoolExecutor.AbortPolicy()
        );
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private static final class NamedThreadFactory implements java.util.concurrent.ThreadFactory {

        private final String prefix;
        private final AtomicInteger index = new AtomicInteger(1);

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + index.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
