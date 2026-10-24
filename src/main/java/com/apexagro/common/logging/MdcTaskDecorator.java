package com.apexagro.common.logging;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * BE3-05: переносит MDC (в том числе correlation id) в асинхронные задачи: {@code @Async},
 * {@code @ApplicationModuleListener} (Spring Modulith), внешнюю публикацию событий.
 * Boot применяет бин TaskDecorator к стандартному {@code applicationTaskExecutor}.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (context == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(context);
            }
            try {
                task.run();
            } finally {
                if (previous == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previous);
                }
            }
        };
    }
}
