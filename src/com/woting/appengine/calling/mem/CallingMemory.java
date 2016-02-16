package com.woting.appengine.calling.mem;

import java.util.concurrent.ConcurrentHashMap;
import com.woting.appengine.calling.model.OneCall;

public class CallingMemory {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static CallingMemory instance = new CallingMemory();
    }
    public static CallingMemory getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    protected ConcurrentHashMap<String, OneCall> callMap;//对讲组信息Map

    private CallingMemory() {
        this.callMap=new ConcurrentHashMap<String, OneCall>();
    }
}