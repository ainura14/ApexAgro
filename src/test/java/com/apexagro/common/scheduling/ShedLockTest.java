package com.apexagro.common.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import javax.sql.DataSource;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.apexagro.TestcontainersConfiguration;

/**
 * BE3-15 DoD: два экземпляра — одна задача. Два независимых LockProvider (как два пода) на одной БД:
 * пока первый держит блокировку, второй её не получает.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ShedLockTest {

    @Autowired
    DataSource dataSource;

    private JdbcTemplateLockProvider instance() {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .withTableName(SchedulingConfig.LOCK_TABLE)
                .usingDbTime()
                .build());
    }

    @Test
    void onlyOneInstanceRunsTheTask() {
        JdbcTemplateLockProvider podA = instance();
        JdbcTemplateLockProvider podB = instance();
        LockConfiguration config = new LockConfiguration(Instant.now(), "test.shedlock-" + System.nanoTime(),
                Duration.ofMinutes(5), Duration.ZERO);

        Optional<SimpleLock> lockA = podA.lock(config);
        assertThat(lockA).as("первый экземпляр получил блокировку").isPresent();
        assertThat(podB.lock(config)).as("второй экземпляр не получил").isEmpty();

        lockA.get().unlock();
        Optional<SimpleLock> lockB = podB.lock(config);
        assertThat(lockB).as("после завершения задача доступна снова").isPresent();
        lockB.get().unlock();
    }
}
