package com.zspace.spring.redis.config;

import com.zspace.spring.configure.property.ConfigProperties;

/**
 * redis连接参数配置
 * @author liuwenqing02
 *
 */
@ConfigProperties(prefix = "spring.redis")
public class RedisConfigProperties {

    private String host;

    private Integer port;

    private String password;

    private Integer database = 0;

    private Integer timeout = 20000;

    private Integer maxTotal = 8;

    private Integer maxIdle = 8;

    private Integer maxWaitMillis = 10000;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getDatabase() {
        return database;
    }

    public void setDatabase(Integer database) {
        this.database = database;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(Integer maxTotal) {
        this.maxTotal = maxTotal;
    }

    public Integer getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(Integer maxIdle) {
        this.maxIdle = maxIdle;
    }

    public Integer getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public void setMaxWaitMillis(Integer maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

}
