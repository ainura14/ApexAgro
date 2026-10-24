package com.apexagro.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * BE3-02: в миграциях нет FK и других обращений между схемами модулей.
 * Путь можно переопределить: {@code -Dapexagro.migrations.root=...}.
 */
class MigrationBoundaryTest {

    private static final Path MIGRATIONS = Path.of(
            System.getProperty("apexagro.migrations.root", "src/main/resources/db/migration"));

    @Test
    void migrationsDoNotCrossSchemaBoundaries() throws IOException {
        List<MigrationScanner.Violation> violations = MigrationScanner.scan(MIGRATIONS);
        if (!violations.isEmpty()) {
            fail("Нарушения границ схем в миграциях:\n" + violations.stream()
                    .map(v -> "  " + v)
                    .collect(Collectors.joining("\n")));
        }
    }

    /** Самопроверка скрипта: намеренные нарушения должны находиться. */
    @Nested
    class ScannerSelfTest {

        @TempDir
        Path root;

        @Test
        void detectsCrossSchemaForeignKey() throws IOException {
            write("ordering/V202610120900__shipments.sql", "CREATE TABLE ordering.shipments (id uuid PRIMARY KEY);");
            write("delivery/V202610121000__tracking.sql", """
                    CREATE TABLE delivery.shipment_tracking (
                        shipment_id uuid PRIMARY KEY REFERENCES ordering.shipments (id)
                    );
                    """);

            List<MigrationScanner.Violation> violations = MigrationScanner.scan(root);

            assertThat(violations).singleElement().satisfies(v -> {
                assertThat(v.line()).isEqualTo(2);
                assertThat(v.message()).contains("FK между схемами").contains("ordering.shipments");
            });
        }

        @Test
        void detectsQuotedAndAlterTableForeignKey() throws IOException {
            write("ordering/V202610120900__orders.sql", "CREATE TABLE ordering.orders (id uuid PRIMARY KEY);");
            write("delivery/V202610121000__fk.sql", """
                    ALTER TABLE delivery.shipment_tracking
                        ADD CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES "ordering"."orders" (id);
                    """);

            assertThat(MigrationScanner.scan(root)).singleElement()
                    .satisfies(v -> assertThat(v.message()).contains("FK между схемами"));
        }

        @Test
        void detectsReadOrWriteOfForeignSchema() throws IOException {
            write("ordering/V202610120900__shipments.sql", "CREATE TABLE ordering.shipments (id uuid PRIMARY KEY);");
            write("delivery/V202610121000__view.sql",
                    "CREATE VIEW delivery.v AS SELECT s.id FROM ordering.shipments s;");

            assertThat(MigrationScanner.scan(root)).singleElement()
                    .satisfies(v -> assertThat(v.message()).contains("чужой схеме"));
        }

        @Test
        void allowsSameSchemaReferencesCommentsAndStrings() throws IOException {
            write("ordering/V202610120900__shipments.sql", "CREATE TABLE ordering.shipments (id uuid PRIMARY KEY);");
            write("delivery/V202610121000__history.sql", """
                    -- связь с ordering.shipments только по id, без FK
                    /* ordering.shipments упоминается в комментарии */
                    CREATE TABLE delivery.shipment_tracking (shipment_id uuid PRIMARY KEY);
                    CREATE TABLE shipment_status_history (
                        id bigserial PRIMARY KEY,
                        shipment_id uuid NOT NULL REFERENCES delivery.shipment_tracking (shipment_id)
                    );
                    COMMENT ON COLUMN delivery.shipment_tracking.shipment_id IS 'id из ordering.shipments';
                    """);

            assertThat(MigrationScanner.scan(root)).isEmpty();
        }

        @Test
        void detectsMigrationOutsideSchemaFolder() throws IOException {
            write("delivery/V202610121000__ok.sql", "SELECT 1;");
            write("V1__init.sql", "CREATE SCHEMA delivery;");

            assertThat(MigrationScanner.scan(root)).singleElement()
                    .satisfies(v -> assertThat(v.message()).contains("вне папки схемы"));
        }

        private void write(String relative, String content) throws IOException {
            Path file = root.resolve(relative);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        }
    }
}