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
        RedisOperService roService=null;
        try {
            roService=new RedisOperService(redisConn, 4);
            String _value=roService.get(rUdk.getKey_DeviceType_UserId());
            String _userId=(_value==null?null:new String(_value));
            boolean hadLogon=_userId==null?false:(_userId.equals(rUdk.getUserId())&&roService.get(rUdk.getKey_UserLoginStatus())!=null);

            //已经登录
            if (hadLogon) {
                roService.set(rUdk.getKey_UserLoginStatus(), System.currentTimeMillis()+"::"+operDesc);
                roService.pExpire(rUdk.getKey_UserLoginStatus(), 30*60*1000);//30分钟后过期
                try {//这里可能有问题，先这样
                    roService.pExpire(rUdk.getKey_UserLoginDeviceType(), 30*60*1000);//30分钟后过期
                    roService.pExpire(rUdk.getKey_DeviceType_UserId(), 30*60*1000);//30分钟后过期
                    roService.pExpire(rUdk.getKey_DeviceType_UserInfo(), 30*60*1000);//30分钟后过期
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
                return map;
            }

            //处理未登录
            MobileUsedPo mup=muService.getUsedInfo(udk.getDeviceId(), udk.getPCDType());
            if (mup!=null&&mup.getStatus()==1) {//自动登录
                if (!mup.getUserId().equals(_userId)) {
                    map.put("ReturnType", "2003");
                    map.put("Msg", "请先登录");
                } else {
                    //删除之前的用户设备登录信息
                    try {
                        String bs=roService.get(rUdk.getKey_UserLoginDeviceType());
                        if (bs!=null&&bs.length()>0) {
                            String deviceId=new String(bs);
                            RedisUserDeviceKey _oldKey=new RedisUserDeviceKey(udk);
                            _oldKey.setDeviceId(deviceId);
                            roService.del(_oldKey.getKey_UserLoginStatus());
                            roService.del(_oldKey.getKey_UserLoginDeviceType());
                            roService.del(_oldKey.getKey_DeviceType_UserId());
                            roService.del(_oldKey.getKey_DeviceType_UserInfo());
                        }
                    } catch(Exception e) {
                    }
                    rUdk.setUserId(mup.getUserId());
                    udk.setUserId(mup.getUserId());
                    roService.set(rUdk.getKey_UserLoginStatus(), (System.currentTimeMillis()+"::register"));
                    roService.pExpire(rUdk.getKey_UserLoginStatus(), 30*60*1000);//30分钟后过期
                    roService.set(rUdk.getKey_UserLoginDeviceType(), rUdk.getValue_DeviceId());
                    roService.pExpire(rUdk.getKey_UserLoginDeviceType(), 30*60*1000);//30分钟后过期
                    UserPo upo=userService.getUserById(mup.getUserId());
                    roService.set(rUdk.getKey_DeviceType_UserId(), upo.getUserId());
                    roService.pExpire(rUdk.getKey_DeviceType_UserId(), 30*60*1000);//30分钟后过期
                    roService.set(rUdk.getKey_DeviceType_UserInfo(), (JsonUtils.objToJson(upo.toHashMap4Mobile())));
                    roService.pExpire(rUdk.getKey_DeviceType_UserInfo(), 30*60*1000);//30分钟后过期

                    map.put("ReturnType", "1001");
                    map.put("Msg", "设备自动登录成功");
                    map.put("UserId", mup.getUserId());
                    map.put("UserInfo", upo.toHashMap4Mobile());
                }
            } else {
                map.put("ReturnType", "2002");
                map.put("Msg", "设备无法自动登录");
            }

//            //检查是否有这个登录
//            //byte[] _value=conn.get(rUdk.getKey_UserLoginStatus());
//            if (_value!=null) {//已经登录
//                conn.set(rUdk.getKey_UserLoginStatus(), (System.currentTimeMillis()+"::"+operDesc));
//                conn.expire(rUdk.getKey_UserLoginStatus(), 30*60);//30分钟后过期
//                try {//这里可能有问题，先这样
//                    conn.expire(rUdk.getKey_UserLoginDeviceType(), 30*60);//30分钟后过期
//                    conn.expire(rUdk.getKey_DeviceType_UserId(), 30*60);//30分钟后过期
//                    conn.expire(rUdk.getKey_DeviceType_UserInfo(), 30*60);//30分钟后过期
//                } catch(Exception e) {
//                }
//                map.put("ReturnType", "1001");
//                map.put("UserId", rUdk.getUserId());
//                try {
//                    Map<String, Object> um=(Map<String, Object>)JsonUtils.jsonToObj(new String(conn.get(rUdk.getKey_DeviceType_UserInfo())), Map.class);
//                    map.put("UserInfo", um);
//                } catch(Exception e) {
//                }
//                map.put("Msg", "用户已登录");
//            } else {//未登录
//                //查找用户
//                MobileUDKey mUdk=new MobileUDKey(udk);
//                if (mUdk.isUser()) {
//                    UserPo up=userService.getUserById(mUdk.getUserId());
//                    if (up==null) {
//                        map.put("ReturnType", "1002");
//                        map.put("Msg", "不能找到相应的用户");
//                    } else {
//                        MobileUsedPo mup=muService.getUsedInfo(udk.getDeviceId(), udk.getPCDType());
//                        if (mup.getStatus()==1&&mup.getUserId().equals(udk.getUserId())) {//自动登录
//                            //删除之前的用户设备登录信息
//                            try {
//                                byte[] bs=conn.get(rUdk.getKey_UserLoginDeviceType());
//                                if (bs!=null&&bs.length>0) {
//                                    String deviceId=new String(bs);
//                                    RedisUserDeviceKey _oldKey=new RedisUserDeviceKey(udk);
//                                    _oldKey.setDeviceId(deviceId);
//                                    conn.del(_oldKey.getKey_UserLoginStatus());
//                                    conn.del(_oldKey.getKey_UserLoginDeviceType());
//                                    conn.del(_oldKey.getKey_DeviceType_UserId());
//                                    conn.del(_oldKey.getKey_DeviceType_UserInfo());
//                                }
//                            } catch(Exception e) {
//                            }
//                            conn.set(rUdk.getKey_UserLoginStatus(), (System.currentTimeMillis()+"::register"));
//                            conn.expire(rUdk.getKey_UserLoginStatus(), 30*60);//30分钟后过期
//                            conn.set(rUdk.getKey_UserLoginDeviceType(), rUdk.getValue_DeviceId());
//                            conn.expire(rUdk.getKey_UserLoginDeviceType(), 30*60);//30分钟后过期
//                            UserPo upo=userService.getUserById(mup.getUserId());
//                            conn.set(rUdk.getKey_DeviceType_UserId(), upo.getUserId());
//                            conn.expire(rUdk.getKey_DeviceType_UserId(), 30*60);//30分钟后过期
//                            conn.set(rUdk.getKey_DeviceType_UserInfo(), (JsonUtils.objToJson(upo.toHashMap4Mobile())));
//                            conn.expire(rUdk.getKey_DeviceType_UserInfo(), 30*60);//30分钟后过期
//                            map.put("ReturnType", "1001");
//                            map.put("Msg", "设备自动登录成功");
//                        } else {
//                            map.put("ReturnType", "2003");
//                            map.put("Msg", "请先登录");
//                        }
//                    }
//                } else {//不是User，自动登录
//                    if (operDesc.equals("common/entryApp")||udk.getPCDType()==3) {//自动登录
//                        MobileUsedPo mup=muService.getUsedInfo(udk.getDeviceId(), udk.getPCDType());
//                        if (mup!=null&&mup.getStatus()==1) {//自动登录
//                            //删除之前的用户设备登录信息
//                            try {
//                                byte[] bs=conn.get(rUdk.getKey_UserLoginDeviceType());
//                                if (bs!=null&&bs.length>0) {
//                                    String deviceId=new String(bs);
//                                    RedisUserDeviceKey _oldKey=new RedisUserDeviceKey(udk);
//                                    _oldKey.setDeviceId(deviceId);
//                                    conn.del(_oldKey.getKey_UserLoginStatus());
//                                    conn.del(_oldKey.getKey_UserLoginDeviceType());
//                                    conn.del(_oldKey.getKey_DeviceType_UserId());
//                                    conn.del(_oldKey.getKey_DeviceType_UserInfo());
//                                }
//                            } catch(Exception e) {
//                            }
//                            rUdk.setUserId(mup.getUserId());
//                            udk.setUserId(mup.getUserId());
//                            conn.set(rUdk.getKey_UserLoginStatus(), (System.currentTimeMillis()+"::register"));
//                            conn.expire(rUdk.getKey_UserLoginStatus(), 30*60);//30分钟后过期
//                            conn.set(rUdk.getKey_UserLoginDeviceType(), rUdk.getValue_DeviceId());
//                            conn.expire(rUdk.getKey_UserLoginDeviceType(), 30*60);//30分钟后过期
//                            UserPo upo=userService.getUserById(mup.getUserId());
//                            conn.set(rUdk.getKey_DeviceType_UserId(), upo.getUserId());
//                            conn.expire(rUdk.getKey_DeviceType_UserId(), 30*60);//30分钟后过期
//                            conn.set(rUdk.getKey_DeviceType_UserInfo(), (JsonUtils.objToJson(upo.toHashMap4Mobile())));
//                            conn.expire(rUdk.getKey_DeviceType_UserInfo(), 30*60);//30分钟后过期
//
//                            map.put("ReturnType", "1001");
//                            map.put("Msg", "设备自动登录成功");
//                            map.put("UserId", mup.getUserId());
//                            map.put("UserInfo", upo.toHashMap4Mobile());
//                        } else {
//                            map.put("ReturnType", "2002");
//                            map.put("Msg", "设备无法自动登录");
//                        }
//                    } else {
//                        map.put("ReturnType", "2003");
//                        map.put("Msg", "请先登录");
//                    }
//                }
//            }
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
            //删除之前的用户设备登录信息
            try {
                String bs=roService.get(rUdk.getKey_UserLoginDeviceType());
                if (bs!=null&&bs.length()>0) {
                    String deviceId=new String(bs);
                    RedisUserDeviceKey _oldKey=new RedisUserDeviceKey(udk);
                    _oldKey.setDeviceId(deviceId);
                    roService.del(_oldKey.getKey_UserLoginStatus());
                    roService.del(_oldKey.getKey_UserLoginDeviceType());
                    roService.del(_oldKey.getKey_DeviceType_UserId());
                    roService.del(_oldKey.getKey_DeviceType_UserInfo());
                }
            } catch(Exception e) {
            }
            roService.set(rUdk.getKey_UserLoginStatus(), (System.currentTimeMillis()+"::register"));
            roService.pExpire(rUdk.getKey_UserLoginStatus(), 30*60*1000);//30分钟后过期
            roService.set(rUdk.getKey_UserLoginDeviceType(), rUdk.getValue_DeviceId());
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
}