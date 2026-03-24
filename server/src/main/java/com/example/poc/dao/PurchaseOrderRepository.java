package com.example.poc.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.poc.dao.jdbcquery.JdbcTemplateQuery;
import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.entity.PurchaseOrder;

/**
 * 受注テーブルへのアクセスを担当するSpring Data Repositoryです。
 */
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, PurchaseOrderRepositoryCustom {

    /**
     * 指定ステータスの受注合計金額を返します。
     *
     * @param status 集計対象ステータス
     * @return 合計金額（該当なしは0）
     */
    @Query("select coalesce(sum(p.total), 0) from PurchaseOrder p where p.status = :status")
    long sumTotalByStatus(String status);

    /**
     * 外部 SQL ファイルを読み込み、NamedParameterJdbcTemplate で DTO 集計を行います。
     *
     * @param status 集計対象ステータス（null 時は全件）
     * @return ステータス別集計
     */
    @JdbcTemplateQuery
    List<OrderStatusSummary> summarizeByStatusWithJdbc(@Param("status") String status);

    /**
     * `default` メソッドから `@JdbcTemplateQuery` 対象メソッドを呼ぶサンプルです。
     *
     * @return PAID ステータスの集計
     */
    default List<OrderStatusSummary> summarizePaidWithJdbc() {
        return summarizeByStatusWithJdbc("PAID");
    }
}
