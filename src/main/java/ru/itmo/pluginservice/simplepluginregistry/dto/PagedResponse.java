package ru.itmo.pluginservice.simplepluginregistry.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        int page,
        int limit,
        long totalItems,
        int totalPages
) {
    public static <S, T> PagedResponse<T> from(Page<S> source, List<T> items) {
        return new PagedResponse<>(
                items,
                source.getNumber() + 1,
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages()
        );
    }
}
