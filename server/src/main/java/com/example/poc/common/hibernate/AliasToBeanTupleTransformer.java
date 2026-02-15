package com.example.poc.common.hibernate;

import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.query.TupleTransformer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Hibernate の {@link TupleTransformer} 実装です。
 * <p>
 * SQLのエイリアス名を JavaBean のプロパティ名へ対応付けし、Spring の
 * {@link ConversionService} で型変換しながら値を設定します。
 * </p>
 * <p>
 * 本実装は「未知aliasを許容しない」方針です。想定外の列別名を検知した時点で
 * fail-fast させ、SQL側の設計ミスを早期に顕在化させます。
 * </p>
 *
 * @param <T> 変換先DTO型（デフォルトコンストラクタ + setter を想定）
 */
public class AliasToBeanTupleTransformer<T> implements TupleTransformer<T> {

    private static final String UNRESOLVED_PROPERTY = "__UNRESOLVED__";

    private final Class<T> targetType;
    private final ConversionService conversionService;
    private final Map<String, String> precomputedPropertyLookup;
    /** 生のalias文字列（引用符・大文字小文字を含む）に対する解決結果キャッシュ。 */
    private final ConcurrentMap<String, String> rawAliasCache = new ConcurrentHashMap<>();

    /**
     * shared conversion service を使うコンストラクタです。
     *
     * @param targetType 変換先DTO型
     */
    public AliasToBeanTupleTransformer(Class<T> targetType) {
        this(targetType, DefaultConversionService.getSharedInstance());
    }

    /**
     * 変換サービスを明示指定するコンストラクタです。
     *
     * @param targetType 変換先DTO型
     * @param conversionService 型変換に使う変換サービス
     */
    public AliasToBeanTupleTransformer(Class<T> targetType, ConversionService conversionService) {
        Assert.notNull(targetType, "targetType must not be null");
        Assert.notNull(conversionService, "conversionService must not be null");
        this.targetType = targetType;
        this.conversionService = conversionService;
        // DTOのsetter情報を起動時に前計算し、実行時はMap参照のみで解決する。
        this.precomputedPropertyLookup = buildWritableProperties(targetType);
    }

    /**
     * SQL結果1行（tuple）を DTO に変換します。
     *
     * @param tuple 1行分の値配列
     * @param aliases 列別名配列
     * @return 変換後DTO
     * @throws IllegalStateException tuple/aliasの不整合、または未知aliasを検知した場合
     */
    @Override
    public T transformTuple(Object[] tuple, String[] aliases) {
        validateTupleAndAliases(tuple, aliases);

        T target = BeanUtils.instantiateClass(targetType);
        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(target);
        beanWrapper.setConversionService(conversionService);

        int size = tuple.length;
        for (int i = 0; i < size; i++) {
            // alias -> property はキャッシュ付き解決。未知aliasは即例外。
            String property = resolvePropertyNameOrThrow(aliases[i], i);
            Object value = tuple[i];
            Class<?> propertyType = beanWrapper.getPropertyType(property);
            if (value != null && propertyType != null && conversionService.canConvert(value.getClass(), propertyType)) {
                // 数値型などの差異は Spring ConversionService で吸収する。
                value = conversionService.convert(value, propertyType);
            }
            beanWrapper.setPropertyValue(property, value);
        }
        return target;
    }

    /**
     * DTOの書き込み可能プロパティ名を事前計算します。
     * <p>
     * 実行時の解決コストを抑えるため、camelCase/snake_case の両方を
     * 小文字キーで登録します。
     * </p>
     */
    private Map<String, String> buildWritableProperties(Class<T> type) {
        Map<String, String> map = new HashMap<>();
        for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(type)) {
            if (descriptor.getWriteMethod() == null) {
                continue;
            }
            String propertyName = descriptor.getName();
            map.putIfAbsent(cacheKey(propertyName), propertyName);
            map.putIfAbsent(cacheKey(toSnakeCase(propertyName)), propertyName);
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * alias から DTOプロパティ名を解決します。
     *
     * @param alias SQL側の列別名
     * @param tupleIndex tuple内の列位置（エラー情報出力用）
     * @return DTOプロパティ名
     * @throws IllegalStateException aliasが未解決の場合
     */
    private String resolvePropertyNameOrThrow(String alias, int tupleIndex) {
        String resolved = resolvePropertyName(alias);
        if (resolved != null) {
            return resolved;
        }
        throw new IllegalStateException("Unknown SQL alias for transformer. targetType=" + targetType.getName()
                + ", aliasIndex=" + tupleIndex + ", alias=" + alias
                + ", knownKeys=" + precomputedPropertyLookup.keySet());
    }

    private String resolvePropertyName(String alias) {
        if (!StringUtils.hasText(alias)) {
            return null;
        }

        String rawHit = rawAliasCache.get(alias);
        if (rawHit != null) {
            return UNRESOLVED_PROPERTY.equals(rawHit) ? null : rawHit;
        }

        String normalized = normalizeAlias(alias);
        String resolved = resolvePropertyNameInternal(normalized);
        rawAliasCache.putIfAbsent(alias, resolved);
        return UNRESOLVED_PROPERTY.equals(resolved) ? null : resolved;
    }

    private String resolvePropertyNameInternal(String normalizedAlias) {
        String normalizedKey = cacheKey(normalizedAlias);
        String mapped = precomputedPropertyLookup.get(normalizedKey);
        return mapped != null ? mapped : UNRESOLVED_PROPERTY;
    }

    /** 大文字小文字差を吸収するため、キャッシュキーは小文字へ正規化します。 */
    private String cacheKey(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /**
     * SQL別名の引用符を除去して比較可能な形へ正規化します。
     * <p>
     * 例: {@code "ORDER_COUNT"} / {@code `order_count`} -> {@code ORDER_COUNT} / {@code order_count}
     * </p>
     */
    private String normalizeAlias(String alias) {
        String candidate = alias.trim();
        if (candidate.startsWith("\"") && candidate.endsWith("\"") && candidate.length() > 1) {
            return candidate.substring(1, candidate.length() - 1);
        }
        if (candidate.startsWith("`") && candidate.endsWith("`") && candidate.length() > 1) {
            return candidate.substring(1, candidate.length() - 1);
        }
        return candidate;
    }

    /** camelCase を snake_case へ変換します。 */
    private String toSnakeCase(String propertyName) {
        StringBuilder builder = new StringBuilder(propertyName.length() + 8);
        for (int i = 0; i < propertyName.length(); i++) {
            char ch = propertyName.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (builder.length() > 0) {
                    builder.append('_');
                }
                builder.append(Character.toLowerCase(ch));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    /**
     * tuple/aliases の前提条件を検証します。
     * <p>
     * 実行時の曖昧な挙動を避けるため、null/空/長さ不一致は全て例外とします。
     * </p>
     */
    private void validateTupleAndAliases(Object[] tuple, String[] aliases) {
        if (tuple == null || aliases == null) {
            throw new IllegalStateException("Tuple or aliases is null. tuple=" + (tuple == null)
                    + ", aliases=" + (aliases == null) + ", targetType=" + targetType.getName());
        }
        if (tuple.length == 0 || aliases.length == 0) {
            throw new IllegalStateException("Tuple and aliases are empty. targetType=" + targetType.getName());
        }
        if (tuple.length != aliases.length) {
            throw new IllegalStateException("Tuple and aliases size mismatch. tuple.length=" + tuple.length
                    + ", aliases.length=" + aliases.length + ", targetType=" + targetType.getName());
        }
    }
}
