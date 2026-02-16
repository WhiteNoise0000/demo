package com.example.poc.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
