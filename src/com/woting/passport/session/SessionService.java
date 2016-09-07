package com.woting.passport.session;

import java.util.List;
import java.util.Map;

import com.woting.passport.session.key.UserDeviceKey;

/**
 * 与Session处理相关的方法
 * @author wanghui
 */
public interface SessionService {
    /**
     * 处理用户设备Key进入系统，若未登录要看之前的登录情况，自动登录
     * @param udk 根据用户设备Key
     * @param operDesc 操作的描述
     * @return
     */
    public Map<String, Object> dealUDkeyEntry(UserDeviceKey udk, String operDesc);

    /**
     * 返回当前用户活动的用户列表，判断活动用户：
     * 若设备和App同时登录，则返回两个用户
     * @param udk 根据用户设备Key
     * @return UserDeviceKey
     */
    public List<? extends UserDeviceKey> getActivedUserUDKs(String userId);

    /**
     * 返回当前用户活动的用户,根据用户名称和设备类型，获得当前的活跃用户的设备用户Key
     * @param userId 用户Id
     * @param pcdType 设备类型
     * @return UserDeviceKey
     */
    public UserDeviceKey getActivedUserUDK(String userId, int pcdType);

    /**
     * 把用户设备注册到Redis中
     * @param mUdk
     */
    public void registUser(UserDeviceKey udk);

    /**
     * 登录用户的注销
     * @param mUdk
     */
    public void logoutSession(UserDeviceKey udk);
}