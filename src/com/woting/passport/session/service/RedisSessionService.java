package com.woting.passport.session.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.stereotype.Service;

import com.spiritdata.framework.util.StringUtils;
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.woting.passport.UGA.service.UserService;
import com.woting.passport.login.persistence.pojo.MobileUsedPo;
import com.woting.passport.login.service.MobileUsedService;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.SessionService;
import com.woting.passport.session.key.UserDeviceKey;
import com.woting.passport.session.redis.RedisUserDeviceKey;

@Service
public class RedisSessionService implements SessionService {
    @Resource
    JedisConnectionFactory redisConn;
    @Resource
    private UserService userService;
    @Resource
    private MobileUsedService muService;

    @Override
    /**
     * return 
     */
    /**
     * 处理用户设备Key进入系统，若未登录要看之前的登录情况，自动登录
     * @param udk 根据用户设备Key
     * @return Map类型，key 和 value意义如下：
     *   2001 未获得IMEI无法处理
     *   2002 设备无法自动登录
     *   2003 请先登录
     *   1002 不能找到相应的用户
     *   1001 用户已登录
     *   1001 设备自动登录成功
     *   1001 设备自动登录成功，并返回用户的UserId
     */
    public Map<String, Object> dealUDkeyEntry(UserDeviceKey udk, String operDesc) {
        Map<String,Object> map=new HashMap<String, Object>();
        if (udk==null||StringUtils.isNullOrEmptyOrSpace(udk.getDeviceId())) {
            map.put("ReturnType", "2001");
            map.put("Msg", "未获得IMEI无法处理");
            return map;
        }

        RedisUserDeviceKey rUdk=new RedisUserDeviceKey(udk);
        RedisConnection conn=null;
        try {
            conn=redisConn.getConnection();
            //检查是否有这个登录
            byte[] _value=conn.get(rUdk.getKey_UserLoginStatus().getBytes());
            if (_value!=null) {//已经登录
                conn.set(rUdk.getKey_UserLoginStatus().getBytes(), (System.currentTimeMillis()+"::"+operDesc).getBytes());
                conn.expire(rUdk.getKey_UserLoginStatus().getBytes(), 30*60);//30分钟后过期
                map.put("ReturnType", "1001");
                map.put("Msg", "用户已登录");
            } else {//未登录
                //查找用户
                MobileUDKey mudk=new MobileUDKey(udk);
                if (mudk.isUser()) {
                    UserPo up=userService.getUserById(mudk.getUserId());
                    if (up==null) {
                        map.put("ReturnType", "1002");
                        map.put("Msg", "不能找到相应的用户");
                    } else {
                        MobileUsedPo mup=muService.getUsedInfo(udk.getDeviceId(), udk.getPCDType());
                        if (mup.getStatus()==1&&mup.getUserId().equals(udk.getUserId())) {//自动登录
                            conn.set(rUdk.getKey_UserLoginStatus().getBytes(), (System.currentTimeMillis()+"::register").getBytes());
                            conn.expire(rUdk.getKey_UserLoginStatus().getBytes(), 30*60);//30分钟后过期
                            conn.set(rUdk.getKey_UserLoginDeviceType().getBytes(), rUdk.getValue_DeviceId().getBytes());
                            conn.expire(rUdk.getKey_UserLoginDeviceType().getBytes(), 30*60);//30分钟后过期
                            map.put("ReturnType", "1001");
                            map.put("Msg", "设备自动登录成功");
                        } else {
                            map.put("ReturnType", "2003");
                            map.put("Msg", "请先登录");
                        }
                    }
                } else {//不是User，也自动登录
                    if (operDesc.equals("common/entryApp")) {//是进入也自动登录
                        MobileUsedPo mup=muService.getUsedInfo(udk.getDeviceId(), udk.getPCDType());
                        if (mup.getStatus()==1) {//自动登录
                            rUdk.setUserId(mup.getUserId());
                            conn.set(rUdk.getKey_UserLoginStatus().getBytes(), (System.currentTimeMillis()+"::register").getBytes());
                            conn.expire(rUdk.getKey_UserLoginStatus().getBytes(), 30*60);//30分钟后过期
                            conn.set(rUdk.getKey_UserLoginDeviceType().getBytes(), rUdk.getValue_DeviceId().getBytes());
                            conn.expire(rUdk.getKey_UserLoginDeviceType().getBytes(), 30*60);//30分钟后过期
                            map.put("ReturnType", "1001");
                            map.put("Msg", "设备自动登录成功");
                            udk.setUserId(mup.getUserId());
                            map.put("UserId", mup.getUserId());
                        } else {
                            map.put("ReturnType", "2002");
                            map.put("Msg", "设备无法自动登录");
                        }
                    }
                }
            }
        } finally {
            redisConn.getShardInfo().createResource();
            if (conn!=null) conn.close();
            conn=null;
        }
        return map;
    }


