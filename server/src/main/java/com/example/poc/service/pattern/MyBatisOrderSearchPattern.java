package com.example.poc.service.pattern;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.poc.dao.mybatis.OrderSearchMyBatisMapper;
import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;

import lombok.RequiredArgsConstructor;

/**
 * MyBatis を使う検索経路をまとめたサービスです。
 * <p>
 * SQL-first な読み取り実装を JPA 系パターンと並べて比較しやすくします。
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyBatisOrderSearchPattern {

    private final OrderSearchMyBatisMapper mapper;

    /**
     * MyBatis の動的SQLで条件付き検索を実行します。
     *
     * @param cond 検索条件
     * @return 条件に一致した受注一覧
     */
    public List<OrderSearchView> searchDynamically(SearchCond cond) {
        return mapper.searchOrders(cond);
    }

    /**
     * MyBatis でステータス集計を実行します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    public List<OrderStatusSummary> summarizeByStatus(String status) {
        return mapper.summarizeByStatus(status);
    }
}
