package com.woting.passport.login.service;

import javax.annotation.Resource;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.stereotype.Service;

@Service
public class LoginRedisService {
    @Resource(name="connectionFactory")
    private JedisConnectionFactory jedisConnectionFactory;

    public void login() {
        RedisConnection rc=null;
        try {
            rc=jedisConnectionFactory.getConnection();
            
        } finally {
            if (rc!=null) {rc.close(); rc=null;}
        }
    }
}