package com.pspd.backend.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Enveloppe de pagination stable exposée par l'API (évite de sérialiser
 * directement {@code Page}/{@code PageImpl}, dont le format JSON n'est pas garanti).
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(
            p.getContent(), p.getNumber(), p.getSize(),
            p.getTotalElements(), p.getTotalPages());
    }

    /** Pagination construite à la main (ex. tri/distance calculés en mémoire — B4). */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
