package com.woting.appengine.intercom;

public class InterComConfig {
    private int THREADCOUNT_DEALINTERCOM=2;//处理原生接收队列线程的个数
    public int getTHREADCOUNT_DEALINTERCOM() {
        return THREADCOUNT_DEALINTERCOM;
    }

    private int CLEANSPEAKER_INTERVAL=2000;//检查清除对讲者标识的时间间隔，2秒
    public int getCLEANSPEAKER_INTERVAL() {
        return CLEANSPEAKER_INTERVAL;
    }

    private int SPEAKER_EXPIRE=2000;//对讲者过期时间，默认2秒；若在一个对讲组内，在这个时间内未收到任何对讲者的信息，就视为对讲过期
    public int getSPEAKER_EXPIRE() {
        return SPEAKER_EXPIRE;
    }
}