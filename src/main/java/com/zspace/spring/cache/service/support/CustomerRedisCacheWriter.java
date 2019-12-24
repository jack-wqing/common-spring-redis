package com.zspace.spring.cache.service.support;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class CustomerRedisCacheWriter implements RedisCacheWriter{

    private final RedisConnectionFactory connectionFactory;
    private final Duration sleepTime;

    CustomerRedisCacheWriter(RedisConnectionFactory connectionFactory) {
        this(connectionFactory, Duration.ZERO);
    }
    CustomerRedisCacheWriter(RedisConnectionFactory connectionFactory, Duration sleepTime) {
        Assert.notNull(connectionFactory, "ConnectionFactory must not be null!");
        Assert.notNull(sleepTime, "SleepTime must not be null!");
        this.connectionFactory = connectionFactory;
        this.sleepTime = sleepTime;
    }

    @Override
    public void put(String name, byte[] key, byte[] value, @Nullable Duration ttl) {
        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(value, "Value must not be null!");
        execute(name, connection -> {
            if (shouldExpireWithin(ttl)) {
                connection.set(key, value, Expiration.from(ttl.toMillis(), TimeUnit.MILLISECONDS), SetOption.upsert());
            } else {
                connection.set(key, value);
            }
            return "OK";
        });
    }

    @Override
    public byte[] get(String name, byte[] key) {

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(key, "Key must not be null!");

        return execute(name, connection -> connection.get(key));
    }

    @Override
    public byte[] putIfAbsent(String name, byte[] key, byte[] value, @Nullable Duration ttl) {
        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(value, "Value must not be null!");
        return execute(name, connection -> {
            if (isLockingCacheWriter()) {
                doLock(name, connection);
            }
            try {
                if (connection.setNX(key, value)) {
                    if (shouldExpireWithin(ttl)) {
                        connection.pExpire(key, ttl.toMillis());
                    }
                    return null;
                }
                return connection.get(key);
            } finally {

                if (isLockingCacheWriter()) {
                    doUnlock(name, connection);
                }
            }
        });
    }

    @Override
    public void remove(String name, byte[] key) {

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(key, "Key must not be null!");
        execute(name, connection -> connection.del(key));
    }

    @Override
    public void clean(String name, byte[] pattern) {
        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(pattern, "Pattern must not be null!");
        execute(name, connection -> {
            boolean wasLocked = false;
            try {
                if (isLockingCacheWriter()) {
                    doLock(name, connection);
                    wasLocked = true;
                }
                byte[][] keys = Optional.ofNullable(connection.keys(pattern)).orElse(Collections.emptySet())
                        .toArray(new byte[0][]);
                if (keys.length > 0) {
                    connection.del(keys);
                }
            } finally {

                if (wasLocked && isLockingCacheWriter()) {
                    doUnlock(name, connection);
                }
            }
            return "OK";
        });
    }

    //雪崩处理  -- 加相同key访问的锁
    public Boolean snowslideLock(String name) {
        return snowslideExecute(connection -> doSnowSlickLock(name, connection));
    }
    public void snowslideUnLock(String name) {
        unlock(name);
    }
    void lock(String name) {
        execute(name, connection -> doLock(name, connection));
    }
    void unlock(String name) {
        executeLockFree(connection -> doUnlock(name, connection));
    }
    private Boolean doLock(String name, RedisConnection connection) {
        return connection.setNX(createCacheLockKey(name), new byte[0]);
    }
    
    private Boolean doSnowSlickLock(String name, RedisConnection connection) {
        byte[] lockKey = createCacheLockKey(name);
        Boolean lockFlag = connection.setNX(lockKey, new byte[0]);
        if(lockFlag != null && lockFlag) {
            connection.pExpire(lockKey, 500);
        }
        return lockFlag;
    }
    
    private Long doUnlock(String name, RedisConnection connection) {
        return connection.del(createCacheLockKey(name));
    }
    boolean doCheckLock(String name, RedisConnection connection) {
        return connection.exists(createCacheLockKey(name));
    }
    private boolean isLockingCacheWriter() {
        return !sleepTime.isZero() && !sleepTime.isNegative();
    }

    private <T> T execute(String name, Function<RedisConnection, T> callback) {
        RedisConnection connection = connectionFactory.getConnection();
        try {
            checkAndPotentiallyWaitUntilUnlocked(name, connection);
            return callback.apply(connection);
        } finally {
            connection.close();
        }
    }
    
    private <T> T snowslideExecute(Function<RedisConnection, T> callback) {
        RedisConnection connection = connectionFactory.getConnection();
        try {
            return callback.apply(connection);
        } finally {
            connection.close();
        }
    }

    private void executeLockFree(Consumer<RedisConnection> callback) {
        RedisConnection connection = connectionFactory.getConnection();
        try {
            callback.accept(connection);
        } finally {
            connection.close();
        }
    }

    private void checkAndPotentiallyWaitUntilUnlocked(String name, RedisConnection connection) {

        if (!isLockingCacheWriter()) {
            return;
        }
        try {

            while (doCheckLock(name, connection)) {
                Thread.sleep(sleepTime.toMillis());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PessimisticLockingFailureException(String.format("Interrupted while waiting to unlock cache %s", name),
                    ex);
        }
    }

    private static boolean shouldExpireWithin(@Nullable Duration ttl) {
        return ttl != null && !ttl.isZero() && !ttl.isNegative();
    }

    private static byte[] createCacheLockKey(String name) {
        return (name + "~lock").getBytes(StandardCharsets.UTF_8);
    }

    
}
