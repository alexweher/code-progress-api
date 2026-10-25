package com.example.codeprogress.common.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageRequestFactory {

    public static final int MAX_PAGE_SIZE = 100;

    private PageRequestFactory() {
    }

    public static Pageable create(
            int page,
            int size,
            String sort,
            Set<String> allowedSortFields,
            String defaultSortField
    ) {
        if (page < 0) {
            throw new InvalidPageRequestException("Page must be greater than or equal to zero");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }

        String sortValue = sort == null || sort.isBlank() ? defaultSortField + ",asc" : sort;
        String[] parts = sortValue.split(",", -1);
        if (parts.length > 2 || parts[0].isBlank()) {
            throw new InvalidPageRequestException("Sort must use the format field,asc or field,desc");
        }

        String field = parts[0].trim();
        if (!allowedSortFields.contains(field)) {
            throw new InvalidPageRequestException("Sorting by '" + field + "' is not allowed");
        }

        String directionValue = parts.length == 1 ? "asc" : parts[1].trim();
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(directionValue);
        } catch (IllegalArgumentException exception) {
            throw new InvalidPageRequestException("Sort direction must be 'asc' or 'desc'");
        }

        Sort pageSort = Sort.by(direction, field);
        if (!field.equals("id")) {
            pageSort = pageSort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, size, pageSort);
    }
}
