package com.app.backend.common.response;

import java.util.List;

/**
 * Response phân trang ổn định (D-2-05) — không serialize Page/Spring Data ra API.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size,
                                         long totalElements, int totalPages) {
        boolean first = page == 0;
        boolean last = totalPages == 0 || page >= totalPages - 1;
        return new PageResponse<>(content, page, size, totalElements, totalPages, first, last);
    }
}
