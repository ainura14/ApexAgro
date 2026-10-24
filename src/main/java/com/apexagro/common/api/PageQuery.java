package com.apexagro.common.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * BE3-07: параметры страницы в запросе: {@code ?page=0&size=20}.
 *
 * <p>Использование в контроллере:
 * <pre>
 * &#64;GetMapping
 * PageResponse&lt;OfferDto&gt; list(&#64;Valid &#64;ParameterObject PageQuery page) {
 *     return PageResponse.from(repository.findAll(page.toPageable(Sort.by("createdAt").descending())), mapper::toDto);
 * }
 * </pre>
 */
public record PageQuery(
        @Min(0) Integer page,
        @Min(1) @Max(MAX_SIZE) Integer size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        page = page == null ? 0 : page;
        size = size == null ? DEFAULT_SIZE : size;
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }

    public Pageable toPageable(Sort sort) {
        return PageRequest.of(page, size, sort);
    }
}
