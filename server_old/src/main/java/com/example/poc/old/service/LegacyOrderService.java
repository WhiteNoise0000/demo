package com.example.poc.old.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.poc.old.dao.LegacyOrderDao;
import com.example.poc.old.dto.LegacyOrderStatusSummary;
import com.example.poc.old.entity.LegacyPurchaseOrder;

/**
 * 旧実装DAOを呼び出すサービス層です。
 * <p>
 * サービス層では「初期データ投入」と「参照」のみに責務を絞り、
 * DAOとの役割分離を分かりやすくしています。
 * </p>
 */
@Service
public class LegacyOrderService {

    private final LegacyOrderDao legacyOrderDao;

    /**
     * @param legacyOrderDao 注文DAO
     */
    @Autowired
    public LegacyOrderService(LegacyOrderDao legacyOrderDao) {
        this.legacyOrderDao = legacyOrderDao;
    }

    /**
     * テーブルが空の場合のみサンプルデータを投入します。
     */
    @Transactional
    public void bootstrapSampleData() {
        if (legacyOrderDao.count() > 0) {
            return;
        }

        legacyOrderDao.saveOrUpdate(new LegacyPurchaseOrder(
                1L, "Legacy Alice", "NEW", new BigDecimal("120.00"), new Date()));
        legacyOrderDao.saveOrUpdate(new LegacyPurchaseOrder(
                2L, "Legacy Bob", "PAID", new BigDecimal("320.50"), new Date()));
    }

    /**
     * 新しい注文から順に取得します。
     *
     * @param limit 最大取得件数
     * @return 注文一覧
     */
    @Transactional(readOnly = true)
    public List<LegacyPurchaseOrder> findRecent(int limit) {
        return legacyOrderDao.findRecent(limit);
    }

    /**
     * AliasToBeanResultTransformer を使った Native SQL 集計を実行します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    @Transactional(readOnly = true)
    public List<LegacyOrderStatusSummary> summarizeByStatusAliasToBean(String status) {
        return legacyOrderDao.summarizeByStatusAliasToBean(status);
    }
}
