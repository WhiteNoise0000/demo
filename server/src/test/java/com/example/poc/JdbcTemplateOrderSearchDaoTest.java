package com.example.poc;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;

import com.example.poc.dao.jdbc.JdbcTemplateOrderSearchDao;
import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;

@JdbcTest
@Import(JdbcTemplateOrderSearchDao.class)
class JdbcTemplateOrderSearchDaoTest {

    @Autowired
    private JdbcTemplateOrderSearchDao dao;

    @Test
    void searchOrders_shouldBuildDynamicSqlAndMapRowsByAlias() {
        SearchCond cond = new SearchCond();
        cond.setStatuses(Set.of("PAID"));
        cond.setTotalGte(new BigDecimal("250.00"));
        cond.setCreatedAtFrom(Instant.parse("2026-01-01T00:00:00Z"));

        List<OrderSearchView> rows = dao.searchOrders(cond);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(OrderSearchView::getId).containsExactly(1007L, 1004L);
        assertThat(rows).extracting(OrderSearchView::getCustomerName).containsExactly("Alice", "Carol");
    }

    @Test
    void summarizeByStatus_shouldMapToDtoViaBeanPropertyRowMapper() {
        List<OrderStatusSummary> rows = dao.summarizeByStatus("PAID");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getStatus()).isEqualTo("PAID");
        assertThat(rows.get(0).getOrderCount()).isEqualTo(3L);
        assertThat(rows.get(0).getTotalSum()).isEqualTo(960L);
    }
}
