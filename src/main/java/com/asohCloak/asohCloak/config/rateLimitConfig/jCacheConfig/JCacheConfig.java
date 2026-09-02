package com.asohCloak.asohCloak.config.rateLimitConfig.jCacheConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

@Configuration
public class JCacheConfig {

    @Bean
    public javax.cache.CacheManager jCacheManager() {
        CachingProvider cachingProvider = Caching.getCachingProvider(
                "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider");
        javax.cache.CacheManager cacheManager = cachingProvider.getCacheManager();

        if (cacheManager.getCache("rate-limit-buckets") == null) {
            cacheManager.createCache("rate-limit-buckets", new MutableConfiguration<>());
        }
        return cacheManager;
    }
}