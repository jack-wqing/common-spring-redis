package com.zspace.spring.cache.service.support;

import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

/**
 * 自定义资源缓存 
 * 当相同key的数据已经进行处理的时候，新的相同请求的key只需要等待完成，之后获取就可以，不需要再次进行请求，减少并发量对后台业务逻辑的影响
 * 因为是多服务器，采用redis记录key的处理，是否已经进行(使用setnx实现)
 * 需要对自带的缓存实现的get方法进行重写
 * 在rediscache的基础上添加逻辑
 * @author liuwenqing02
 *
 */
public class CustomerRedisCache extends RedisCache{
  
    public CustomerRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig) {
        super(name, cacheWriter, cacheConfig);
    }
    
    @Override
    protected Object lookup(Object key) {
        String cacheKey = createCacheKey(key);
        byte[] value = null;
        RedisCacheWriter cacheWriter = getNativeCache();
        if(cacheWriter instanceof CustomerRedisCacheWriter) {
            CustomerRedisCacheWriter customerCacheWriter = (CustomerRedisCacheWriter) cacheWriter;
            int maxWaitTime = 10 * 60;     //最大等待次数,每次停留100毫秒，即最大等待1分钟，超过即退出走数据库查询
            int curTime = 0;               //当前等待次数
            for(;;){
                curTime += 1;
                value = cacheWriter.get(this.getName(), convertCacheKey(cacheKey));
                if(value != null || curTime > maxWaitTime){
                    break;                 //1.如果获取到结果直接返回;2.超过最大次数则返回走业务代码进行查询
                }
                                           //3.如果当前线程拿到了锁，则返回走业务代码进行查询
                if(customerCacheWriter.snowslideLock(cacheKey)){
                    break;
                }
                try {
                   Thread.sleep(100);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
            }
        }else {
            value = cacheWriter.get(this.getName(), convertCacheKey(cacheKey));
        }
        if (value == null) {
            return null;
        }
        return deserializeCacheValue(value);
        
    }
    private byte[] convertCacheKey(String cacheKey) {
        return serializeCacheKey(cacheKey);
    }

}
