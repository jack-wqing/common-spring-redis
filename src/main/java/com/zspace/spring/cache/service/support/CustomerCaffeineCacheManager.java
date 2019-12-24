package com.zspace.spring.cache.service.support;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

import com.github.benmanes.caffeine.cache.Caffeine;

public class CustomerCaffeineCacheManager implements CacheManager {
    
    private static final String separator = "#";
	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);
	private boolean dynamic = true;
	private boolean allowNullValues = true;
	public CustomerCaffeineCacheManager() {
	    
	}
	public boolean isAllowNullValues() {
		return this.allowNullValues;
	}
	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(this.cacheMap.keySet());
	}
	@Override
	@Nullable
	public Cache getCache(String name) {
		Cache cache = this.cacheMap.get(name);
		if (cache == null && this.dynamic) {
			synchronized (this.cacheMap) {
				cache = this.cacheMap.get(name);
				if (cache == null) {
					cache = createCaffeineCache(name);
					this.cacheMap.put(name, cache);
				}
			}
		}
		return cache;
	}
	protected Cache createCaffeineCache(String name) {
		return new CustomerCaffeineCache(name, createNativeCaffeineCache(name), isAllowNullValues());
	}
	/**
	 * 此处通过缓存名称：动态的从注解标记中拿到：expireTime(seconds),initialCapacity,maximumSize
	 * @param name :cachename#e1#i200#m2000
	 * @return
	 */
	protected com.github.benmanes.caffeine.cache.Cache<Object, Object> createNativeCaffeineCache(String cacheName) {
	    long expire          = 1;
	    int initialCapacity = 200;
	    long maximumSize     = 1000;
	    if(cacheName.indexOf(separator) != -1) {
	        String[] values = cacheName.split(separator);
	        
	        for (String value : values) {
                char prefix = value.charAt(0);
                String paraValue = value.substring(1);
                switch (prefix) {
                    case 'e':
                        expire = handleLong(paraValue, 1);
                        break;
                    case 'i':
                        initialCapacity = handleInt(paraValue, 200);
                        break;
                    case 'm':
                        maximumSize = handleLong(paraValue, 1000);
                        break;
                    default:
                        break;
                }
            }
	    }
	    com.github.benmanes.caffeine.cache.Cache<Object, Object> cache = Caffeine.newBuilder()
	            .expireAfterWrite(expire, TimeUnit.SECONDS).initialCapacity(initialCapacity).maximumSize(maximumSize).build();
	    return cache;
	}
	
	private int handleInt(String value, int defaultValue) {
	    return NumberUtils.toInt(value, defaultValue);
	}
	
	private int handleLong(String value, int defaultValue) {
        return NumberUtils.toInt(value, defaultValue);
    }
}
