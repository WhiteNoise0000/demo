package com.example.poc.dao.jdbcquery;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.Assert;

import jakarta.persistence.EntityManager;

/**
 * {@link JdbcTemplateQuery} を解決できる Spring Data JPA 用 FactoryBean です。
 */
public class JdbcTemplateQueryJpaRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
        extends JpaRepositoryFactoryBean<T, S, ID> {

    private BeanFactory beanFactory;

    private EntityPathResolver entityPathResolver = SimpleEntityPathResolver.INSTANCE;

    private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;

    private JpaQueryMethodFactory queryMethodFactory;

    public JdbcTemplateQueryJpaRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEntityPathResolver(ObjectProvider<EntityPathResolver> resolver) {
        super.setEntityPathResolver(resolver);
        this.entityPathResolver = resolver.getIfAvailable(() -> SimpleEntityPathResolver.INSTANCE);
    }

    @Override
    public void setQueryMethodFactory(JpaQueryMethodFactory queryMethodFactory) {
        super.setQueryMethodFactory(queryMethodFactory);
        this.queryMethodFactory = queryMethodFactory;
    }

    @Override
    public void setEscapeCharacter(char escapeCharacter) {
        super.setEscapeCharacter(escapeCharacter);
        this.escapeCharacter = EscapeCharacter.of(escapeCharacter);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        Assert.state(beanFactory instanceof ListableBeanFactory, "ListableBeanFactory is required");

        NamedParameterJdbcOperations jdbcOperations =
                ((ListableBeanFactory) beanFactory).getBean(NamedParameterJdbcOperations.class);
        ResourcePatternResolver resourceResolver =
                (beanFactory instanceof ResourcePatternResolver candidate)
                        ? candidate
                        : new PathMatchingResourcePatternResolver();

        JdbcTemplateQueryJpaRepositoryFactory factory =
                new JdbcTemplateQueryJpaRepositoryFactory(entityManager, jdbcOperations, resourceResolver);
        factory.setEntityPathResolver(entityPathResolver);
        factory.setEscapeCharacter(escapeCharacter);
        if (queryMethodFactory != null) {
            factory.setQueryMethodFactory(queryMethodFactory);
        }
        return factory;
    }
}
