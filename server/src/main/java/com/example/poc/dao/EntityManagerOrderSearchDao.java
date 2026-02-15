package com.example.poc.dao;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

import com.example.poc.common.hibernate.TupleTransformerFactory;
import com.example.poc.dto.OrderStatusSummary;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.OrderSearchView;

import lombok.AllArgsConstructor;

/**
 * 新実装側の「EntityManager パターン」を表すDAOです。
 * <p>
 * Criteria API と Named Query（JPQL/Native）を使う実装を1か所にまとめ、
 * Spring Data パターンとの対比をしやすくしています。
 * </p>
 */
@Repository
@AllArgsConstructor
public class EntityManagerOrderSearchDao {

    private final TupleTransformerFactory tupleTransformerFactory;

    @PersistenceContext
    private final EntityManager entityManager;

    /**
     * Criteria API で動的検索を実行します。
     *
     * @param cond 検索条件
     * @param limit 最大件数（0以下は上限なし）
     * @return 条件に一致した受注一覧
     */
    public List<OrderSearchView> searchByCriteriaWithLimit(SearchCond cond, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<OrderSearchView> cq = cb.createQuery(OrderSearchView.class);
        Root<OrderSearchView> root = cq.from(OrderSearchView.class);

        List<Predicate> predicates = new ArrayList<>();
        if (cond != null) {
            if (cond.getCustomerNameKeyword() != null && !cond.getCustomerNameKeyword().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("customerName")),
                        "%" + cond.getCustomerNameKeyword().toLowerCase() + "%"));
            }
            if (cond.getStatuses() != null && !cond.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(cond.getStatuses()));
            }
            if (cond.getCreatedAtFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), cond.getCreatedAtFrom()));
            }
            if (cond.getCreatedAtTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), cond.getCreatedAtTo()));
            }
            if (cond.getTotalGte() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("total"), cond.getTotalGte()));
            }
        }

        cq.select(root)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.desc(root.get("createdAt")));

        TypedQuery<OrderSearchView> query = entityManager.createQuery(cq);
        if (limit > 0) {
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }

    /**
     * Named JPQL を実行します。
     *
     * @param keyword 顧客名の部分一致キーワード
     * @return 条件に一致した受注一覧
     */
    public List<OrderSearchView> runNamedJpql(String keyword) {
        String value = keyword == null ? "" : keyword;
        return entityManager.createNamedQuery("OrderSearchView.findByCustomerNameLike", OrderSearchView.class)
                .setParameter(1, value)
                .getResultList();
    }

    /**
     * Named Native Query + ResultSetMapping を実行します。
     *
     * @param status 集計対象のステータス（null時は全件）
     * @return ステータス別集計
     */
    public List<OrderStatusSummary> runNamedNative(String status) {
        return entityManager.createNamedQuery("OrderSearchView.summaryByStatusNative", OrderStatusSummary.class)
                .setParameter("status", status)
                .getResultList();
    }

    /**
     * Native Query の結果を Hibernate TupleTransformer で DTO へ変換します。
     *
     * @param status 集計対象のステータス（null時は全件）
     * @return ステータス別集計
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<OrderStatusSummary> runNativeWithTupleTransformer(String status) {
        // SQLは orm.xml の Named Native Query に寄せ、DAO側は実行と変換に専念する。
        NativeQuery nativeQuery = entityManager
                .createNamedQuery("OrderSearchView.summaryByStatusNativeForTupleTransformer")
                .setParameter("status", status)
                .unwrap(NativeQuery.class);

        // DTO型をキーにFactoryから共有Transformerを取得し、都度newを避ける。
        NativeQuery<OrderStatusSummary> mappedQuery = nativeQuery
                .setTupleTransformer(tupleTransformerFactory.aliasToBean(OrderStatusSummary.class));
        return mappedQuery.getResultList();
    }
}
