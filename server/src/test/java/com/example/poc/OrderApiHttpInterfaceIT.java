package com.example.poc;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;
import com.example.poc.service.OrderService;

/**
 * {@link OrderService} をHTTPクライアントとして生成し、
 * 実アプリ（RANDOM_PORT）への疎通を検証する統合テストです。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderApiHttpInterfaceIT {

    @LocalServerPort
    private int port;

    private OrderService client;

    /**
     * 各テスト前にHTTP Interfaceクライアントを生成します。
     */
    @BeforeEach
    void setUp() {
        // @HttpExchangeクライアントをテストごとに組み立て、RANDOM_PORTのアプリへ向ける
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        this.client = factory.createClient(OrderService.class);
    }

    /**
     * Specification / Criteria / Named Query の主要APIが正常に呼び出せることを確認します。
     *
     * @throws Exception テスト実行時に例外が発生した場合
     */
    @Test
    void shouldCallSearchSpecAndCriteriaAndNamedQueries() {
        // HTTP Interface経由で、JPA/EntityManager/MyBatis の各APIを一通り疎通確認する
        SearchCond cond = new SearchCond();
        cond.setStatuses(Set.of("PAID", "SHIPPED"));
        cond.setTotalGte(new BigDecimal("100.00"));
        cond.setCreatedAtFrom(Instant.parse("2026-01-01T00:00:00Z"));

        List<OrderSearchView> specRows = client.searchSpecBySpringData(cond);
        List<OrderSearchView> criteriaRows = client.searchCriteriaByEntityManager(cond);
        List<OrderSearchView> myBatisRows = client.searchDynamicByMyBatis(cond);
        List<OrderSearchView> jpqlRows = client.namedJpqlByEntityManager("ali");
        List<OrderStatusSummary> nativeRows = client.namedNativeByEntityManager(null);
        List<OrderSearchView> jpqlRowsSpringData = client.namedJpqlBySpringData("ali"); // Spring Data JPA版
        List<OrderStatusSummary> nativeRowsSpringData = client.namedNativeBySpringData(null); // Spring Data JPA版
        List<OrderStatusSummary> tupleTransformerRows = client.summaryByStatusTupleTransformer(null); // TupleTransformer版
        List<OrderStatusSummary> myBatisSummaryRows = client.summaryByStatusMyBatis("PAID"); // MyBatis版

        assertThat(specRows).isNotEmpty();
        assertThat(criteriaRows).isNotEmpty();
        assertThat(myBatisRows).isNotEmpty();
        assertThat(jpqlRows).hasSize(3);
        assertThat(nativeRows).isNotEmpty();
        assertThat(jpqlRowsSpringData).hasSize(jpqlRows.size());
        assertThat(nativeRowsSpringData).hasSize(nativeRows.size());
        assertThat(tupleTransformerRows).hasSize(nativeRows.size());
        assertThat(myBatisSummaryRows).hasSize(1);

        // 比較しやすいように、件数だけでなく主要項目の並びも一致させる
        assertThat(myBatisRows.stream().map(OrderSearchView::getId).toList())
                .containsExactlyElementsOf(specRows.stream().map(OrderSearchView::getId).toList());
        assertThat(jpqlRowsSpringData.stream().map(OrderSearchView::getId).toList())
                .containsExactlyElementsOf(jpqlRows.stream().map(OrderSearchView::getId).toList());
        assertThat(nativeRowsSpringData.stream().map(OrderStatusSummary::getStatus).toList())
                .containsExactlyElementsOf(nativeRows.stream().map(OrderStatusSummary::getStatus).toList());
        assertThat(nativeRowsSpringData.stream().map(OrderStatusSummary::getOrderCount).toList())
                .containsExactlyElementsOf(nativeRows.stream().map(OrderStatusSummary::getOrderCount).toList());
        assertThat(nativeRowsSpringData.stream().map(OrderStatusSummary::getTotalSum).toList())
                .containsExactlyElementsOf(nativeRows.stream().map(OrderStatusSummary::getTotalSum).toList());
        assertThat(tupleTransformerRows.stream().map(OrderStatusSummary::getStatus).toList())
                .containsExactlyElementsOf(nativeRows.stream().map(OrderStatusSummary::getStatus).toList());
        assertThat(tupleTransformerRows.stream().map(OrderStatusSummary::getOrderCount).toList())
                .containsExactlyElementsOf(nativeRows.stream().map(OrderStatusSummary::getOrderCount).toList());
        assertThat(tupleTransformerRows.stream().map(OrderStatusSummary::getTotalSum).toList())
                .containsExactlyElementsOf(nativeRows.stream().map(OrderStatusSummary::getTotalSum).toList());
        assertThat(myBatisSummaryRows.get(0).getStatus()).isEqualTo("PAID");
        assertThat(myBatisSummaryRows.get(0).getOrderCount()).isEqualTo(3L);
        assertThat(myBatisSummaryRows.get(0).getTotalSum()).isEqualTo(960L);
    }
}
