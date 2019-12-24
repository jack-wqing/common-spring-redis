package com.zspace.spring.cache.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.zspace.spring.cache.service.CacheKeyGenerator;
import com.zspace.spring.cache.service.DefaultCachingConfigurer;
import com.zspace.spring.cache.service.support.CustomerCaffeineCacheManager;
import com.zspace.spring.cache.service.support.CustomerRedisCacheManager;
import com.zspace.spring.configure.condition.ConditionalOnBean;

@Configuration
@EnableCaching
class CacheConfiguration {
    
	@Bean("redisCache")
	@ConditionalOnBean(RedisConnectionFactory.class)
	public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
		return new CustomerRedisCacheManager(redisConnectionFactory);
	}
	@Bean("localCache")
    public CustomerCaffeineCacheManager caffeineCacheManager() {
        return new CustomerCaffeineCacheManager();
    }
	@Bean
	public CacheKeyGenerator keyGenerator() {
	    return new CacheKeyGenerator();
	}
	@Bean
	public CachingConfigurer cachingConfigurer(CustomerCaffeineCacheManager cacheManager, CacheKeyGenerator keyGenerator) {
	    return new DefaultCachingConfigurer(cacheManager, keyGenerator);
	}
	
}
