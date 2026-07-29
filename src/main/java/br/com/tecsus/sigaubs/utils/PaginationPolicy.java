package br.com.tecsus.sigaubs.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PaginationPolicy {

    public static final int DEFAULT_PAGE_SIZE = 15;
    private static final int FIRST_PAGE = 0;
    private static final int MINIMUM_PAGE_SIZE = 1;
    private static final int MAXIMUM_PAGE_SIZE = 100;

    private PaginationPolicy() {
    }

    public static int normalizePageNumber(int pageNumber) {
        return Math.max(FIRST_PAGE, pageNumber);
    }

    public static int normalizePageSize(int pageSize) {
        return Math.clamp(pageSize, MINIMUM_PAGE_SIZE, MAXIMUM_PAGE_SIZE);
    }

    public static PageRequest pageRequest(int pageNumber, int pageSize) {
        return PageRequest.of(
                normalizePageNumber(pageNumber),
                normalizePageSize(pageSize));
    }

    public static PageRequest defaultPageRequest() {
        return PageRequest.of(FIRST_PAGE, DEFAULT_PAGE_SIZE);
    }

    public static PageRequest pageRequest(
            int pageNumber,
            int pageSize,
            Sort.Direction direction,
            String property) {
        return PageRequest.of(
                normalizePageNumber(pageNumber),
                normalizePageSize(pageSize),
                direction,
                property);
    }

    public static PageRequest defaultPageRequest(
            Sort.Direction direction,
            String property) {
        return PageRequest.of(FIRST_PAGE, DEFAULT_PAGE_SIZE, direction, property);
    }
}
