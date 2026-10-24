package com.apexagro.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.apexagro.TestcontainersConfiguration;

/**
 * BE3-13 DoD: readiness падает, когда БД недоступна, а liveness остаётся UP (под не перезапускается зря).
 * Недоступность моделируется неверным паролем для новых соединений пула.
 */
@SpringBootTest(properties = "spring.datasource.hikari.connection-timeout=1000")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DirtiesContext
class ReadinessWithoutDatabaseTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    HikariDataSource dataSource;

    @Test
    void readinessIsDownWhenDatabaseIsUnavailable() throws Exception {
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());

        dataSource.getHikariConfigMXBean().setPassword("wrong-password");
        dataSource.getHikariPoolMXBean().softEvictConnections();

        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }
}
