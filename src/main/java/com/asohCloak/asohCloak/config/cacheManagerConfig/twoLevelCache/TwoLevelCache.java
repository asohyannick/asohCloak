package com.asohCloak.asohCloak.config.cacheManagerConfig.twoLevelCache;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class TwoLevelCache implements org.springframework.cache.Cache {

    private final String name;
    private final Cache<Object, Object> localCache;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration ttl;

    public TwoLevelCache(String name, Cache<Object, Object> localCache,
                         RedisTemplate<String, Object> redisTemplate, Duration ttl) {
        this.name = name;
        this.localCache = localCache;
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    @Override @NonNull
    public String getName() { return name; }

    @Override @NonNull
    public Object getNativeCache() { return localCache; }

    private String redisKey(Object key) { return name + "::" + key; }

    @Override
    public ValueWrapper get(@NonNull Object key) {
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            return new SimpleValueWrapper(value);
        }
        Object redisValue = redisTemplate.opsForValue().get(redisKey(key));
        if (redisValue != null) {
            localCache.put(key, redisValue);
            return new SimpleValueWrapper(redisValue);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(@NonNull Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) return (T) wrapper.get();
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper == null) return null;
        Object value = wrapper.get();
        return type == null ? (T) value : type.cast(value);
    }

    @Override
    public void put(@NonNull Object key, Object value) {
        if (value == null) return;
        localCache.put(key, value);
        redisTemplate.opsForValue().set(redisKey(key), value, ttl);
    }

    @Override
    public void evict(@NonNull Object key) {
        localCache.invalidate(key);
        redisTemplate.delete(redisKey(key));
    }

    @Override
    public void clear() {
        localCache.invalidateAll();

        ScanOptions options = ScanOptions.scanOptions()
                .match(name + "::*")
                .count(500)
                .build();

        List<String> batch = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= 500) {
                    redisTemplate.delete(batch);
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            redisTemplate.delete(batch);
        }
    }
}