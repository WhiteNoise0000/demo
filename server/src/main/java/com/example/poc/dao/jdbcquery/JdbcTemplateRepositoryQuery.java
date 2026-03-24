package com.example.poc.dao.jdbcquery;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.BeanUtils;
import org.springframework.core.CollectionFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;

/**
 * SQL ファイルを読み込み、NamedParameterJdbcTemplate で実行する RepositoryQuery 実装です。
 * <p>
 * サンプル実装のため、{@code Pageable}/{@code Sort}/更新系 DML は対象外です。
 * </p>
 */
class JdbcTemplateRepositoryQuery implements RepositoryQuery {

    private static final ConcurrentMap<String, String> SQL_CACHE = new ConcurrentHashMap<>();

    private final Method method;

    private final RepositoryMetadata metadata;

    private final QueryMethod queryMethod;

    private final JdbcTemplateQuery annotation;

    private final EntityManager entityManager;

    private final NamedParameterJdbcOperations jdbcOperations;

    private final ResourcePatternResolver resourceResolver;

    JdbcTemplateRepositoryQuery(Method method,
            RepositoryMetadata metadata,
            ProjectionFactory projectionFactory,
            JdbcTemplateQuery annotation,
            EntityManager entityManager,
            NamedParameterJdbcOperations jdbcOperations,
            ResourcePatternResolver resourceResolver) {
        this.method = method;
        this.metadata = metadata;
        this.queryMethod = new QueryMethod(method, metadata, projectionFactory);
        this.annotation = annotation;
        this.entityManager = entityManager;
        this.jdbcOperations = jdbcOperations;
        this.resourceResolver = resourceResolver;

        validateSupportedMethodShape();
    }

    @Override
    public Object execute(Object[] parameters) {
        if (annotation.flushAutomatically()) {
            entityManager.flush();
        }

        String sql = resolveSql();
        SqlParameterSource parameterSource = createParameterSource(parameters);
        Class<?> returnedType = queryMethod.getReturnedObjectType();

        if (queryMethod.isCollectionQuery()) {
            List<?> rows = jdbcOperations.query(sql, parameterSource, createRowMapper(returnedType));
            return adaptCollectionResult(rows);
        }

        if (isOptionalReturnType()) {
            List<?> rows = jdbcOperations.query(sql, parameterSource, createRowMapper(returnedType));
            return Optional.ofNullable(singleResult(rows));
        }

        if (isScalarType(returnedType)) {
            return jdbcOperations.queryForObject(sql, parameterSource, returnedType);
        }

        List<?> rows = jdbcOperations.query(sql, parameterSource, createRowMapper(returnedType));
        return singleResult(rows);
    }

    @Override
    public QueryMethod getQueryMethod() {
        return queryMethod;
    }

    private void validateSupportedMethodShape() {
        Parameters<?, ?> parameters = queryMethod.getParameters();
        Assert.state(!queryMethod.isPageQuery(), "@JdbcTemplateQuery sample does not support Page return types");
        Assert.state(!queryMethod.isSliceQuery(), "@JdbcTemplateQuery sample does not support Slice return types");
        Assert.state(!queryMethod.isScrollQuery(), "@JdbcTemplateQuery sample does not support Scroll return types");
        Assert.state(!queryMethod.isStreamQuery(), "@JdbcTemplateQuery sample does not support Stream return types");
        Assert.state(!parameters.hasPageableParameter(), "@JdbcTemplateQuery sample does not support Pageable");
        Assert.state(!parameters.hasSortParameter(), "@JdbcTemplateQuery sample does not support Sort");
    }

    private String resolveSql() {
        String resourceLocation = resolveSqlLocation();
        return SQL_CACHE.computeIfAbsent(resourceLocation, this::readSql);
    }

    private String resolveSqlLocation() {
        if (!StringUtils.hasText(annotation.value())) {
            return "classpath:sql/" + metadata.getRepositoryInterface().getSimpleName() + "." + method.getName() + ".sql";
        }

        String configured = annotation.value().trim();
        if (configured.startsWith("classpath:") || configured.startsWith("classpath*:")) {
            return configured;
        }
        if (configured.endsWith(".sql")) {
            return "classpath:sql/" + configured;
        }
        return "classpath:sql/" + configured + ".sql";
    }

    private String readSql(String resourceLocation) {
        Resource resource = resourceResolver.getResource(resourceLocation);
        Assert.state(resource.exists(), "SQL resource not found: " + resourceLocation);
        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read SQL resource: " + resourceLocation, ex);
        }
    }

    private SqlParameterSource createParameterSource(Object[] arguments) {
        Parameters<?, ?> bindableParameters = queryMethod.getParameters().getBindableParameters();
        if (bindableParameters.getNumberOfParameters() == 0) {
            return new MapSqlParameterSource();
        }

        if (bindableParameters.getNumberOfParameters() == 1) {
            Parameter parameter = bindableParameters.getParameter(0);
            Object value = arguments[parameter.getIndex()];
            SqlParameterSource source = createSingleParameterSource(parameter, value);
            if (source != null) {
                return source;
            }
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        for (Parameter parameter : bindableParameters) {
            Assert.state(parameter.isNamedParameter(),
                    () -> "Named parameters are required. Add @Param to " + method);
            params.addValue(parameter.getRequiredName(), arguments[parameter.getIndex()]);
        }
        return params;
    }

    private SqlParameterSource createSingleParameterSource(Parameter parameter, Object value) {
        if (value instanceof SqlParameterSource sqlParameterSource) {
            return sqlParameterSource;
        }
        if (value instanceof Map<?, ?> map) {
            return new MapSqlParameterSource(toStringKeyMap(map));
        }
        if (!parameter.isNamedParameter() && value != null && !isScalarType(value.getClass())) {
            return new BeanPropertySqlParameterSource(value);
        }
        if (parameter.isNamedParameter()) {
            return new MapSqlParameterSource(parameter.getRequiredName(), value);
        }
        return null;
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> source) {
        Map<String, Object> converted = new LinkedHashMap<>();
        source.forEach((key, value) -> converted.put(String.valueOf(key), value));
        return converted;
    }

    private RowMapper<?> createRowMapper(Class<?> rowType) {
        if (isScalarType(rowType)) {
            return SingleColumnRowMapper.newInstance(rowType);
        }
        return DataClassRowMapper.newInstance(rowType);
    }

    private Object adaptCollectionResult(List<?> rows) {
        Class<?> declaredReturnType = method.getReturnType();
        if (declaredReturnType.isInstance(rows)) {
            return rows;
        }

        @SuppressWarnings("unchecked")
        Collection<Object> target = (Collection<Object>) CollectionFactory.createCollection(declaredReturnType,
                rows.size());
        target.addAll(rows);
        return target;
    }

    private boolean isOptionalReturnType() {
        return Optional.class.equals(method.getReturnType());
    }

    private boolean isScalarType(Class<?> type) {
        return BeanUtils.isSimpleValueType(type) || Enum.class.isAssignableFrom(type);
    }

    private Object singleResult(List<?> rows) {
        if (rows.isEmpty()) {
            return null;
        }
        if (rows.size() > 1) {
            throw new IncorrectResultSizeDataAccessException(1, rows.size());
        }
        return rows.get(0);
    }
}
