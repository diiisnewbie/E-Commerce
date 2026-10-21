package com.app.backend.product.query;

import com.app.backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductSortTest {

    @Test
    void TC_PRD_UT_01_parse_sort_hop_le_va_luon_kem_khoa_phu_id() {
        Sort sort = ProductSort.parse("price,asc");
        List<Sort.Order> orders = sort.stream().toList();
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getProperty()).isEqualTo("price");
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(orders.get(1).getProperty()).isEqualTo("id");
        assertThat(orders.get(1).getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void TC_PRD_UT_01_thieu_huong_default_dung_theo_truong() {
        Sort nameSort = ProductSort.parse("name");
        assertThat(nameSort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);

        Sort createdSort = ProductSort.parse("createdAt");
        assertThat(createdSort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);

        Sort nullSort = ProductSort.parse(null);
        assertThat(nullSort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void TC_PRD_UT_01_truong_ngoai_whitelist_loi() {
        assertThatThrownBy(() -> ProductSort.parse("password,asc"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException ex = (BusinessException) e;
                    assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(ex.getFields()).containsKey("sort");
                });
    }

    @Test
    void TC_PRD_UT_01_huong_sai_loi() {
        assertThatThrownBy(() -> ProductSort.parse("price,sideways"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getFields()).containsKey("sort"));
    }
}
