package com.example.poc.dao.jdbcquery;

import java.util.Optional;

import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import jakarta.persistence.EntityManager;

/**
 * {@link JdbcTemplateQuery} を QueryLookupStrategy に差し込む JPA RepositoryFactory です。
 */
class JdbcTemplateQueryJpaRepositoryFactory extends JpaRepositoryFactory {

    private final EntityManager entityManager;

    private final NamedParameterJdbcOperations jdbcOperations;

    private final ResourcePatternResolver resourceResolver;

    JdbcTemplateQueryJpaRepositoryFactory(EntityManager entityManager,
            NamedParameterJdbcOperations jdbcOperations,
            ResourcePatternResolver resourceResolver) {
        super(entityManager);
        this.entityManager = entityManager;
        this.jdbcOperations = jdbcOperations;
        this.resourceResolver = resourceResolver;
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key,
            ValueExpressionDelegate valueExpressionDelegate) {
        return super.getQueryLookupStrategy(key, valueExpressionDelegate)
                .map(delegate -> new JdbcTemplateQueryLookupStrategy(
                        delegate,
                        entityManager,
                        jdbcOperations,
                        resourceResolver));
    }
}
