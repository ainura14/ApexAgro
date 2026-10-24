package com.apexagro.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.apexagro.TestcontainersConfiguration;

/**
 * BE3-03 DoD: миграции всех схем применяются на пустой БД (в CI Testcontainers даёт чистый PostgreSQL),
 * и каждая папка db/migration/&lt;schema&gt; превращается в схему без правки конфига.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FlywayMigrationsTest {

    private static final Path MIGRATIONS = Path.of(
            System.getProperty("apexagro.migrations.root", "src/main/resources/db/migration"));

    @Autowired
    Flyway flyway;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allMigrationsAppliedAndNothingPending() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).allSatisfy(m -> assertThat(m.getState().isApplied()).isTrue());
    }

    @Test
    void everyMigrationFolderBecomesSchema() throws IOException {
        List<String> folders;
        try (Stream<Path> dirs = Files.list(MIGRATIONS)) {
            folders = dirs.filter(Files::isDirectory).map(d -> d.getFileName().toString()).toList();
        }
        List<String> schemas = jdbc.queryForList("SELECT schema_name FROM information_schema.schemata", String.class);
        assertThat(schemas).containsAll(folders);
    }

    @Test
    void infrastructureTablesExist() {
        assertThat(jdbc.queryForObject("SELECT to_regclass('common.event_publication') IS NOT NULL", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT to_regclass('common.shedlock') IS NOT NULL", Boolean.class)).isTrue();
    }

    @Test
    void versionsFollowNamingConvention() throws IOException {
        try (Stream<Path> files = Files.walk(MIGRATIONS)) {
            assertThat(files.filter(Files::isRegularFile).map(f -> f.getFileName().toString()))
                    .allSatisfy(name -> assertThat(name).matches("(V\\d{12}__[a-z0-9_]+|R__[a-z0-9_]+)\\.sql"));
        }
    }
}
