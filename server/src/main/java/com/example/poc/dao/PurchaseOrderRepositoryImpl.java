package com.example.poc.dao;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.example.poc.entity.PurchaseOrder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 旧 saveOrUpdate 相当の挙動を、ID事前採番 + version事前設定前提で実現する実装です。
 */
@Repository
@Transactional
public class PurchaseOrderRepositoryImpl implements PurchaseOrderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PurchaseOrder saveOrUpdateLikeLegacy(PurchaseOrder order) {
        validate(order);

        int updated = entityManager.createQuery("""
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
            entityManager.clear();
            return entityManager.find(PurchaseOrder.class, order.getId());
        }

        if (!isPersisted(order.getId()) && Long.valueOf(0L).equals(order.getVersion())) {
            try {
                entityManager.createNativeQuery("""
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
                entityManager.clear();
                return entityManager.find(PurchaseOrder.class, order.getId());
            } catch (RuntimeException ex) {
                throw new ObjectOptimisticLockingFailureException(PurchaseOrder.class, order.getId(), ex);
            }
        }

        throw new ObjectOptimisticLockingFailureException(PurchaseOrder.class, order.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPersisted(Long id) {
        if (id == null) {
            return false;
        }
        return entityManager.find(PurchaseOrder.class, id) != null;
    }

    private void validate(PurchaseOrder order) {
        Assert.notNull(order, "order must not be null");
        Assert.notNull(order.getId(), "id must not be null");
        Assert.notNull(order.getVersion(), "version must not be null");
        Assert.isTrue(order.getVersion() >= 0L, "version must be >= 0");
    }
}
