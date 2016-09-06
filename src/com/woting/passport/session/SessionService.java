package com.woting.passport.session;

import java.util.Map;

import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.key.UserDeviceKey;

/**
 * 与Session处理相关的方法
 * @author wanghui
 */
public interface SessionService {

    /**
     * 根据用户设备Key，获得该key在系统中的登录情况
     * @param udk 根据用户设备Key
     * @return
     */
    public Map<String, Object> getLoginStatus(UserDeviceKey udk);

    /**
     * 用户Session设置为过期
     * @param udk
     * @return
     */
    public boolean expireSession(UserDeviceKey udk);

    public UserDeviceKey getActivedUserUDK(String userId);

    public void registUser(MobileUDKey mUdk);
}