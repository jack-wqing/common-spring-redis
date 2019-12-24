package com.zspace.spring.cache.service.support;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zspace.spring.cache.service.ObjectRedisSerializer;

/**
 * 
 * 重写缓存  -- 进行雪崩情况处理
 * @author liuwenqing02
 *
 */
public class CustomerCaffeineCache extends AbstractValueAdaptingCache {

	private final String name;
	
	private final com.github.benmanes.caffeine.cache.Cache<Object, Object> cache;
	
	private final Cache<String, Boolean> lockcache = Caffeine.newBuilder().expireAfterWrite(500, TimeUnit.MILLISECONDS)
	        .initialCapacity(100).maximumSize(1000).build();

	public CustomerCaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
		this(name, cache, true);
	}

	public CustomerCaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache,
			boolean allowNullValues) {

		super(allowNullValues);
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(cache, "Cache must not be null");
		this.name = name;
		this.cache = cache;
	}

	@Override
	public final String getName() {
		return this.name;
	}
	
	@Override
	public final com.github.benmanes.caffeine.cache.Cache<Object, Object> getNativeCache() {
		return this.cache;
	}

	@Override
	@Nullable
	public ValueWrapper get(Object key) {
		return super.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T get(Object key, final Callable<T> valueLoader) {
		return (T) fromStoreValue(this.cache.get(key, new LoadFunction(valueLoader)));
	}

	@Override
	@Nullable
	protected Object lookup(Object key) {
        String cacheKey = createCacheKey(key);
        Object value = null;
        int maxWaitTime = 10 * 60;     //最大等待次数,每次停留100毫秒，即最大等待1分钟，超过即退出走数据库查询
        int curTime = 0;               //当前等待次数
        for(;;){
            curTime += 1;
            value = this.cache.getIfPresent(key);
            if(value != null || curTime > maxWaitTime){
                break;                 //1.如果获取到结果直接返回;2.超过最大次数则返回走业务代码进行查询
            }
                                       //3.如果当前线程拿到了锁，则返回走业务代码进行查询
            if(snowslideLock(cacheKey)){
                break;
            }
            try {
               Thread.sleep(100);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
        }
		return value;
	}

	@Override
	public void put(Object key, @Nullable Object value) {
		this.cache.put(key, toStoreValue(value));
	}

	@Override
	@Nullable
	public ValueWrapper putIfAbsent(Object key, @Nullable final Object value) {
		PutIfAbsentFunction callable = new PutIfAbsentFunction(value);
		Object result = this.cache.get(key, callable);
		return (callable.called ? null : toValueWrapper(result));
	}

	@Override
	public void evict(Object key) {
		this.cache.invalidate(key);
	}

	@Override
	public void clear() {
		this.cache.invalidateAll();
	}

	private class PutIfAbsentFunction implements Function<Object, Object> {

		@Nullable
		private final Object value;

		private boolean called;

		public PutIfAbsentFunction(@Nullable Object value) {
			this.value = value;
		}

		@Override
		public Object apply(Object key) {
			this.called = true;
			return toStoreValue(this.value);
		}
	}


	private class LoadFunction implements Function<Object, Object> {

		private final Callable<?> valueLoader;

		public LoadFunction(Callable<?> valueLoader) {
			this.valueLoader = valueLoader;
		}

		@Override
		public Object apply(Object o) {
			try {
				return toStoreValue(valueLoader.call());
			}
			catch (Exception ex) {
				throw new ValueRetrievalException(o, valueLoader, ex);
			}
		}
	}

	private boolean snowslideLock(String cacheKey) {
	    boolean flag = false;
	    Object value = lockcache.getIfPresent(cacheKey);
	    if(value == null) {
	        synchronized (lockcache) {
	            value = lockcache.getIfPresent(cacheKey);
	            if(value == null) {
	                lockcache.put(cacheKey, true);
	                flag = true;
	            }
	        }
	    }
	    return flag;
	}
	
	private String createCacheKey(Object key) {
	    String cacheKey = key.toString();
	    try {
	        cacheKey = ObjectRedisSerializer.getObjectMapper().writeValueAsString(key);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
	    return cacheKey;
	}
	
}
