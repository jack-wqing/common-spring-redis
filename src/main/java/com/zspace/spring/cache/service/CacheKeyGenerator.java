package com.zspace.spring.cache.service;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.KeyGenerator;

import com.alibaba.fastjson.JSON;

public class CacheKeyGenerator implements KeyGenerator {
	@Override
	public Object generate(Object target, Method method, Object... params) {
		StringBuilder key = new StringBuilder();
		key.append(target.getClass().getName()).append(".").append(method.getName()).append(":");
		if (params.length == 0) {
			return key.append(0).toString();
		}
		key.append(JSON.toJSONString(params));
		return MD5Util.GetMD5Code(key.toString());
	}
}