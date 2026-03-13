package com.example.poc.service;

import java.util.List;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;

/**
 * 受注検索APIの契約インターフェースです。
 * <p>
 * 新旧実装や実装パターン比較のため、同じ業務要件を
 * 複数の技術アプローチで呼び分けられるようにしています。
 * </p>
 */
@HttpExchange(url = "/orders", accept = "application/json", contentType = "application/json")
public interface OrderService {

    /**
     * Spring Data JPA + Specification で動的検索を行います。
     *
     * @param cond 検索条件
     * @return 条件に一致した受注一覧
     */
    @PostExchange("/search/spec/spring-data")
    List<OrderSearchView> searchSpecBySpringData(@RequestBody SearchCond cond);

    /**
     * EntityManager + Criteria API で動的検索を行います。
     *
     * @param cond 検索条件
     * @return 条件に一致した受注一覧
     */
    @PostExchange("/search/criteria/entity-manager")
    List<OrderSearchView> searchCriteriaByEntityManager(@RequestBody SearchCond cond);

    /**
     * MyBatis の動的SQLで条件付き検索を行います。
     *
     * @param cond 検索条件
     * @return 条件に一致した受注一覧
     */
    @PostExchange("/search/dynamic/mybatis")
    List<OrderSearchView> searchDynamicByMyBatis(@RequestBody SearchCond cond);

    /**
     * NamedParameterJdbcTemplate で動的 SQL を組み立てて検索を行います。
     *
     * @param cond 検索条件
     * @return 条件に一致した受注一覧
     */
    @PostExchange("/search/dynamic/jdbc-template")
    List<OrderSearchView> searchDynamicByJdbcTemplate(@RequestBody SearchCond cond);

    /**
     * EntityManager 経由で Named JPQL を実行します。
     *
     * @param kw 顧客名の部分一致キーワード
     * @return 条件に一致した受注一覧
     */
    @GetExchange("/named/jpql/entity-manager")
    List<OrderSearchView> namedJpqlByEntityManager(@RequestParam String kw);

    /**
     * EntityManager 経由で Named Native Query を実行します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    @GetExchange("/named/native/entity-manager")
    List<OrderStatusSummary> namedNativeByEntityManager(@RequestParam(required = false) String status);

    /**
     * Spring Data JPA 経由で Named JPQL を実行します。
     *
     * @param kw 顧客名の部分一致キーワード
     * @return 条件に一致した受注一覧
     */
    @GetExchange("/named/jpql/spring-data")
    List<OrderSearchView> namedJpqlBySpringData(@RequestParam String kw);

    /**
     * Spring Data JPA 経由で Named Native Query を実行します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    @GetExchange("/named/native/spring-data")
    List<OrderStatusSummary> namedNativeBySpringData(@RequestParam(required = false) String status);

    /**
     * Hibernate TupleTransformer を使って Native Query 結果を DTO へ変換します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    @GetExchange("/summary/tuple-transformer/entity-manager")
    List<OrderStatusSummary> summaryByStatusTupleTransformer(@RequestParam(required = false) String status);

    /**
     * MyBatis で Native SQL 集計を実行します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    @GetExchange("/summary/mybatis")
    List<OrderStatusSummary> summaryByStatusMyBatis(@RequestParam(required = false) String status);

    /**
     * NamedParameterJdbcTemplate + BeanPropertyRowMapper で Native SQL 集計を実行します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    @GetExchange("/summary/jdbc-template")
    List<OrderStatusSummary> summaryByStatusJdbcTemplate(@RequestParam(required = false) String status);
}
