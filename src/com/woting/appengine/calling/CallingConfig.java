package com.woting.appengine.calling;

/**
 * 电话处理的常量
 * @author wanghui
 */
public class CallingConfig {
    private int THREADCOUNT_DEALCALLING=2;//处理原生接收队列线程的个数，此线程包括分发和创建处理线程
    public int getTHREADCOUNT_DEALCALLING() {
        return THREADCOUNT_DEALCALLING;
    }

    private int IT1_EXPIRE=500;//检查是否在线的过期时间
    public int getIT1_EXPIRE() {
        return IT1_EXPIRE;
    }

    private int IT2_EXPIRE=30000;//检查无应答的过期时间，默认半分钟
    public int getIT2_EXPIRE() {
        return IT2_EXPIRE;
    }

}