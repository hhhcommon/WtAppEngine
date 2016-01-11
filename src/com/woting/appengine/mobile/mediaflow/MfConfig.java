package com.woting.appengine.mobile.mediaflow;

public class MfConfig {
    private int THREADCOUNT_DEALMF=2;//处理原生接收队列线程的个数
    public int getTHREADCOUNT_DEALMF() {
        return THREADCOUNT_DEALMF;
    }
}