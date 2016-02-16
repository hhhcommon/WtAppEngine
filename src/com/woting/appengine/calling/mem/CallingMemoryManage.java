package com.woting.appengine.calling.mem;

import com.woting.appengine.calling.model.OneCall;

public class CallingMemoryManage {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static CallingMemoryManage instance = new CallingMemoryManage();
    }
    public static CallingMemoryManage getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    //数据区
    protected CallingMemory cm; //电话内存结构

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
        this.cm.callMap.put(oc.getCallId(), oc);
        return 1;
    }

    /**
     * 判断是否有人在通话
     * @param callorId 通话者Id
     * @return 若有人在通话，返回true
     */
    public boolean isTalk(String callorId) {
        OneCall oc=null;
        for (String k: this.cm.callMap.keySet()) {
            oc=this.cm.callMap.get(k);
            if (oc==null) continue;
            if (oc.getCallorId().equals(callorId)||oc.getDiallorId().equals(callorId)) return true;
        }
        return false;
    }

    /**
     * 根据callId获得电话通话数据
     * @param callId 电话通话Id
     * @return 电话通话数据
     */
    public OneCall getCallData(String callId) {
        return (this.cm!=null&&this.cm.callMap!=null)?this.cm.callMap.get(callId):null;
    }
}