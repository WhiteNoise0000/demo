package com.example.poc.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.entity.OrderSearchView;

/**
 * 新実装側の「Spring Data JPA パターン」を表すRepositoryです。
 * <p>
 * `OrderSearchView` を対象に、Specification検索と Named Query 実行を担います。
 * </p>
 */
public interface SpringDataOrderSearchViewRepository
        extends JpaRepository<OrderSearchView, Long>, JpaSpecificationExecutor<OrderSearchView> {

    /**
     * {@code orm.xml} の Named JPQL を Spring Data JPA 経由で実行します。
     *
     * @param keyword 顧客名の部分一致キーワード（null時は呼び出し側で空文字に寄せる）
     * @return 条件に一致した受注一覧
     */
    @Query(name = "OrderSearchView.findByCustomerNameLike")
    List<OrderSearchView> runNamedJpql(String keyword);

    /**
     * {@code orm.xml} の Named Native Query を Spring Data JPA 経由で実行します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    @Query(name = "OrderSearchView.summaryByStatusNative", nativeQuery = true)
    List<OrderStatusSummary> runNamedNative(String status);

}
