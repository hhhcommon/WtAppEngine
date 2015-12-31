package com.woting.appengine.mobile.push;

/**
 * 推送相关常量设置
 * @author wanghui
 */
public class PushConfig {
    private int THREADCOUNT_DEALRECEIVEQUEUE=2;//处理原生接收队列线程的个数
    public int getTHREADCOUNT_DEALRECEIVEQUEUE() {
        return THREADCOUNT_DEALRECEIVEQUEUE;
    }

    private int PORT_PUSHSERVER=15678;//推送服务端口号
    public int getPORT_PUSHSERVER() {
        return PORT_PUSHSERVER;
    }

    //检查清除会话的间隔时间，毫秒
    private int CLEAN_INTERVAL=5*60*1000;//5分钟
    public int getCLEAN_INTERVAL() {
        return CLEAN_INTERVAL;
    }
    public void setCLEAN_INTERVAL(int CLEAN_INTERVAL) {
        this.CLEAN_INTERVAL = CLEAN_INTERVAL;
    }
}