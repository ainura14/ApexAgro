package com.apexagro.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * BE3-04 / BE3-06 DoD: реестр ErrorCode — коды уникальны во всём приложении, у каждого кода есть текст на ru и ky.
 * Новый enum ErrorCode в любом модуле подхватывается автоматически.
 */
class ErrorCodeRegistryTest {

    @Test
    void codesAreUniqueAndTranslated() throws Exception {
        List<ErrorCode> codes = allErrorCodes();
        assertThat(codes).as("найдены коды ошибок").isNotEmpty();

        Map<String, String> owners = new HashMap<>();
        List<String> duplicates = new ArrayList<>();
        for (ErrorCode code : codes) {
            String previous = owners.put(code.code(), code.getClass().getName());
            if (previous != null && !previous.equals(code.getClass().getName())) {
                duplicates.add(code.code() + " (" + previous + ", " + code.getClass().getName() + ")");
            }
            assertThat(code.code()).as("формат кода").matches("[A-Z][A-Z0-9_]*");
        }
        assertThat(duplicates).as("коды ошибок должны быть уникальны").isEmpty();

        for (String lang : List.of("ru", "ky")) {
            Properties bundle = load("messages/messages_" + lang + ".properties");
            List<String> missing = codes.stream().map(ErrorCode::messageKey).filter(k -> !bundle.containsKey(k)).toList();
            assertThat(missing).as("нет текстов в messages_" + lang + ".properties").isEmpty();
        }
    }

    private static List<ErrorCode> allErrorCodes() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition bd) {
                return true;
            }
        };
        scanner.addIncludeFilter(new AssignableTypeFilter(ErrorCode.class));
        List<ErrorCode> result = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("com.apexagro")) {
            Class<?> type = Class.forName(bd.getBeanClassName());
            if (type.isEnum()) {
                for (Object constant : type.getEnumConstants()) {
                    result.add((ErrorCode) constant);
                }
            }
        }
        return result;
    }

    private static Properties load(String path) throws IOException {
        Properties p = new Properties();
        try (InputStream in = ErrorCodeRegistryTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }
}
