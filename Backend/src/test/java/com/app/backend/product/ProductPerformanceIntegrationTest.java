package com.app.backend.product;

import com.app.backend.common.CatalogIntegrationTestBase;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-PERF-01: chứng minh không N+1 — public list ≤ 2 SQL, admin list ≤ 3 SQL.
 * Dùng hibernate Statistics (PrepareStatementCount) — assert <= vì Spring Data
 * có thể bỏ COUNT query khi trang đầu chưa đầy.
 */
class ProductPerformanceIntegrationTest extends CatalogIntegrationTestBase {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void TC_PERF_01_khong_n_plus_1() {
        String token = adminToken();
        long catId = idOf(mustCreateCategory(token, "Perf Cat " + uniqueSuffix(), "desc"));
        String prefix = "perf" + uniqueSuffix();
        for (int i = 0; i < 20; i++) {
            mustCreateProduct(token, prefix + "-" + i, "Perf Sp " + i + " " + prefix,
                    new BigDecimal("10000").add(new BigDecimal(i)), catId, 5);
        }

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);

        // Warm-up + clear
        getPublic("/api/v1/products?search=" + prefix + "&size=20");
        getWithBearer("/api/v1/admin/products?search=" + prefix + "&size=20", token);
        stats.clear();

        // Public list — content query + count query ≤ 2
        ResponseEntity<Map<String, Object>> publicList =
                getPublic("/api/v1/products?search=" + prefix + "&size=20");
        assertThat(publicList.getStatusCode().value()).isEqualTo(200);
        long publicSql = stats.getPrepareStatementCount();
        assertThat(publicSql).isLessThanOrEqualTo(2);
        stats.clear();

        // Admin list — content + count + stock IN-query ≤ 3
        ResponseEntity<Map<String, Object>> adminList =
                getWithBearer("/api/v1/admin/products?search=" + prefix + "&size=20", token);
        assertThat(adminList.getStatusCode().value()).isEqualTo(200);
        long adminSql = stats.getPrepareStatementCount();
        assertThat(adminSql).isLessThanOrEqualTo(3);
    }
}
