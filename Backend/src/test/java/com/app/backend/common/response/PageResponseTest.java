package com.app.backend.common.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void TC_PRD_UT_02_trang_rong() {
        PageResponse<String> page = PageResponse.of(List.of(), 0, 20, 0, 0);
        assertThat(page.totalPages()).isZero();
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isTrue();
        assertThat(page.content()).isEmpty();
    }

    @Test
    void TC_PRD_UT_02_tong_pages_ceiling_khi_le() {
        // 5 phần tử, size 2 → 3 trang (ceil 5/2)
        PageResponse<Integer> page = PageResponse.of(List.of(1, 2), 0, 2, 5, 3);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isFalse();

        PageResponse<Integer> lastPage = PageResponse.of(List.of(9), 2, 2, 5, 3);
        assertThat(lastPage.first()).isFalse();
        assertThat(lastPage.last()).isTrue();
    }

    @Test
    void TC_PRD_UT_02_content_giu_thu_tu() {
        PageResponse<String> page = PageResponse.of(List.of("a", "b", "c"), 0, 3, 3, 1);
        assertThat(page.content()).containsExactly("a", "b", "c");
        assertThat(page.totalElements()).isEqualTo(3);
    }
}
