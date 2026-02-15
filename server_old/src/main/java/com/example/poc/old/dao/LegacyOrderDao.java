package com.example.poc.old.dao;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.poc.old.dto.LegacyOrderStatusSummary;
import com.example.poc.old.entity.LegacyPurchaseOrder;

/**
 * 旧実装スタイルのDAOです。
 * <p>
 * Session API を直接扱うため処理は追いやすい一方で、クエリの文字列安全性や
 * 型安全性は現代JPA実装より弱くなります。
 * </p>
 */
@Repository
public class LegacyOrderDao {

    private final SessionFactory sessionFactory;

    /**
     * @param sessionFactory 現在スレッドに紐づくSessionを取得するためのファクトリ
     */
    @Autowired
    public LegacyOrderDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * 注文を新規登録または更新します。
     *
     * @param order 保存対象の注文
     */
    @Transactional
    public void saveOrUpdate(LegacyPurchaseOrder order) {
        currentSession().saveOrUpdate(order);
    }

    /**
     * 作成日時の降順で注文を取得します。
     *
     * @param limit 最大取得件数
     * @return 新しい順の注文一覧
     */
    @Transactional(readOnly = true)
    public List<LegacyPurchaseOrder> findRecent(int limit) {
        Query query = currentSession().createQuery("from LegacyPurchaseOrder o order by o.createdAt desc");
        query.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<LegacyPurchaseOrder> rows = query.list();
        return rows;
    }

    /**
     * 注文件数を取得します。
     *
     * @return 注文件数
     */
    @Transactional(readOnly = true)
    public long count() {
        Long count = (Long) currentSession()
                .createQuery("select count(o.id) from LegacyPurchaseOrder o")
                .uniqueResult();
        return count == null ? 0L : count;
    }

    /**
     * hbm.xml に定義した Named SQL を実行し、集計結果を DTO へ変換します。
     * <p>
     * 変換は Hibernate 標準の {@link AliasToBeanResultTransformer} を利用します。
     * </p>
     *
     * @param status 集計対象ステータス（null時は全件）
     * @return ステータス別集計
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Transactional(readOnly = true)
    public List<LegacyOrderStatusSummary> summarizeByStatusAliasToBean(String status) {
        SQLQuery query = (SQLQuery) currentSession()
                .getNamedQuery("LegacyPurchaseOrder.summaryByStatusAliasToBean");
        query.setParameter("status", status, StandardBasicTypes.STRING);
        query.addScalar("status", StandardBasicTypes.STRING);
        query.addScalar("orderCount", StandardBasicTypes.LONG);
        query.addScalar("totalSum", StandardBasicTypes.BIG_DECIMAL);
        query.setResultTransformer(new AliasToBeanResultTransformer(LegacyOrderStatusSummary.class));
        return query.list();
    }

    /**
     * 現在トランザクションに紐づくSessionを返します。
     *
     * @return 現在のHibernate Session
     */
    private Session currentSession() {
        return sessionFactory.getCurrentSession();
    }
}
