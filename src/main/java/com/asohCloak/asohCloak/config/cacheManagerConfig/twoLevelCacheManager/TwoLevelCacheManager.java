package com.asohCloak.asohCloak.config.cacheManagerConfig.twoLevelCacheManager;

import com.asohCloak.asohCloak.config.cacheManagerConfig.twoLevelCache.TwoLevelCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TwoLevelCacheManager implements CacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Caffeine<Object, Object> caffeineBuilder;
    private final Duration ttl;
    private final ConcurrentHashMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    public TwoLevelCacheManager(RedisTemplate<String, Object> redisTemplate,
                                Caffeine<Object, Object> caffeineBuilder, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.caffeineBuilder = caffeineBuilder;
        this.ttl = ttl;
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, n ->
                new TwoLevelCache(n, caffeineBuilder.build(), redisTemplate, ttl));
    }

    @Override
    public Collection<String> getCacheNames() {
        return Set.copyOf(cacheMap.keySet());
    }
}