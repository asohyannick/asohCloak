package com.asohCloak.asohCloak.config.cacheManagerConfig.twoLevelCache;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.Duration;
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
    public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
        ValueWrapper wrapper = get(key);
        return wrapper == null ? null : type.cast(wrapper.get());
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
        redisTemplate.keys(name + "::*").forEach(redisTemplate::delete);
    }
}