package com.example.poc.common.hibernate;

import java.util.List;
import java.util.Map;

import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;

/**
 * Hibernate の NativeQuery 実行を集約する薄いヘルパーです。
 * <p>
 * SQL別名とDTOプロパティ名の対応付けは {@link AliasToBeanTupleTransformer} に委譲し、
 * 呼び出し側は「SQL + パラメータ + DTO型」だけを渡せばよい形に寄せます。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class NativeQueryExecutor {

    private final EntityManager entityManager;
    private final TupleTransformerFactory tupleTransformerFactory;

    /**
     * インラインSQLを実行し、結果を DTO へ変換して返します。
     *
     * @param sql 実行SQL
     * @param targetType 変換先DTO型
     * @param <T> DTO型
     * @return 変換後DTO一覧
     */
    public <T> List<T> list(String sql, Class<T> targetType) {
        return list(sql, Map.of(), targetType);
    }

    /**
     * インラインSQLを実行し、結果を DTO へ変換して返します。
     *
     * @param sql 実行SQL
     * @param params 名前付きパラメータ
     * @param targetType 変換先DTO型
     * @param <T> DTO型
     * @return 変換後DTO一覧
     */
    public <T> List<T> list(String sql, Map<String, ?> params, Class<T> targetType) {
        Assert.hasText(sql, "sql must not be blank");
        return prepareQuery(entityManager.createNativeQuery(sql), params, targetType).getResultList();
    }

    /**
     * Named Native Query を実行し、結果を DTO へ変換して返します。
     *
     * @param queryName Named Native Query 名
     * @param targetType 変換先DTO型
     * @param <T> DTO型
     * @return 変換後DTO一覧
     */
    public <T> List<T> namedList(String queryName, Class<T> targetType) {
        return namedList(queryName, Map.of(), targetType);
    }

    /**
     * Named Native Query を実行し、結果を DTO へ変換して返します。
     *
     * @param queryName Named Native Query 名
     * @param params 名前付きパラメータ
     * @param targetType 変換先DTO型
     * @param <T> DTO型
     * @return 変換後DTO一覧
     */
    public <T> List<T> namedList(String queryName, Map<String, ?> params, Class<T> targetType) {
        Assert.hasText(queryName, "queryName must not be blank");
        return prepareQuery(entityManager.createNamedQuery(queryName), params, targetType).getResultList();
    }

    @SuppressWarnings("unchecked")
    private <T> NativeQuery<T> prepareQuery(Query query, Map<String, ?> params, Class<T> targetType) {
        Assert.notNull(query, "query must not be null");
        Assert.notNull(params, "params must not be null");
        Assert.notNull(targetType, "targetType must not be null");

        bindParameters(query, params);
        return query.unwrap(NativeQuery.class)
                .setTupleTransformer(tupleTransformerFactory.aliasToBean(targetType));
    }

    private void bindParameters(Query query, Map<String, ?> params) {
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }
}
