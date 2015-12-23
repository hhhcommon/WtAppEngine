package com.woting.intercom;

public class InterComConfig {
    private int THREADCOUNT_DEALINTERCOM=2;//处理原生接收队列线程的个数
    public int getTHREADCOUNT_DEALINTERCOM() {
        return THREADCOUNT_DEALINTERCOM;
    }
}