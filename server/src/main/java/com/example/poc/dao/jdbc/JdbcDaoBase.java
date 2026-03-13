package com.example.poc.dao.jdbc;

import java.util.List;
import java.util.Map;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * {@link NamedParameterJdbcTemplate} を使う DAO の共通基底です。
 * <p>
 * AliasToBean 相当の PoC として、BeanPropertyRowMapper を標準マッパーに寄せます。
 * </p>
 */
public abstract class JdbcDaoBase {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    protected JdbcDaoBase(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    protected final <T> List<T> queryList(String sql, Map<String, ?> params, Class<T> mappedClass) {
        return queryList(sql, new MapSqlParameterSource(params), mappedClass);
    }

    protected final <T> List<T> queryList(String sql, SqlParameterSource params, Class<T> mappedClass) {
        return jdbcTemplate.query(sql, params, beanPropertyRowMapper(mappedClass));
    }

    protected final int update(String sql, SqlParameterSource params) {
        return jdbcTemplate.update(sql, params);
    }

    protected final void execute(String sql) {
        jdbcTemplate.getJdbcOperations().execute(sql);
    }

    private static <T> BeanPropertyRowMapper<T> beanPropertyRowMapper(Class<T> mappedClass) {
        BeanPropertyRowMapper<T> mapper = BeanPropertyRowMapper.newInstance(mappedClass);
        mapper.setConversionService(DefaultConversionService.getSharedInstance());
        return mapper;
    }
}
