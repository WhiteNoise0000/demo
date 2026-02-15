package com.example.poc.service.pattern;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.poc.dao.SpringDataOrderSearchViewRepository;
import com.example.poc.dao.spec.OrderSearchSpecificationFactory;
import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;

import lombok.RequiredArgsConstructor;

/**
 * 新実装のうち Spring Data JPA を使う経路をまとめたサービスです。
 * <p>
 * Specification 検索と Named Query 実行の窓口を統一し、
 * EntityManager 経路との比較を分かりやすくします。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SpringDataOrderSearchPattern {

    private final SpringDataOrderSearchViewRepository repository;

    /**
     * Specification を使って動的検索を行います。
     *
     * @param cond 検索条件
     * @return 作成日時降順の検索結果
     */
    public List<OrderSearchView> searchBySpecification(SearchCond cond) {
        return repository.findAll(
                OrderSearchSpecificationFactory.fromCondition(cond),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Named JPQL を Spring Data JPA 経由で実行します。
     *
     * @param keyword 顧客名の部分一致キーワード
     * @return 条件に一致した受注一覧
     */
    public List<OrderSearchView> searchByNamedJpql(String keyword) {
        String value = keyword == null ? "" : keyword;
        return repository.runNamedJpql(value);
    }

    /**
     * Named Native Query を Spring Data JPA 経由で実行します。
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    public List<OrderStatusSummary> summarizeByNamedNative(String status) {
        return repository.runNamedNative(status);
    }
}
