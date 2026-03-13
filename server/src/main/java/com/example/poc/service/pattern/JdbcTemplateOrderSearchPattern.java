package com.example.poc.service.pattern;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.poc.dao.jdbc.JdbcTemplateOrderSearchDao;
import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;

import lombok.RequiredArgsConstructor;

/**
 * NamedParameterJdbcTemplate を使う検索経路をまとめたサービスです。
 * <p>
 * Java text block + BeanPropertyRowMapper による SQL-first 実装を
 * 他の PoC パターンと比較しやすくします。
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JdbcTemplateOrderSearchPattern {

    private final JdbcTemplateOrderSearchDao dao;

    /**
     * Java 側で組み立てた動的 SQL を実行します。
     *
     * @param cond 検索条件
     * @return 条件に一致した受注一覧
     */
    public List<OrderSearchView> searchDynamically(SearchCond cond) {
        return dao.searchOrders(cond);
    }

    /**
     * BeanPropertyRowMapper で DTO 集計結果を返します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    public List<OrderStatusSummary> summarizeByStatus(String status) {
        return dao.summarizeByStatus(status);
    }
}
