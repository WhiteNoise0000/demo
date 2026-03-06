package com.example.poc;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;

import com.example.poc.common.hibernate.NativeQueryExecutor;
import com.example.poc.common.hibernate.TupleTransformerFactory;
import com.example.poc.dao.CustomerRepository;
import com.example.poc.dao.EntityManagerOrderSearchDao;
import com.example.poc.dao.PurchaseOrderRepository;
import com.example.poc.dao.SpringDataOrderSearchViewRepository;
import com.example.poc.dao.spec.OrderSearchSpecificationFactory;
import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;

@DataJpaTest
@Import({ EntityManagerOrderSearchDao.class, TupleTransformerFactory.class, NativeQueryExecutor.class })
class OrderJpaTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private SpringDataOrderSearchViewRepository springDataOrderSearchViewRepository;

    @Autowired
    private EntityManagerOrderSearchDao entityManagerOrderSearchDao;

    @Autowired
    private NativeQueryExecutor nativeQueryExecutor;

    @Test
    void derivedQuery_shouldFindCustomerByName() {
        // 派生クエリ(findBy...)で大文字小文字を無視した部分一致検索が効くことを確認する
        assertThat(customerRepository.findByNameContainingIgnoreCase("ali"))
                .extracting("name")
                .containsExactly("Alice");
    }

    @Test
    void queryAnnotation_shouldSumTotalByStatus() {
        // @Query(JPQL)で集計SQLを書いた場合に、longで合計を正しく受け取れることを確認する
        long sum = purchaseOrderRepository.sumTotalByStatus("PAID");
        assertTrue(sum == 960L);
    }

    @Test
    void specification_shouldFilterDynamically() {
        // Specificationで条件を積み上げ、動的WHEREが期待通り合成されることを確認する
        SearchCond cond = new SearchCond();
        cond.setStatuses(Set.of("PAID"));
        cond.setTotalGte(new BigDecimal("250.00"));
        cond.setCreatedAtFrom(Instant.parse("2026-01-01T00:00:00Z"));

        List<OrderSearchView> rows = springDataOrderSearchViewRepository.findAll(
                OrderSearchSpecificationFactory.fromCondition(cond),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(OrderSearchView::getId).containsExactly(1007L, 1004L);
    }

    @Test
    void criteria_shouldFilterDynamicallyWithLimit() {
        // Criteria API版でも同等の動的WHEREが書けることと、limit適用が効くことを確認する
        SearchCond cond = new SearchCond();
        cond.setCustomerNameKeyword("bo");

        List<OrderSearchView> rows = entityManagerOrderSearchDao.searchByCriteriaWithLimit(cond, 2);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(OrderSearchView::getCustomerName).containsOnly("Bob");
        assertThat(rows).extracting(OrderSearchView::getId).containsExactly(1008L, 1005L);
    }

    @Test
    void namedJpql_shouldReturnRows() {
        // orm.xmlのNamed JPQLをEntityManager経由で呼び出し、並び順を含めて結果を検証する
        List<OrderSearchView> rows = entityManagerOrderSearchDao.runNamedJpql("ali");

        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(OrderSearchView::getId).containsExactly(1007L, 1003L, 1001L);
    }

    @Test
    void namedNative_shouldReturnSummary() {
        // orm.xmlのNamed Native Query + ResultSetMappingでDTOへ変換できることを確認する
        List<OrderStatusSummary> summary = entityManagerOrderSearchDao.runNamedNative("PAID");

        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).getStatus()).isEqualTo("PAID");
        assertThat(summary.get(0).getOrderCount()).isEqualTo(3L);
        assertThat(summary.get(0).getTotalSum()).isEqualTo(960L);
    }

    @Test
    void tupleTransformer_shouldReturnSummary() {
        // Hibernate TupleTransformer版では同じNative SQLからDTOへ変換できる
        List<OrderStatusSummary> summary = entityManagerOrderSearchDao.runNativeWithTupleTransformer("PAID");

        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).getStatus()).isEqualTo("PAID");
        assertThat(summary.get(0).getOrderCount()).isEqualTo(3L);
        assertThat(summary.get(0).getTotalSum()).isEqualTo(960L);
    }

    @Test
    void nativeQueryExecutor_shouldMapAliasesToDtoWithInlineSql() {
        List<OrderStatusSummary> summary = nativeQueryExecutor.list("""
                select status as status, count(*) as order_count, sum(total) as total_sum
                from purchase_orders
                where (:status is null or status = :status)
                group by status
                order by status
                """, java.util.Map.of("status", "PAID"), OrderStatusSummary.class);

        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).getStatus()).isEqualTo("PAID");
        assertThat(summary.get(0).getOrderCount()).isEqualTo(3L);
        assertThat(summary.get(0).getTotalSum()).isEqualTo(960L);
    }
}
