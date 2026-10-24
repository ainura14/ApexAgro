package com.apexagro.common.openapi;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BE3-08 / BE3-04: общая часть всех спецификаций: заголовок, схема Bearer JWT, схема ProblemDetail
 * и стандартные ответы-ошибки с примерами для каждой операции.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    static final String PROBLEM = "ProblemDetail";
    static final String PROBLEM_JSON = "application/problem+json";

    @Bean
    OpenAPI apexagroOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Apex Agro API").version("v1")
                        .description("Ошибки — RFC 7807 (application/problem+json) с полями code, traceId, errors[]. "
                                + "Деньги — строка с 2 знаками, количество — строка с 3 знаками, даты — ISO 8601 UTC. "
                                + "Язык ответа — Accept-Language: ky | ru."))
                .components(new Components()
                        .addSecuritySchemes("bearer", new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT"))
                        .addSchemas(PROBLEM, problemSchema()))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }

    /** Ко всем операциям всех групп добавляет 400/401/403/404/429/500 с примерами, если модуль не описал свои. */
    @Bean
    GlobalOpenApiCustomizer problemResponses() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSchemas(PROBLEM, problemSchema());
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                var responses = operation.getResponses();
                errorExamples().forEach((status, example) -> {
                    if (responses != null && !responses.containsKey(status)) {
                        responses.addApiResponse(status, problemResponse(example));
                    }
                });
            }));
        };
    }

    static Schema<?> problemSchema() {
        Schema<Object> fieldError = new ObjectSchema()
                .addProperty("field", new StringSchema())
                .addProperty("code", new StringSchema().example("NotBlank"))
                .addProperty("message", new StringSchema());
        return new ObjectSchema()
                .description("RFC 7807 Problem Details")
                .addProperty("type", new StringSchema().example("about:blank"))
                .addProperty("title", new StringSchema().example("Bad Request"))
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("detail", new StringSchema().description("Текст на языке запроса"))
                .addProperty("instance", new StringSchema())
                .addProperty("code", new StringSchema().description("Код из реестра ErrorCode").example("VALIDATION_FAILED"))
                .addProperty("traceId", new StringSchema().description("= X-Correlation-Id"))
                .addProperty("errors", new ArraySchema().items(fieldError).description("Только для VALIDATION_FAILED"))
                .required(List.of("status", "code"));
    }

    static Map<String, Example> errorExamples() {
        return Map.of(
                "400", example(400, "Bad Request", "VALIDATION_FAILED", "Данные запроса заполнены неверно",
                        List.of(Map.of("field", "name", "code", "NotBlank", "message", "Не должно быть пустым"))),
                "401", example(401, "Unauthorized", "UNAUTHORIZED", "Требуется вход в систему", null),
                "403", example(403, "Forbidden", "FORBIDDEN", "Недостаточно прав для этого действия", null),
                "404", example(404, "Not Found", "NOT_FOUND", "Не найдено", null),
                "429", example(429, "Too Many Requests", "RATE_LIMITED", "Слишком много запросов. Повторите немного позже", null),
                "500", example(500, "Internal Server Error", "INTERNAL_ERROR", "Внутренняя ошибка. Повторите попытку позже", null));
    }

    private static Example example(int status, String title, String code, String detail, Object errors) {
        var value = new java.util.LinkedHashMap<String, Object>();
        value.put("type", "about:blank");
        value.put("title", title);
        value.put("status", status);
        value.put("detail", detail);
        value.put("code", code);
        value.put("traceId", "3f2b7c1e-9a4d-4c55-8f0e-1d2a3b4c5d6e");
        if (errors != null) {
            value.put("errors", errors);
        }
        return new Example().value(value);
    }

    private static ApiResponse problemResponse(Example example) {
        MediaType media = new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM))
                .examples(Map.of("example", example));
        return new ApiResponse().description(String.valueOf(((Map<?, ?>) example.getValue()).get("title")))
                .content(new Content().addMediaType(PROBLEM_JSON, media));
    }
}
