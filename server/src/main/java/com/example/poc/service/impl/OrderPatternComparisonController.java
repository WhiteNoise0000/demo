package com.example.poc.service.impl;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;
import com.example.poc.service.OrderService;
import com.example.poc.service.pattern.EntityManagerOrderSearchPattern;
import com.example.poc.service.pattern.MyBatisOrderSearchPattern;
import com.example.poc.service.pattern.SpringDataOrderSearchPattern;

import lombok.RequiredArgsConstructor;

/**
 * 新実装側の比較用APIエントリポイントです。
 * <p>
 * HTTP契約は {@link OrderService} に集約し、本クラスでは
 * 「どの実装パターンへ委譲するか」だけを明示します。
 * </p>
 */
@RestController
@RequiredArgsConstructor
public class OrderPatternComparisonController implements OrderService {

    private final SpringDataOrderSearchPattern springDataPattern;
    private final EntityManagerOrderSearchPattern entityManagerPattern;
    private final MyBatisOrderSearchPattern myBatisPattern;

    /**
     * {@inheritDoc}
     * <p>
     * Spring Data JPA + Specification 実装へ委譲します。
     * </p>
     */
    @Override
    public List<OrderSearchView> searchSpecBySpringData(SearchCond cond) {
        return springDataPattern.searchBySpecification(cond);
    }

    /**
     * {@inheritDoc}
     * <p>
     * EntityManager + Criteria API 実装へ委譲します。
     * </p>
     */
    @Override
    public List<OrderSearchView> searchCriteriaByEntityManager(SearchCond cond) {
        return entityManagerPattern.searchByCriteria(cond, 100);
    }

    /**
     * {@inheritDoc}
     * <p>
     * MyBatis の動的SQL実装へ委譲します。
     * </p>
     */
    @Override
    public List<OrderSearchView> searchDynamicByMyBatis(SearchCond cond) {
        return myBatisPattern.searchDynamically(cond);
    }

    /**
     * {@inheritDoc}
     * <p>
     * EntityManager 実装の Named JPQL を実行します。
     * </p>
     */
    @Override
    public List<OrderSearchView> namedJpqlByEntityManager(String kw) {
        return entityManagerPattern.searchByNamedJpql(kw);
    }

    /**
     * {@inheritDoc}
     * <p>
     * EntityManager 実装の Named Native Query を実行します。
     * </p>
     */
    @Override
    public List<OrderStatusSummary> namedNativeByEntityManager(String status) {
        return entityManagerPattern.summarizeByNamedNative(status);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spring Data JPA 実装の Named JPQL を実行します。
     * </p>
     */
    @Override
    public List<OrderSearchView> namedJpqlBySpringData(String kw) {
        return springDataPattern.searchByNamedJpql(kw);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spring Data JPA 実装の Named Native Query を実行します。
     * </p>
     */
    @Override
    public List<OrderStatusSummary> namedNativeBySpringData(String status) {
        return springDataPattern.summarizeByNamedNative(status);
    }

    /**
     * {@inheritDoc}
     * <p>
     * EntityManager 実装で Hibernate TupleTransformer を利用する経路です。
     * </p>
     */
    @Override
    public List<OrderStatusSummary> summaryByStatusTupleTransformer(String status) {
        return entityManagerPattern.summarizeByTupleTransformer(status);
    }

    /**
     * {@inheritDoc}
     * <p>
     * MyBatis 実装の集計SQLを実行します。
     * </p>
     */
    @Override
    public List<OrderStatusSummary> summaryByStatusMyBatis(String status) {
        return myBatisPattern.summarizeByStatus(status);
    }
}
