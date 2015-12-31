package com.woting.appengine.mobile.session;

/**
 * 移动会话的配置信息
 * @author wh
 */
public class MobileSessionConfig {
    //过期时间，默认为30分钟
    private int TIMEOUT=30*1000*60;

    public int getTIMEOUT() {
        return TIMEOUT;
    }
    public void setTIMEOUT(int TIMEOUT) {
        this.TIMEOUT=TIMEOUT;
    }

    //检查清除会话的间隔时间
    private int CLEAN_INTERVAL=1000;

    public int getCLEAN_INTERVAL() {
        return CLEAN_INTERVAL;
    }
    public void setCLEAN_INTERVAL(int CLEAN_INTERVAL) {
        this.CLEAN_INTERVAL=CLEAN_INTERVAL;
    }
}