package com.zspace.spring.cache.service;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * 提供默认的缓存管理器支持
 * 
 * @author liuwenqing02
 *
 */
public class DefaultCachingConfigurer extends CachingConfigurerSupport{

    private final CacheManager cacheManager;
    private final KeyGenerator keyGenerator;
    
    
    public DefaultCachingConfigurer(CacheManager cacheManager, KeyGenerator keyGenerator) {
        this.cacheManager = cacheManager;
        this.keyGenerator = keyGenerator;
    }

    @Override
    public CacheManager cacheManager() {
        return this.cacheManager;
    }

    @Override
    public KeyGenerator keyGenerator() {
        return this.keyGenerator;
    }
    

}
