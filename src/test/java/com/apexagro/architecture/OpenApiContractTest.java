package com.apexagro.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

import com.apexagro.TestcontainersConfiguration;

/**
 * BE3-08: «реализация соответствует спецификации».
 *
 * <p>Для каждого модуля (группа springdoc = модуль) сравнивает сгенерированную по коду спецификацию
 * с контрактом {@code api/<module>.yaml}: набор операций (метод + путь), наличие тела запроса,
 * успешные (2xx) коды ответов. Модуль с эндпоинтами без файла в {@code api/} — тоже ошибка.
 *
 * <p>Сгенерированные спецификации пишутся в {@code build/openapi/<module>.json}: CI дополнительно
 * прогоняет по ним openapi-diff (глубокое сравнение схем) и генерирует TS-клиент для фронтенда.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiContractTest {

    static final Path API_DIR = Path.of(System.getProperty("apexagro.api.root", "api"));
    static final Path OUT_DIR = Path.of(System.getProperty("apexagro.openapi.out", "build/openapi"));

    @Autowired
    MockMvc mvc;

    @Autowired
    Environment env;

    @Test
    void implementationMatchesContracts() throws Exception {
        Files.createDirectories(OUT_DIR);
        List<String> problems = new ArrayList<>();
        for (String module : modules()) {
            String generatedJson = mvc.perform(get("/v3/api-docs/" + module)).andReturn().getResponse()
                    .getContentAsString(StandardCharsets.UTF_8);
            Files.writeString(OUT_DIR.resolve(module + ".json"), generatedJson);
            OpenAPI generated = Json.mapper().readValue(generatedJson, OpenAPI.class);

            Path contractFile = API_DIR.resolve(module + ".yaml");
            boolean hasEndpoints = generated.getPaths() != null && !generated.getPaths().isEmpty();
            if (!Files.exists(contractFile)) {
                if (hasEndpoints) {
                    problems.add(module + ": есть эндпоинты, но нет контракта " + contractFile);
                }
                continue;
            }
            OpenAPI contract = Yaml.mapper().readValue(contractFile.toFile(), OpenAPI.class);
            problems.addAll(compare(module, contract, generated));
        }
        assertThat(problems).as("Расхождения кода и контрактов в api/").isEmpty();
    }

    static List<String> compare(String module, OpenAPI contract, OpenAPI generated) {
        Map<String, Operation> expected = operations(contract);
        Map<String, Operation> actual = operations(generated);
        List<String> problems = new ArrayList<>();
        for (String op : new TreeSet<>(expected.keySet())) {
            if (!actual.containsKey(op)) {
                problems.add(module + ": операция из контракта не реализована: " + op);
            }
        }
        for (String op : new TreeSet<>(actual.keySet())) {
            if (!expected.containsKey(op)) {
                problems.add(module + ": операции нет в контракте: " + op);
                continue;
            }
            Operation e = expected.get(op);
            Operation a = actual.get(op);
            if ((e.getRequestBody() != null) != (a.getRequestBody() != null)) {
                problems.add(module + ": " + op + ": тело запроса в контракте и в коде не совпадает");
            }
            Set<String> expectedOk = successCodes(e);
            Set<String> actualOk = successCodes(a);
            if (!expectedOk.equals(actualOk)) {
                problems.add(module + ": " + op + ": успешные ответы " + actualOk + ", в контракте " + expectedOk);
            }
        }
        return problems;
    }

    static Map<String, Operation> operations(OpenAPI api) {
        Map<String, Operation> result = new java.util.TreeMap<>();
        if (api.getPaths() == null) {
            return result;
        }
        api.getPaths().forEach((path, item) -> item.readOperationsMap()
                .forEach((method, op) -> result.put(method + " " + normalize(path), op)));
        return result;
    }

    /** {@code /items/{itemId}} и {@code /items/{id}} считаются одним путём. */
    static String normalize(String path) {
        return path.replaceAll("\\{[^}]+}", "{}").replaceAll("/$", "");
    }

    static Set<String> successCodes(Operation op) {
        Set<String> codes = new TreeSet<>();
        if (op.getResponses() != null) {
            op.getResponses().keySet().stream().filter(c -> c.startsWith("2")).forEach(codes::add);
        }
        return codes;
    }

    private List<String> modules() throws IOException {
        List<String> modules = new ArrayList<>();
        for (int i = 0; env.containsProperty("springdoc.group-configs[" + i + "].group"); i++) {
            modules.add(env.getProperty("springdoc.group-configs[" + i + "].group"));
        }
        if (Files.isDirectory(API_DIR)) {
            try (Stream<Path> files = Files.list(API_DIR)) {
                files.map(f -> f.getFileName().toString()).filter(n -> n.endsWith(".yaml") && !n.startsWith("_"))
                        .map(n -> n.substring(0, n.length() - ".yaml".length()))
                        .filter(m -> !modules.contains(m))
                        .forEach(m -> modules.add(m));
            }
        }
        assertThat(modules).isNotEmpty();
        return modules;
    }

}
