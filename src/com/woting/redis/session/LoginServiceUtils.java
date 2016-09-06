package com.woting.redis.session;

import java.util.Map;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import com.woting.passport.session.key.UserDeviceKey;

/**
 * 处理登录（包括登录状态，登录逻辑等）的方法类。
 * 为与RedisPool和SprintICO配合，这些方法不采用类思路。面向方法去做
 * @author wanghui
 *
 */
public abstract class LoginServiceUtils {
    /**
     * 获得登录状态
     * @param redisConn redis的连接
     * @param udk 用户设备key
     * @return
     */
     public static Map<String, Object> getLoginStatus(JedisConnectionFactory redisConn, UserDeviceKey udk) {
         return null;
     }

    public static boolean expireSession(JedisConnectionFactory redisConn, UserDeviceKey udk) {
        return false;
    }
}
