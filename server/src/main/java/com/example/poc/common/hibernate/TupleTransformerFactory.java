package com.example.poc.common.hibernate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.query.TupleTransformer;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * {@link TupleTransformer} の生成を集約するFactoryです。
 * <p>
 * 変換先DTO型ごとにインスタンスをキャッシュし、DAO側の都度生成コストを抑えます。
 * </p>
 */
@Component
public class TupleTransformerFactory {

    private final ConversionService conversionService;
    private final Map<Class<?>, TupleTransformer<?>> aliasToBeanCache = new ConcurrentHashMap<>();

    /**
     * shared conversion service を使って初期化します。
     */
    public TupleTransformerFactory() {
        this(DefaultConversionService.getSharedInstance());
    }

    /**
     * 指定の変換サービスで初期化します。
     *
     * @param conversionService 型変換サービス
     */
    public TupleTransformerFactory(ConversionService conversionService) {
        Assert.notNull(conversionService, "conversionService must not be null");
        this.conversionService = conversionService;
    }

    /**
     * Alias to Bean 変換用Transformerを返します（DTO型単位でキャッシュ）。
     *
     * @param targetType 変換先DTO型
     * @param <T> DTO型
     * @return 共有Transformer
     */
    @SuppressWarnings("unchecked")
    public <T> TupleTransformer<T> aliasToBean(Class<T> targetType) {
        Assert.notNull(targetType, "targetType must not be null");
        // DTO型ごとに1インスタンスだけ生成し、以降はキャッシュ済みを再利用する。
        return (TupleTransformer<T>) aliasToBeanCache.computeIfAbsent(
                targetType,
                type -> new AliasToBeanTupleTransformer<>((Class<Object>) type, conversionService));
    }
}
