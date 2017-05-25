package com.woting.passport.UGA;

import com.woting.push.config.Config;

/**
 * Socket连接客户端配置信息
 * @author wanghui
 */
public class PasswordConfig implements Config {
    private boolean useEncryption; //是否对密码进行加密
    public boolean isUseEncryption() {
        return useEncryption;
    }
    public void setUseEncryption(boolean useEncryption) {
        this.useEncryption=useEncryption;
    }

    private boolean reEncryption; //是否重新进行加密，只对那些未加密的密码进行处理
    public boolean isReEncryption() {
        return reEncryption;
    }
    public void setReEncryption(boolean reEncryption) {
        this.reEncryption=reEncryption;
    }

    private long expiredTime; //redis过期时间
    public long getExpiredTime() {
        return expiredTime;
    }
    public void setExpiredTime(long expiredTime) {
        this.expiredTime = expiredTime;
    }
}