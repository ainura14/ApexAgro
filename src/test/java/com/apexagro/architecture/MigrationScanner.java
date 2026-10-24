package com.apexagro.architecture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * BE3-02: проверка границ схем в миграциях Flyway.
 *
 * <p>Ожидаемая раскладка (BE3-03): {@code db/migration/<schema>/V<yyyyMMddHHmm>__описание.sql}.
 * Имя папки = схема, которой владеет модуль.
 *
 * <p>Нарушения:
 * <ul>
 *   <li>FK ({@code REFERENCES other_schema.table}) на таблицу другой схемы;</li>
 *   <li>любое другое обращение к {@code other_schema.object} (CREATE/ALTER TABLE, VIEW, INSERT, SELECT ...),
 *       где {@code other_schema} — одна из папок миграций;</li>
 *   <li>SQL-файл лежит не в папке схемы.</li>
 * </ul>
 *
 * <p>Имена без префикса схемы считаются своими. Комментарии и строковые литералы не проверяются.
 * Без зависимостей: используется из {@code MigrationBoundaryTest} и запускается как скрипт:
 * {@code java src/test/java/com/apexagro/architecture/MigrationScanner.java src/main/resources/db/migration}
 */
public final class MigrationScanner {

    public record Violation(Path file, int line, String message) {
        @Override
        public String toString() {
            return file + ":" + line + " — " + message;
        }
    }

    // schema.object, каждая часть может быть в двойных кавычках
    private static final Pattern QUALIFIED_NAME = Pattern.compile(
            "(?<![\\w$.\"])\"?([A-Za-z_][\\w$]*)\"?\\s*\\.\\s*\"?([A-Za-z_][\\w$]*)\"?");
    private static final Pattern REFERENCES_BEFORE = Pattern.compile("(?i)\\bREFERENCES\\s*$");

    private MigrationScanner() {
    }

    public static List<Violation> scan(Path migrationRoot) throws IOException {
        if (!Files.isDirectory(migrationRoot)) {
            throw new IllegalArgumentException("Папка миграций не найдена: " + migrationRoot.toAbsolutePath());
        }

        Set<String> schemas = new TreeSet<>();
        try (Stream<Path> dirs = Files.list(migrationRoot)) {
            dirs.filter(Files::isDirectory)
                    .forEach(d -> schemas.add(d.getFileName().toString().toLowerCase(Locale.ROOT)));
        }

        List<Violation> violations = new ArrayList<>();
        List<Path> sqlFiles;
        try (Stream<Path> files = Files.walk(migrationRoot)) {
            sqlFiles = files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".sql"))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }

        for (Path file : sqlFiles) {
            Path relative = migrationRoot.relativize(file);
            if (relative.getNameCount() < 2) {
                violations.add(new Violation(relative, 1,
                        "миграция лежит вне папки схемы; ожидается db/migration/<schema>/" + relative));
                continue;
            }
            String ownSchema = relative.getName(0).toString().toLowerCase(Locale.ROOT);
            String sql = Files.readString(file, StandardCharsets.UTF_8);
            violations.addAll(scanSql(relative, ownSchema, sql, schemas));
        }
        return violations;
    }

    static List<Violation> scanSql(Path file, String ownSchema, String sql, Set<String> knownSchemas) {
        String code = stripCommentsAndStrings(sql);
        List<Violation> result = new ArrayList<>();
        Matcher m = QUALIFIED_NAME.matcher(code);
        while (m.find()) {
            String schema = m.group(1).toLowerCase(Locale.ROOT);
            if (schema.equals(ownSchema) || !knownSchemas.contains(schema)) {
                continue;
            }
            String object = schema + "." + m.group(2).toLowerCase(Locale.ROOT);
            int line = lineOf(code, m.start());
            String before = code.substring(Math.max(0, m.start() - 64), m.start());
            if (REFERENCES_BEFORE.matcher(before).find()) {
                result.add(new Violation(file, line, "FK между схемами: " + ownSchema + " → " + object
                        + ". Связь между модулями — только по id без FK (см. MODULE_SCHEMAS.md)"));
            } else {
                result.add(new Violation(file, line, "миграция схемы '" + ownSchema
                        + "' обращается к чужой схеме: " + object));
            }
        }
        return result;
    }

    /** Заменяет комментарии и строковые литералы пробелами, сохраняя переводы строк (номера строк не сдвигаются). */
    static String stripCommentsAndStrings(String sql) {
        StringBuilder out = new StringBuilder(sql.length());
        int i = 0;
        int n = sql.length();
        while (i < n) {
            char c = sql.charAt(i);
            char next = i + 1 < n ? sql.charAt(i + 1) : '\0';
            if (c == '-' && next == '-') {
                while (i < n && sql.charAt(i) != '\n') {
                    out.append(' ');
                    i++;
                }
            } else if (c == '/' && next == '*') {
                int depth = 0;
                do {
                    char ch = sql.charAt(i);
                    char nx = i + 1 < n ? sql.charAt(i + 1) : '\0';
                    if (ch == '/' && nx == '*') {
                        depth++;
                        out.append("  ");
                        i += 2;
                    } else if (ch == '*' && nx == '/') {
                        depth--;
                        out.append("  ");
                        i += 2;
                    } else {
                        out.append(ch == '\n' ? '\n' : ' ');
                        i++;
                    }
                } while (i < n && depth > 0);
            } else if (c == '\'') {
                out.append(' ');
                i++;
                while (i < n) {
                    char ch = sql.charAt(i);
                    if (ch == '\'' && i + 1 < n && sql.charAt(i + 1) == '\'') {
                        out.append("  ");
                        i += 2;
                    } else if (ch == '\'') {
                        out.append(' ');
                        i++;
                        break;
                    } else {
                        out.append(ch == '\n' ? '\n' : ' ');
                        i++;
                    }
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static int lineOf(String text, int index) {
        int line = 1;
        for (int i = 0; i < index; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /** Запуск как скрипт: код выхода 1 при нарушениях. */
    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : "src/main/resources/db/migration");
        List<Violation> violations = scan(root);
        if (violations.isEmpty()) {
            System.out.println("Миграции: нарушений границ схем нет (" + root + ")");
            return;
        }
        System.err.println("Нарушения границ схем в миграциях:");
        violations.forEach(v -> System.err.println("  " + v));
        System.exit(1);
    }
}