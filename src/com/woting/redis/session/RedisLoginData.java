package com.woting.redis.session;

/**
 * 用于Redis数据操作中，生成Key或相应的值。本接口仅为实现RedisSession框架中的数据获取。
 * RedisSession可被多平台实现
 * @author wanghui
 */
public interface RedisLoginData {
    public String getLockKey();
}