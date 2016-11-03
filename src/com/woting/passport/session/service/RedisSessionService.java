package com.woting.passport.session.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.stereotype.Service;

import com.spiritdata.framework.UGA.UgaUser;
import com.spiritdata.framework.ext.spring.redis.RedisOperService;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.StringUtils;
import com.woting.passport.UGA.persis.pojo.UserPo;
import com.woting.passport.UGA.service.UserService;
import com.woting.passport.login.persis.pojo.MobileUsedPo;
import com.woting.passport.login.service.MobileUsedService;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.DeviceType;
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
     *
     *   0000 用户设备key为空，无法处理
     *
     *   1001 用户已登录
     *
     *   1002 不能找到相应的用户
     *   1003 得不到相应的PCDType
     *   2005 请求用户与已登录用户不相符合
     *
     *   2004 移动客户端——未获得IMEI无法处理
     *   3004 PC客户端——未获得SessionId无法处理
     *
     *   2003 请先登录
     */
    public Map<String, Object> dealUDkeyEntry(UserDeviceKey udk, String operDesc) {
        Map<String,Object> map=new HashMap<String, Object>();
        if (udk==null) {
            map.put("ReturnType", "0000");
            map.put("Msg", "用户设备key为空，无法处理");
            return map;
        }
        DeviceType dt=DeviceType.buildDtByPCDType(udk.getPCDType());
        if (dt==DeviceType.ERR) {
            map.put("ReturnType", "1003");
            map.put("Msg", "得不到相应的PCDType");
            return map;
        }
        if (StringUtils.isNullOrEmptyOrSpace(udk.getDeviceId())) {
            if (dt==DeviceType.PC) {
                map.put("ReturnType", "3004");
                map.put("Msg", "未获得SessionId无法处理");
            } else {
                map.put("ReturnType", "2004");
                map.put("Msg", "未获得IMEI无法处理");
            }
            return map;
        }

        RedisUserDeviceKey rUdk=new RedisUserDeviceKey(udk);
        RedisOperService roService=null;
        try {
            roService=new RedisOperService(redisConn, 4);
            //从Redis中获得对应额UserId
            String _value=roService.get(rUdk.getKey_DeviceType_UserId());
            String _userId=(_value==null?null:new String(_value));
            boolean hadLogon=_userId==null?false:(_userId.equals(rUdk.getUserId())&&roService.get(rUdk.getKey_UserLoginStatus())!=null);

            if (hadLogon) {//已经登录
                roService.set(rUdk.getKey_UserLoginStatus(), System.currentTimeMillis()+"::"+operDesc);
                roService.pExpire(rUdk.getKey_UserLoginStatus(), 30*60*1000);//30分钟后过期
                try {
                    if (!roService.pExpire(rUdk.getKey_UserLoginDeviceType(), 30*60*1000)) {//保持一致性
                        roService.set(rUdk.getKey_UserLoginDeviceType(), udk.getDeviceId(), 30*60*1000);
                    }
                    if (!roService.pExpire(rUdk.getKey_DeviceType_UserId(), 30*60*1000)) {//保持一致性
                        roService.set(rUdk.getKey_DeviceType_UserId(), _userId, 30*60*1000);
                    }
                    if (!roService.pExpire(rUdk.getKey_DeviceType_UserInfo(), 30*60*1000)) {//保持一致性
                        UserPo up=userService.getUserById(_userId);
                        if (up==null) {
                            //删除所有用户相关的Key值
                            delUserAll(_userId, roService);
                            map.put("ReturnType", "1002");
                            map.put("Msg", "不能找到相应的用户");
                            return map;
                        }
                        roService.set(rUdk.getKey_DeviceType_UserInfo(), JsonUtils.objToJson(up.toHashMap4Mobile()), 30*60*1000);
                    }
                    
                } catch(Exception e) {
                }
                map.put("ReturnType", "1001");
                map.put("UserId", rUdk.getUserId());
                try {
                    Map<String, Object> um=(Map<String, Object>)JsonUtils.jsonToObj(roService.get(rUdk.getKey_DeviceType_UserInfo()), Map.class);
                    map.put("UserInfo", um);
                } catch(Exception e) {
                }
                map.put("Msg", "用户已登录");
            } else {//处理未登录
                MobileUsedPo mup=muService.getUsedInfo(udk.getDeviceId(), udk.getPCDType());
                boolean noLog=false;
                noLog=mup==null||mup.getStatus()!=1||mup.getUserId()==null;
                if (noLog) {//无法登录
                    //删除在该设备上的登录信息
                    RedisUserDeviceKey _rUdk=new RedisUserDeviceKey(new UserDeviceKey());
                    _rUdk.setDeviceId(udk.getDeviceId());
                    _rUdk.setPCDType(dt.getPCDType());
                    String uid=roService.get(_rUdk.getKey_DeviceType_UserId());
                    if (!StringUtils.isNullOrEmptyOrSpace(uid)) { //进行删除工作
                        _rUdk.setUserId(uid);
                        cleanUserLogin(_rUdk, roService);
                    }
                    if (StringUtils.isNullOrEmptyOrSpace(_userId)) {
                        _rUdk.setUserId(_userId);
                        cleanUserLogin(_rUdk, roService);
                    }
                    map.put("ReturnType", "2003");
                    map.put("Msg", "请先登录");
                } else {//可以进行登录
                    if (udk.getUserId()!=null&&!udk.getUserId().equals(mup.getUserId())) {
                        map.put("ReturnType", "2005");
                        map.put("Msg", "请求用户与已登录用户不相符合");
                    } else {
                        //1-删除该用户在此类设备上的登录信息——踢出（不允许同一用户在同一类型的不同设备上同时登录）
                        try {
                            String did=roService.get(rUdk.getKey_UserLoginDeviceType());
                            if (did!=null&&did.trim().length()>0) {
                                RedisUserDeviceKey _oldKey=new RedisUserDeviceKey(udk);
                                _oldKey.setDeviceId(did);
                                cleanUserLogin(_oldKey, roService);
                            }
                        } catch(Exception e) {}
                        //2-删除之前的用户在该设备的登录信息（不允许同一设备上有两个用户同时登录）
                        try {
                            String uid=roService.get(rUdk.getKey_DeviceType_UserId());
                            if (uid!=null&&uid.trim().length()>0) {
                                RedisUserDeviceKey _oldKey=new RedisUserDeviceKey(udk);
                                _oldKey.setUserId(uid);
                                cleanUserLogin(_oldKey, roService);
                            }
                        } catch(Exception e) { }
                        UserPo upo=userService.getUserById(mup.getUserId());
                        if (upo==null) {
                            //删除所有用户相关的Key值
                            delUserAll(mup.getUserId(), roService);
                            map.put("ReturnType", "1002");
                            map.put("Msg", "不能找到相应的用户");
                            return map;
                        }
                        rUdk.setUserId(mup.getUserId());
                        udk.setUserId(mup.getUserId());
                        roService.set(rUdk.getKey_UserLoginStatus(), (System.currentTimeMillis()+"::register"));
                        roService.pExpire(rUdk.getKey_UserLoginStatus(), 30*60*1000);//30分钟后过期
                        roService.set(rUdk.getKey_UserLoginDeviceType(), rUdk.getDeviceId());
                        roService.pExpire(rUdk.getKey_UserLoginDeviceType(), 30*60*1000);//30分钟后过期
                        roService.set(rUdk.getKey_DeviceType_UserId(), upo.getUserId());
                        roService.pExpire(rUdk.getKey_DeviceType_UserId(), 30*60*1000);//30分钟后过期
                        roService.set(rUdk.getKey_DeviceType_UserInfo(), (JsonUtils.objToJson(upo.toHashMap4Mobile())));
                        roService.pExpire(rUdk.getKey_DeviceType_UserInfo(), 30*60*1000);//30分钟后过期

                        map.put("ReturnType", "1001");
                        map.put("UserId", upo.getUserId());
                        try {
                            map.put("UserInfo", upo.toHashMap4Mobile());
                        } catch(Exception e) {
                        }
                        map.put("Msg", "用户已登录");
                    }
                }
            }
        } finally {
            if (roService!=null) roService.close();
            roService=null;
        }
        return map;
    }

    @Override
    public <V extends UgaUser> void registUser(UserDeviceKey udk, V user) {
        RedisUserDeviceKey rUdk=new RedisUserDeviceKey(udk);

        RedisOperService roService=null;
        try {
            roService=new RedisOperService(redisConn, 4);
            //1-删除该用户在此类设备上的登录信息——踢出（不允许同一用户在同一类型的不同设备上同时登录）
            try {
                String did=roService.get(rUdk.getKey_UserLoginDeviceType());
                if (did!=null&&did.trim().length()>0) {
                    RedisUserDeviceKey _oldKey=new RedisUserDeviceKey(udk);
                    _oldKey.setDeviceId(did);
                    cleanUserLogin(_oldKey, roService);
                }
            } catch(Exception e) {}
            //2-删除在该设备上的其他用户登录信息（不允许同一设备上有两个用户同时登录）
            try {
                //删除用户在
                String uid=roService.get(rUdk.getKey_DeviceType_UserId());
                if (uid!=null&&uid.trim().length()>0) {
                    RedisUserDeviceKey _oldKey=new RedisUserDeviceKey(udk);
                    _oldKey.setUserId(uid);
                    cleanUserLogin(_oldKey, roService);
                }
            } catch(Exception e) {}
            roService.set(rUdk.getKey_UserLoginStatus(), (System.currentTimeMillis()+"::register"));
            roService.pExpire(rUdk.getKey_UserLoginStatus(), 30*60*1000);//30分钟后过期
            roService.set(rUdk.getKey_UserLoginDeviceType(), rUdk.getDeviceId());
            roService.pExpire(rUdk.getKey_UserLoginDeviceType(), 30*60*1000);//30分钟后过期
            UserPo upo=(UserPo)user;
            roService.set(rUdk.getKey_DeviceType_UserId(), upo.getUserId());
            roService.pExpire(rUdk.getKey_DeviceType_UserId(), 30*60*1000);//30分钟后过期
            roService.set(rUdk.getKey_DeviceType_UserInfo(), (JsonUtils.objToJson(upo.toHashMap4Mobile())));
            roService.pExpire(rUdk.getKey_DeviceType_UserInfo(), 30*60*1000);//30分钟后过期
        } finally {
            if (roService!=null) roService.close();
            roService=null;
        }
    }

    @Override
    public List<? extends UserDeviceKey> getActivedUserUDKs(String userId) {
        List<UserDeviceKey> retl=new ArrayList<UserDeviceKey>();

        RedisOperService roService=null;
        try {
            roService=new RedisOperService(redisConn, 4);
            MobileUDKey mUdk=new MobileUDKey();
            mUdk.setUserId(userId);
            mUdk.setPCDType(1);
            RedisUserDeviceKey rUdk=new RedisUserDeviceKey(mUdk);
            String _deviceId=roService.get(rUdk.getKey_UserLoginDeviceType());
            if (_deviceId!=null) {
                mUdk.setDeviceId(new String(_deviceId));
                retl.add(mUdk);
            }
            mUdk=new MobileUDKey();
            mUdk.setUserId(userId);
            mUdk.setPCDType(2);
            rUdk=new RedisUserDeviceKey(mUdk);
            _deviceId=roService.get(rUdk.getKey_UserLoginDeviceType());
            if (_deviceId!=null) {
                mUdk.setDeviceId(new String(_deviceId));
                retl.add(mUdk);
            }
        } finally {
            if (roService!=null) roService.close();
            roService=null;
        }
        return retl.isEmpty()?null:retl;
    }

    @Override
    public UserDeviceKey getActivedUserUDK(String userId, int pcdType) {
        MobileUDKey mUdk=new MobileUDKey();
        mUdk.setUserId(userId);
        mUdk.setPCDType(pcdType);
        RedisUserDeviceKey rUdk=new RedisUserDeviceKey(mUdk);

        RedisOperService roService=null;
        try {
            roService=new RedisOperService(redisConn, 4);
            String _deviceId=roService.get(rUdk.getKey_UserLoginDeviceType());
            if (_deviceId==null) return null;
            mUdk.setDeviceId(new String(_deviceId));
            return mUdk;
        } finally {
            if (roService!=null) roService.close();
            roService=null;
        }
    }

    @Override
    public void logoutSession(UserDeviceKey udk) {
        RedisUserDeviceKey rUdk=new RedisUserDeviceKey(udk);
        RedisOperService roService=null;
        try {
            roService=new RedisOperService(redisConn, 4);
            roService.del(rUdk.getKey_UserLoginStatus());
            roService.del(rUdk.getKey_UserLoginDeviceType());
            roService.del(rUdk.getKey_DeviceType_UserId());
            roService.del(rUdk.getKey_DeviceType_UserInfo());
        } finally {
            if (roService!=null) roService.close();
            roService=null;
        }
    }

    private void delUserAll(String userId, RedisOperService ros) {
        for (DeviceType dt: DeviceType.values()) {
            if (dt!=DeviceType.ERR) {
                RedisUserDeviceKey rUdk=new RedisUserDeviceKey(new UserDeviceKey());
                rUdk.setUserId(userId);
                rUdk.setPCDType(dt.getPCDType());
                String did=ros.get(rUdk.getKey_UserLoginDeviceType_OnlyUseUserId());
                if (!StringUtils.isNullOrEmptyOrSpace(did)&&did.length()>5) { //进行删除工作
                    rUdk.setDeviceId(did);
                    cleanUserLogin(rUdk, ros);
                }
            }
        }
    }
    private void cleanUserLogin(RedisUserDeviceKey rUdk, RedisOperService ros) {
        ros.del(rUdk.getKey_UserLoginStatus());
        ros.del(rUdk.getKey_UserLoginDeviceType());
        ros.del(rUdk.getKey_DeviceType_UserId());
        ros.del(rUdk.getKey_DeviceType_UserInfo());
    }
}