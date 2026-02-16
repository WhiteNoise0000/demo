package com.example.poc.old;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.OptimisticLockingFailureException;

import com.example.poc.old.config.LegacyHibernateConfig;
import com.example.poc.old.dao.LegacyOrderDao;
import com.example.poc.old.entity.LegacyPurchaseOrder;

class LegacyOptimisticLockComparisonTest {

    @Test
    void saveOrUpdate_shouldIncrementVersionAndRejectStaleVersion() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(LegacyHibernateConfig.class)) {
            LegacyOrderDao dao = context.getBean(LegacyOrderDao.class);
            Long id = 99001L;

            LegacyPurchaseOrder created = newOrder(id, "NEW", 0L);
            dao.saveOrUpdate(created);

            LegacyPurchaseOrder afterInsert = dao.findById(id);
            assertNotNull(afterInsert);
            assertEquals(Long.valueOf(0L), afterInsert.getVersion());

            LegacyPurchaseOrder latest = newOrder(id, "PAID", afterInsert.getVersion());
            dao.saveOrUpdate(latest);

            LegacyPurchaseOrder afterUpdate = dao.findById(id);
            assertNotNull(afterUpdate);
            assertEquals(Long.valueOf(1L), afterUpdate.getVersion());

            LegacyPurchaseOrder stale = newOrder(id, "CANCELLED", 0L);
            assertThrows(OptimisticLockingFailureException.class, () -> dao.saveOrUpdate(stale));
        }
    }

    private LegacyPurchaseOrder newOrder(Long id, String status, Long version) {
        LegacyPurchaseOrder order = new LegacyPurchaseOrder(
                id,
                "Legacy Optimistic Lock",
                status,
                new BigDecimal("100.00"),
                new Date());
        order.setVersion(version);
        return order;
    }
}
