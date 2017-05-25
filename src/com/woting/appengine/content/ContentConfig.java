package com.woting.appengine.content;

import com.woting.push.config.Config;

/**
 * Socket连接客户端配置信息
 * @author wanghui
 */
public class ContentConfig implements Config {
    private boolean useRedis; //是否使用Redis获取内容数据，用以提速
    public boolean isUseRedis() {
        return useRedis;
    }
    public void setUseRedis(boolean useRedis) {
        this.useRedis=useRedis;
    }

    private boolean resident; //是否使用Redis获取内容数据，用以提速
    public boolean isResident() {
        return resident;
    }
    public void setResident(boolean resident) {
        this.resident=resident;
    }

    private long expiredTime; //redis过期时间
    public long getExpiredTime() {
        return expiredTime;
    }
    public void setExpiredTime(long expiredTime) {
        this.expiredTime = expiredTime;
    }
}