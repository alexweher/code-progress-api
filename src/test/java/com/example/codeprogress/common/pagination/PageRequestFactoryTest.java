package com.example.codeprogress.common.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestFactoryTest {

    @Test
    void createsPageableWithAllowedSort() {
        var pageable = PageRequestFactory.create(2, 25, "title,desc", Set.of("id", "title"), "id");

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(25);
        assertThat(pageable.getSort().getOrderFor("title"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void usesDefaultSortWhenSortIsEmpty() {
        var pageable = PageRequestFactory.create(0, 20, "", Set.of("id"), "id");

        assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void rejectsInvalidSortDirection() {
        assertThatThrownBy(() -> PageRequestFactory.create(
                0, 20, "title,sideways", Set.of("title"), "title"
        )).isInstanceOf(InvalidPageRequestException.class)
                .hasMessage("Sort direction must be 'asc' or 'desc'");
    }

    @Test
    void rejectsMalformedSort() {
        assertThatThrownBy(() -> PageRequestFactory.create(
                0, 20, "title,asc,extra", Set.of("title"), "title"
        )).isInstanceOf(InvalidPageRequestException.class)
                .hasMessage("Sort must use the format field,asc or field,desc");
    }
}
