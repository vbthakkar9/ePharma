package com.pharmacy.saas.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
            .content(page.getContent())
            .pageNumber(page.getNumber())
            .pageSize(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .empty(page.isEmpty())
            .build();
    }

    public static <T, U> PageResponse<U> map(PageResponse<T> source, Function<? super T, ? extends U> mapper) {
        return PageResponse.<U>builder()
            .content(source.getContent().stream().map(mapper).collect(Collectors.toList()))
            .pageNumber(source.getPageNumber())
            .pageSize(source.getPageSize())
            .totalElements(source.getTotalElements())
            .totalPages(source.getTotalPages())
            .first(source.isFirst())
            .last(source.isLast())
            .empty(source.isEmpty())
            .build();
    }

    public ApiResponse.Meta toMeta() {
        return ApiResponse.Meta.builder()
            .totalElements(totalElements)
            .totalPages(totalPages)
            .currentPage(pageNumber)
            .pageSize(pageSize)
            .hasNext(!last)
            .hasPrevious(!first)
            .build();
    }
}