package com.example.poc.service.pattern;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.poc.dao.EntityManagerOrderSearchDao;
import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;

import lombok.RequiredArgsConstructor;

/**
 * 新実装のうち EntityManager を使う経路をまとめたサービスです。
 * <p>
 * Criteria API、Named Query、TupleTransformer を1つの窓口に集約し、
 * Spring Data 経路と並べて比較しやすくしています。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class EntityManagerOrderSearchPattern {

    private final EntityManagerOrderSearchDao dao;

    /**
     * Criteria API で動的検索を実行します。
     *
     * @param cond 検索条件
     * @param limit 最大件数
     * @return 条件に一致した受注一覧
     */
    public List<OrderSearchView> searchByCriteria(SearchCond cond, int limit) {
        return dao.searchByCriteriaWithLimit(cond, limit);
    }

    /**
     * Named JPQL を実行します。
     *
     * @param keyword 顧客名の部分一致キーワード
     * @return 条件に一致した受注一覧
     */
    public List<OrderSearchView> searchByNamedJpql(String keyword) {
        return dao.runNamedJpql(keyword);
    }

    /**
     * Named Native Query を実行します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    public List<OrderStatusSummary> summarizeByNamedNative(String status) {
        return dao.runNamedNative(status);
    }

    /**
     * Native Query 結果を TupleTransformer で DTO 変換して返します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    public List<OrderStatusSummary> summarizeByTupleTransformer(String status) {
        return dao.runNativeWithTupleTransformer(status);
    }
}
