package com.woting.appengine.mobile.mediaflow;

public class MfConfig {
    private int THREADCOUNT_DEALMF=2;//处理原生接收队列线程的个数
    public int getTHREADCOUNT_DEALMF() {
        return THREADCOUNT_DEALMF;
    }

    //检查清除对话的间隔时间
    private int CLEAN_INTERVAL=1000;
    public int getCLEAN_INTERVAL() {
        return CLEAN_INTERVAL;
    }
    public void setCLEAN_INTERVAL(int CLEAN_INTERVAL) {
        this.CLEAN_INTERVAL=CLEAN_INTERVAL;
    }
}