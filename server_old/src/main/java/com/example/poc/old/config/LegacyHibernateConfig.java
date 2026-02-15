package com.example.poc.old.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.example.poc.old.entity.LegacyPurchaseOrder;

/**
 * Spring 4.2 + Hibernate 5.1 構成の最小JavaConfigです。
 * <p>
 * Boot の自動設定は使わず、DataSource / SessionFactory / TransactionManager を
 * 明示的に定義して、旧実装の構成要素を見える形にしています。
 * </p>
 */
@Configuration
@ComponentScan(basePackages = "com.example.poc.old")
@EnableTransactionManagement
public class LegacyHibernateConfig {

    /**
     * メモリDB(H2)の接続設定です。
     *
     * @return DAO層で共有するDataSource
     */
    @Bean
    public DataSource dataSource() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:legacydb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    /**
     * Hibernate の SessionFactory を構築します。
     *
     * @param dataSource DB接続情報
     * @return Spring管理のSessionFactory Bean
     */
    @Bean
    public LocalSessionFactoryBean sessionFactory(DataSource dataSource) {
        LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setAnnotatedClasses(LegacyPurchaseOrder.class);
        // Native SQL は hbm.xml に外だしし、DAO から Named SQL として呼び出す。
        factoryBean.setMappingLocations(new ClassPathResource("mappings/LegacyOrderQueries.hbm.xml"));
        factoryBean.setHibernateProperties(hibernateProperties());
        return factoryBean;
    }

    /**
     * `@Transactional` を Hibernate セッションに橋渡しするためのトランザクション管理Beanです。
     *
     * @param sessionFactory HibernateのSessionFactory
     * @return HibernateTransactionManager
     */
    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }

    /**
     * Hibernate 5.1 用の最低限の実行プロパティです。
     *
     * @return Hibernateプロパティ
     */
    private Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.show_sql", "true");
        properties.setProperty("hibernate.format_sql", "true");
        return properties;
    }
}
