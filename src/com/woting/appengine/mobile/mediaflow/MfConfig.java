package com.woting.appengine.mobile.mediaflow;

public class MfConfig {
    private int THREADCOUNT_DEALMF=2;//处理原生接收队列线程的个数
    public int getTHREADCOUNT_DEALMF() {
        return THREADCOUNT_DEALMF;
    }

    private int CLEAN_INTERVAL=1000;//检查清除对话的间隔时间,1秒
    public int getCLEAN_INTERVAL() {
        return CLEAN_INTERVAL;
    }
}