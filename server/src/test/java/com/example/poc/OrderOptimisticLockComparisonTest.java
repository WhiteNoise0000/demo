package com.example.poc;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.example.poc.dao.PurchaseOrderRepository;
import com.example.poc.entity.PurchaseOrder;

import jakarta.persistence.EntityManager;

@DataJpaTest
class OrderOptimisticLockComparisonTest {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void save_existingRow_shouldIncrementVersion() {
        PurchaseOrder existing = purchaseOrderRepository.findById(1002L).orElseThrow();
        assertThat(existing.getVersion()).isEqualTo(0L);

        existing.setStatus("SHIPPED");
        PurchaseOrder updated = purchaseOrderRepository.saveAndFlush(existing);

        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    void save_withPresetIdAndVersionForNewRow_shouldFail() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(9001L);
        order.setCustomerId(1L);
        order.setStatus("NEW");
        order.setTotal(new BigDecimal("123.45"));
        order.setCreatedAt(Instant.parse("2026-02-01T00:00:00Z"));
        order.setVersion(0L);

        assertThatThrownBy(() -> purchaseOrderRepository.saveAndFlush(order))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void save_shouldRejectStaleVersion() {
        PurchaseOrder current = purchaseOrderRepository.findById(1001L).orElseThrow();
        PurchaseOrder stale = detachedCopy(current);

        current.setStatus("PAID");
        purchaseOrderRepository.saveAndFlush(current);
        entityManager.clear();

        stale.setStatus("CANCELLED");
        assertThatThrownBy(() -> purchaseOrderRepository.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void saveOrUpdateLikeLegacy_shouldInsertWhenNotPersistedAndVersionZero() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(9101L);
        order.setCustomerId(1L);
        order.setStatus("NEW");
        order.setTotal(new BigDecimal("333.33"));
        order.setCreatedAt(Instant.parse("2026-02-01T00:00:00Z"));
        order.setVersion(0L);

        PurchaseOrder inserted = purchaseOrderRepository.saveOrUpdateLikeLegacy(order);

        assertThat(inserted.getVersion()).isEqualTo(0L);
        assertThat(purchaseOrderRepository.isPersisted(9101L)).isTrue();
    }

    @Test
    void saveOrUpdateLikeLegacy_shouldIncrementVersionForUpdate() {
        PurchaseOrder current = purchaseOrderRepository.findById(1003L).orElseThrow();
        PurchaseOrder detached = detachedCopy(current);
        detached.setStatus("PAID");

        PurchaseOrder updated = purchaseOrderRepository.saveOrUpdateLikeLegacy(detached);

        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(updated.getStatus()).isEqualTo("PAID");
    }

    @Test
    void saveOrUpdateLikeLegacy_shouldRejectStaleVersion() {
        PurchaseOrder current = purchaseOrderRepository.findById(1004L).orElseThrow();
        PurchaseOrder latest = detachedCopy(current);
        PurchaseOrder stale = detachedCopy(current);

        latest.setStatus("SHIPPED");
        purchaseOrderRepository.saveOrUpdateLikeLegacy(latest);

        stale.setStatus("CANCELLED");
        assertThatThrownBy(() -> purchaseOrderRepository.saveOrUpdateLikeLegacy(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void isPersisted_shouldReturnTrueOnlyForExistingId() {
        assertThat(purchaseOrderRepository.isPersisted(1001L)).isTrue();
        assertThat(purchaseOrderRepository.isPersisted(999999L)).isFalse();
    }

    private PurchaseOrder detachedCopy(PurchaseOrder source) {
        PurchaseOrder copy = new PurchaseOrder();
        copy.setId(source.getId());
        copy.setCustomerId(source.getCustomerId());
        copy.setStatus(source.getStatus());
        copy.setTotal(source.getTotal());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setVersion(source.getVersion());
        return copy;
    }
}
