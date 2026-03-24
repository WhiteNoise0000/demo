package com.example.poc.dao.jdbcquery;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Spring Data JPA Repository メソッドを NamedParameterJdbcTemplate 実行へ振り分けます。
 * <p>
 * {@code value} 未指定時は {@code classpath:sql/<RepositorySimpleName>.<methodName>.sql}
 * を既定の SQL 配置場所として扱います。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JdbcTemplateQuery {

    /**
     * SQL リソースの場所です。
     * <p>
     * 未指定時は {@code classpath:sql/<RepositorySimpleName>.<methodName>.sql} を解決します。
     * {@code classpath:} 省略時は {@code classpath:sql/} 配下として扱います。
     * </p>
     *
     * @return SQL リソースパス
     */
    String value() default "";

    /**
     * 実行前に JPA の永続化コンテキストを flush するかを示します。
     *
     * @return flush が必要なら true
     */
    boolean flushAutomatically() default false;
}
