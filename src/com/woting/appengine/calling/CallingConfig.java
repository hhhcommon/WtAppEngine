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

    private int IT1_EXPIRE=10000;//检查是否在线的过期时间，默认10秒
    public int getIT1_EXPIRE() {
        return IT1_EXPIRE;
    }

    private int IT2_EXPIRE=60000;//检查无应答的过期时间，默认1分钟
    public int getIT2_EXPIRE() {
        return IT2_EXPIRE;
    }
    private int IT3_EXPIRE=120000;//检查通话过期时间，默认2分钟
    public int getIT3_EXPIRE() {
        return IT3_EXPIRE;
    }
}