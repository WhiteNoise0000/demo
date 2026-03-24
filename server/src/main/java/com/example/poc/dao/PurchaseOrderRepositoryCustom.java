package com.example.poc.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.example.poc.dao.support.EntityManagerBackedFragment;
import com.example.poc.dto.SearchCond;
import com.example.poc.entity.Customer;
import com.example.poc.entity.PurchaseOrder;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * 受注 Repository に対する custom fragment です。
 * <p>
 * `default` メソッドに Criteria API や移行過渡期の `saveOrUpdate` を寄せ、
 * 実装クラス側は `EntityManager` 提供だけで済むようにしています。
 * </p>
 */
public interface PurchaseOrderRepositoryCustom extends EntityManagerBackedFragment<PurchaseOrder, Long> {

    @Override
    default Class<PurchaseOrder> domainClass() {
        return PurchaseOrder.class;
    }

    /**
     * `purchase_orders` ベースの動的検索を Criteria API で実行します。
     * <p>
     * `customerNameKeyword` は `customers` への subquery で解決します。
     * </p>
     *
     * @param cond 検索条件
     * @param limit 最大件数（0以下は上限なし）
     * @return 条件に一致した受注一覧
     */
    @Transactional(readOnly = true)
    default List<PurchaseOrder> searchByCriteria(SearchCond cond, int limit) {
        CriteriaQuery<PurchaseOrder> query = criteriaQuery();
        Root<PurchaseOrder> root = root(query);
        List<Predicate> predicates = buildPredicates(cond, query, root);

        query.select(root)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(criteriaBuilder().desc(root.get("createdAt")));

        var typedQuery = entityManager().createQuery(query);
        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        return typedQuery.getResultList();
    }

    /**
     * 旧 Hibernate `saveOrUpdate` に近い振る舞いを移行過渡期向けに提供します。
     * <p>
     * update は `id + version` 一致時のみ成功し、未一致時は未存在判定のうえ insert 可否を判断します。
     * </p>
     *
     * @param order 保存対象
     * @return 保存後の最新状態
     */
    @Deprecated(forRemoval = false)
    @Transactional
    default PurchaseOrder saveOrUpdate(PurchaseOrder order) {
        validate(order);

        int updated = entityManager().createQuery("""
                update PurchaseOrder p
                   set p.customerId = :customerId,
                       p.status = :status,
                       p.total = :total,
                       p.createdAt = :createdAt,
                       p.version = p.version + 1
                 where p.id = :id
                   and p.version = :version
                """)
                .setParameter("customerId", order.getCustomerId())
                .setParameter("status", order.getStatus())
                .setParameter("total", order.getTotal())
                .setParameter("createdAt", order.getCreatedAt())
                .setParameter("id", order.getId())
                .setParameter("version", order.getVersion())
                .executeUpdate();

        if (updated == 1) {
            entityManager().clear();
            return entityManager().find(PurchaseOrder.class, order.getId());
        }

        if (!isPersisted(order.getId()) && Long.valueOf(0L).equals(order.getVersion())) {
            try {
                entityManager().createNativeQuery("""
                        insert into purchase_orders (id, customer_id, status, total, created_at, version)
                        values (:id, :customerId, :status, :total, :createdAt, :version)
                        """)
                        .setParameter("id", order.getId())
                        .setParameter("customerId", order.getCustomerId())
                        .setParameter("status", order.getStatus())
                        .setParameter("total", order.getTotal())
                        .setParameter("createdAt", order.getCreatedAt())
                        .setParameter("version", order.getVersion())
                        .executeUpdate();
                entityManager().clear();
                return entityManager().find(PurchaseOrder.class, order.getId());
            } catch (RuntimeException ex) {
                throw new ObjectOptimisticLockingFailureException(PurchaseOrder.class, order.getId(), ex);
            }
        }

        throw new ObjectOptimisticLockingFailureException(PurchaseOrder.class, order.getId());
    }

    /**
     * 旧名称を残した互換エイリアスです。
     *
     * @param order 保存対象
     * @return 保存後の最新状態
     */
    @Deprecated(forRemoval = false)
    default PurchaseOrder saveOrUpdateLikeLegacy(PurchaseOrder order) {
        return saveOrUpdate(order);
    }

    @Override
    @Transactional(readOnly = true)
    default boolean isPersisted(Long id) {
        return EntityManagerBackedFragment.super.isPersisted(id);
    }

    private List<Predicate> buildPredicates(SearchCond cond,
            CriteriaQuery<PurchaseOrder> query,
            Root<PurchaseOrder> root) {
        List<Predicate> predicates = new ArrayList<>();
        if (cond == null) {
            return predicates;
        }

        if (StringUtils.hasText(cond.getCustomerNameKeyword())) {
            Subquery<Long> customerSubquery = query.subquery(Long.class);
            Root<Customer> customerRoot = customerSubquery.from(Customer.class);
            customerSubquery.select(customerRoot.get("id"))
                    .where(criteriaBuilder().like(
                            criteriaBuilder().lower(customerRoot.get("name")),
                            "%" + cond.getCustomerNameKeyword().toLowerCase() + "%"));
            predicates.add(root.get("customerId").in(customerSubquery));
        }
        if (cond.getStatuses() != null && !cond.getStatuses().isEmpty()) {
            predicates.add(root.get("status").in(cond.getStatuses()));
        }
        if (cond.getCreatedAtFrom() != null) {
            predicates.add(criteriaBuilder().greaterThanOrEqualTo(root.get("createdAt"), cond.getCreatedAtFrom()));
        }
        if (cond.getCreatedAtTo() != null) {
            predicates.add(criteriaBuilder().lessThanOrEqualTo(root.get("createdAt"), cond.getCreatedAtTo()));
        }
        if (cond.getTotalGte() != null) {
            predicates.add(criteriaBuilder().greaterThanOrEqualTo(root.get("total"), cond.getTotalGte()));
        }
        return predicates;
    }

    private void validate(PurchaseOrder order) {
        Assert.notNull(order, "order must not be null");
        Assert.notNull(order.getId(), "id must not be null");
        Assert.notNull(order.getVersion(), "version must not be null");
        Assert.isTrue(order.getVersion() >= 0L, "version must be >= 0");
    }
}
