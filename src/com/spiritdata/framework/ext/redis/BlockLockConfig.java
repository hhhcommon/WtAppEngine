package com.spiritdata.framework.ext.redis;

/**
 * 阻塞锁的设置参数：
 * <pre>
 *   loopIntervalTime:每次循环间隔时间(默认5毫秒)
 *   waitingType:容忍方式(默认采用1)：
 *     =1仅采用loop容忍方式；
 *     =2仅采用waitingTime的容忍方式；
 *     =3两种方式都采用，任何一个满足条件则抛出异常
 *     =4两种方式都采用，两个条件都满足，才抛出异常
 *     =其他值：不进行任何等待，只尝试一次；
 *   waitingLoopCount:容忍的最大循环次数(默认1次)
 *   waitingTime:容忍的最长时间(默认5毫秒)
 * </pre>
 * @author wanghui
 */
public class BlockLockConfig {
    private long loopIntervalTime=5; //每次循环间隔时间，默认为5毫秒
    private int waitingType=1; //容忍方式，默认仅采用循环方式
    private long waitingLoopCount=1; //容忍的最大循环次数，默认=1次
    private long waitingTime=5; //容忍的最长时间，默认为5毫秒

    public long getLoopIntervalTime() {
        return loopIntervalTime;
    }
    public void setLoopIntervalTime(long loopIntervalTime) {
        this.loopIntervalTime = loopIntervalTime;
    }
    public int getWaitingType() {
        return waitingType;
    }
    public void setWaitingType(int waitingType) {
        this.waitingType = waitingType;
    }
    public long getWaitingLoopCount() {
        return waitingLoopCount;
    }
    public void setWaitingLoopCount(long waitingLoopCount) {
        this.waitingLoopCount = waitingLoopCount;
    }
    public long getWaitingTime() {
        return waitingTime;
    }
    public void setWaitingTime(long waitingTime) {
        this.waitingTime = waitingTime;
    }
}