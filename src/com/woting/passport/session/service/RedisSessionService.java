package com.woting.passport.session.service;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.stereotype.Service;

import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.SessionService;
import com.woting.passport.session.key.UserDeviceKey;
import com.woting.redis.session.LoginServiceUtils;

@Service
public class RedisSessionService implements SessionService {
    @Resource
    JedisConnectionFactory redisConn;

    @Override
    public Map<String, Object> getLoginStatus(UserDeviceKey udk) {
        return LoginServiceUtils.getLoginStatus(redisConn, udk);
    }

    @Override
    public boolean expireSession(UserDeviceKey udk) {
        return LoginServiceUtils.expireSession(redisConn, udk);
    }

    @Override
    public UserDeviceKey getActivedUserUDK(String userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void registUser(MobileUDKey mUdk) {
        // TODO Auto-generated method stub
        
    }
}
