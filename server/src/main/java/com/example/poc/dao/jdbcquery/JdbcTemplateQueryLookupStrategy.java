package com.example.poc.dao.jdbcquery;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import jakarta.persistence.EntityManager;

/**
 * 独自アノテーション付きメソッドのみ JDBC 実行へ委譲します。
 */
class JdbcTemplateQueryLookupStrategy implements QueryLookupStrategy {

    private final QueryLookupStrategy delegate;

    private final EntityManager entityManager;

    private final NamedParameterJdbcOperations jdbcOperations;

    private final ResourcePatternResolver resourceResolver;

    JdbcTemplateQueryLookupStrategy(QueryLookupStrategy delegate,
            EntityManager entityManager,
            NamedParameterJdbcOperations jdbcOperations,
            ResourcePatternResolver resourceResolver) {
        this.delegate = delegate;
        this.entityManager = entityManager;
        this.jdbcOperations = jdbcOperations;
        this.resourceResolver = resourceResolver;
    }

    @Override
    public RepositoryQuery resolveQuery(Method method,
            RepositoryMetadata metadata,
            ProjectionFactory factory,
            NamedQueries namedQueries) {
        JdbcTemplateQuery annotation = AnnotatedElementUtils.findMergedAnnotation(method, JdbcTemplateQuery.class);
        if (annotation == null) {
            return delegate.resolveQuery(method, metadata, factory, namedQueries);
        }
        return new JdbcTemplateRepositoryQuery(method, metadata, factory, annotation, entityManager, jdbcOperations,
                resourceResolver);
    }
}
