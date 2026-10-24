package com.apexagro.common.scheduling;

import javax.sql.DataSource;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BE3-15: задачи по расписанию + ShedLock (таблица {@code common.shedlock}).
 *
 * <p>Соглашения для {@code @Scheduled}:
 * <ul>
 *   <li>каждая задача — с {@code @SchedulerLock(name = "<модуль>.<задача>", lockAtMostFor = "...")};
 *       имя уникально во всём приложении (ScheduledTasksConventionTest);</li>
 *   <li>{@code lockAtMostFor} — заметно больше обычного времени выполнения (страховка, если экземпляр упал);</li>
 *   <li>для частых коротких задач — {@code lockAtLeastFor}, чтобы соседний экземпляр не запустил её сразу же;</li>
 *   <li>расписание и сроки — в application.yml ({@code cron = "${apexagro.<модуль>.<задача>.cron}"});</li>
 *   <li>метрики выполнения Spring пишет сам: {@code tasks.scheduled.execution} (Prometheus).</li>
 * </ul>
 * Время блокировки — по часам БД ({@code usingDbTime}), чтобы расхождение часов экземпляров не мешало.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
class SchedulingConfig {

    static final String LOCK_TABLE = "common.shedlock";

    @Bean
    LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .withTableName(LOCK_TABLE)
                .usingDbTime()
                .build());
    }
}
