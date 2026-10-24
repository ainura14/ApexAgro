package com.apexagro.common.api;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * BE3-07: единый формат страницы в ответах API.
 *
 * <pre>{ "items": [...], "page": 0, "size": 20, "total": 134 }</pre>
 *
 * {@code page} считается с нуля (как в Spring Data), {@code total} — общее число элементов.
 */
public record PageResponse<T>(List<T> items, int page, int size, long total) {

    public PageResponse {
        items = List.copyOf(items);
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public static <S, T> PageResponse<T> from(Page<S> page, Function<? super S, ? extends T> mapper) {
        return new PageResponse<>(page.getContent().stream().<T>map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public <R> PageResponse<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResponse<>(items.stream().<R>map(mapper).toList(), page, size, total);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) total / size);
    }
}
