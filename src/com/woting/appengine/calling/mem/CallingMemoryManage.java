package com.woting.appengine.calling.mem;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.woting.appengine.calling.model.OneCall;

public class CallingMemoryManage {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static CallingMemoryManage instance=new CallingMemoryManage();
    }
    public static CallingMemoryManage getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    //数据区
    protected CallingMemory cm; //电话内存结构
    private static final ReadWriteLock lock=new ReentrantReadWriteLock(); //读写锁 

    /*
     * 构造方法，初始化用户组的内存结构
     */
    private CallingMemoryManage() {
        this.cm=CallingMemory.getInstance();
    }

    /**
     * 把一个新的会话处理加入内存Map
     * @param oc 新的会话
     * @return 成功返回1，若已经存在这个会话返回0，若这个会话不是新会话返回-1
     */
    public int addOneCall(OneCall oc) {
        if (oc.getStatus()!=0) return -1;//不是新会话
        if (this.cm.callMap.get(oc.getCallId())!=null) return 0;
        lock.writeLock().lock();
        try {
            this.cm.callMap.put(oc.getCallId(), oc);
        } finally {
            lock.writeLock().unlock();
        }
        return 1;
    }

    /**
     * 判断是否有人在通话
     * @param callorId 通话者Id
     * @param callId 通话Id
     * @return 若有人在通话，返回true
     */
    public boolean isTalk(String callederId, String callId) {
        OneCall oc=null;
        lock.readLock().lock();
        try {
            for (String k: this.cm.callMap.keySet()) {
                oc=this.cm.callMap.get(k);
                if (oc.getStatus()==9||oc.getCallId().equals(callId)) continue;
                if (oc.getCallerId().equals(callederId)||oc.getCallederId().equals(callederId)) return true;
            }
        } finally {
            lock.readLock().unlock();
        }
        return false;
    }

    /**
     * 根据callId获得电话通话数据
     * @param callId 电话通话Id
     * @return 电话通话数据
     */
    public OneCall getCallData(String callId) {
        lock.readLock().lock();
        try {
            if (this.cm!=null&&this.cm.callMap!=null) {
                OneCall oc=this.cm.callMap.get(callId);
                if (oc!=null&&oc.getStatus()!=9&&oc.getStatus()!=4) return oc;
            }
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    //这个现在不用了
    public void removeCallData(String callId) {
        lock.writeLock().lock();
        try {
            if (this.cm!=null&&this.cm.callMap!=null) {
                if (this.cm.callMap.get(callId)!=null) this.cm.callMap.get(callId).setStatus_9();
                this.cm.callMap.remove(callId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}