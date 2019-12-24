package com.zspace.spring.cache.service.support;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * 对cache的生成方式进行修改
 * 实现方法：可以自定义过期时间
 */
public class CustomerRedisCacheManager extends RedisCacheManager implements BeanClassLoaderAware{

    private static final Logger log = LoggerFactory.getLogger(CustomerRedisCacheManager.class);
    private static final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
    private static final Pattern pattern = Pattern.compile("[+\\-*/%]");
    private char separator = '#';
    
    private ClassLoader classLoader;

    public CustomerRedisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        super(new CustomerRedisCacheWriter(redisConnectionFactory), RedisCacheConfiguration.defaultCacheConfig());
    }
    
    @Override
    protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        RedisCacheConfiguration.registerDefaultConverters(conversionService);
        cacheConfig = RedisCacheConfiguration.defaultCacheConfig();
        cacheConfig = cacheConfig.serializeValuesWith(SerializationPair.fromSerializer(new JdkSerializationRedisSerializer(classLoader)));
        int index = name.indexOf(this.separator);
        Duration duration = Duration.ZERO;
        if (index > 0) {
            duration = Duration.ofSeconds(getExpiration(name, index, 1), 0);
        }
        cacheConfig = cacheConfig.entryTtl(duration);
        RedisCache redisCache = super.createRedisCache(name, cacheConfig);
        return new CustomerRedisCache(name, redisCache.getNativeCache(), cacheConfig);
    }

    /**
     * 计算缓存时间
     * @param name 缓存名字 cache#60*60
     * @param separatorIndex 分隔符位置
     * @param defalutExp 默认缓存时间
     * @return
     */
    protected long getExpiration(final String name, final int separatorIndex, final long defalutExp) {
        Long expiration = null;
        String expirationAsString = name.substring(separatorIndex + 1 + 1);
        try {
            if (pattern.matcher(expirationAsString).find()) {
                expiration = NumberUtils.toLong(scriptEngine.eval(expirationAsString).toString(), defalutExp);
            } else {
                expiration = NumberUtils.toLong(expirationAsString, defalutExp);
            }
        } catch (ScriptException e) {
            log.error("extire time convert error:{},exception message:{}", name, e.getMessage());
        }
        return Objects.nonNull(expiration) ? expiration.longValue() : defalutExp;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

}