    @Override
    public void registUser(UserDeviceKey udk) {
        RedisUserDeviceKey rUdk=new RedisUserDeviceKey(udk);

        RedisConnection conn=null;
        try {
            conn=redisConn.getConnection();
            conn.set(rUdk.getKey_UserLoginStatus().getBytes(), (System.currentTimeMillis()+"::register").getBytes());
            conn.expire(rUdk.getKey_UserLoginStatus().getBytes(), 30*60);//30分钟后过期
            conn.set(rUdk.getKey_UserLoginDeviceType().getBytes(), rUdk.getValue_DeviceId().getBytes());
            conn.expire(rUdk.getKey_UserLoginDeviceType().getBytes(), 30*60);//30分钟后过期
        } finally {
            if (conn!=null) conn.close();
            conn=null;
        }
    }

    @Override
    public List<? extends UserDeviceKey> getActivedUserUDKs(String userId) {
        List<UserDeviceKey> retl=new ArrayList<UserDeviceKey>();

        RedisConnection conn=null;
        try {
            conn=redisConn.getConnection();
            MobileUDKey mUdk=new MobileUDKey();
            mUdk.setUserId(userId);
            mUdk.setPCDType(1);
            RedisUserDeviceKey rUdk=new RedisUserDeviceKey(mUdk);
            byte[] _deviceId=conn.get(rUdk.getKey_UserLoginDeviceType().getBytes());
            if (_deviceId!=null) {
                mUdk.setDeviceId(new String(_deviceId));
                retl.add(mUdk);
            }
            mUdk=new MobileUDKey();
            mUdk.setUserId(userId);
            mUdk.setPCDType(2);
            rUdk=new RedisUserDeviceKey(mUdk);
            _deviceId=conn.get(rUdk.getKey_UserLoginDeviceType().getBytes());
            if (_deviceId!=null) {
                mUdk.setDeviceId(new String(_deviceId));
                retl.add(mUdk);
            }
        } finally {
            if (conn!=null) conn.close();
            conn=null;
        }
        return retl.isEmpty()?null:retl;
    }

    @Override
    public UserDeviceKey getActivedUserUDK(String userId, int pcdType) {
        MobileUDKey mUdk=new MobileUDKey();
        mUdk.setUserId(userId);
        mUdk.setPCDType(pcdType);
        RedisUserDeviceKey rUdk=new RedisUserDeviceKey(mUdk);

        RedisConnection conn=null;
        try {
            conn=redisConn.getConnection();
            byte[] _deviceId=conn.get(rUdk.getKey_UserLoginDeviceType().getBytes());
            if (_deviceId==null) return null;
            mUdk.setDeviceId(new String(_deviceId));
            return mUdk;
        } finally {
            if (conn!=null) conn.close();
            conn=null;
        }
    }

    @Override
    public void logoutSession(UserDeviceKey udk) {
        RedisUserDeviceKey rUdk=new RedisUserDeviceKey(udk);

        RedisConnection conn=null;
        try {
            conn=redisConn.getConnection();
            conn.del(rUdk.getKey_UserLoginStatus().getBytes());
            conn.del(rUdk.getKey_UserLoginDeviceType().getBytes());
        } finally {
            if (conn!=null) conn.close();
            conn=null;
        }
    }
}
