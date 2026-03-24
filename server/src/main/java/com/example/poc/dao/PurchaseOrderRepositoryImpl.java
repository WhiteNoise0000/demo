package com.example.poc.dao;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * {@link PurchaseOrderRepositoryCustom} 向け fragment 実装です。
 * <p>
 * 実装クラス側は `EntityManager` 提供だけを担当し、振る舞いは interface default へ寄せます。
 * </p>
 */
@Repository
public class PurchaseOrderRepositoryImpl implements PurchaseOrderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public EntityManager entityManager() {
        return entityManager;
    }
}
