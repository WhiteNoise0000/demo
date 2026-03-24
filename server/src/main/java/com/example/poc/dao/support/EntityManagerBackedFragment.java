package com.example.poc.dao.support;

import org.springframework.data.repository.NoRepositoryBean;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * custom repository fragment から EntityManager を扱うための共通基底です。
 *
 * @param <T> エンティティ型
 * @param <ID> 識別子型
 */
@NoRepositoryBean
public interface EntityManagerBackedFragment<T, ID> {

    /**
     * fragment 実装が保持する EntityManager を返します。
     *
     * @return EntityManager
     */
    EntityManager entityManager();

    /**
     * 対象エンティティ型を返します。
     *
     * @return エンティティ型
     */
    Class<T> domainClass();

    /**
     * CriteriaBuilder を返します。
     *
     * @return CriteriaBuilder
     */
    default CriteriaBuilder criteriaBuilder() {
        return entityManager().getCriteriaBuilder();
    }

    /**
     * 対象エンティティ向けの CriteriaQuery を返します。
     *
     * @return CriteriaQuery
     */
    default CriteriaQuery<T> criteriaQuery() {
        return criteriaBuilder().createQuery(domainClass());
    }

    /**
     * CriteriaQuery に対象エンティティの root を追加します。
     *
     * @param query CriteriaQuery
     * @return root
     */
    default Root<T> root(CriteriaQuery<T> query) {
        return query.from(domainClass());
    }

    /**
     * 指定 ID が永続化済みであれば true を返します。
     *
     * @param id 主キー
     * @return 永続化済みなら true
     */
    default boolean isPersisted(ID id) {
        return id != null && entityManager().find(domainClass(), id) != null;
    }
}
