package com.woting.redis.session;

/**
 * 用于Redis数据操作中，生成Key或相应的值。本接口仅为实现RedisSession框架中的数据获取。
 * RedisSession可被多平台实现
 * @author wanghui
 */
public interface RedisLoginData {
    /**
     * 得到用户登录的锁的key值
     * @return 用户登录锁的key值
     */
    public String getKey_Lock();

    /**
     * 得到用户设备登录状态的key值
     * @return 用户设备登录状态的key值
     */
    public String getKey_UserLoginStatus();

    /**
     * 得到用户登录设备类型的key值
     * @return 用户登录设备类型的key值
     */
    public String getKey_UserLoginDeviceType();

    /**
     * 得到用户登录设备Id<br>
     * 与getKey_UserLoginDeviceType方法配合使用
     * @return 用户登录设备Id
     */
    public String getValue_DeviceId();
}