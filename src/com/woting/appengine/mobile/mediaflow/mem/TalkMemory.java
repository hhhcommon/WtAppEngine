package com.woting.appengine.mobile.mediaflow.mem;

import java.util.concurrent.ConcurrentHashMap;
import com.woting.appengine.mobile.mediaflow.model.WholeTalk;

public class TalkMemory {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static TalkMemory instance = new TalkMemory();
    }
    public static TalkMemory getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    protected ConcurrentHashMap<String, WholeTalk> talkMap;//对讲组信息Map

    private TalkMemory() {
        this.talkMap=new ConcurrentHashMap<String, WholeTalk>();
    }
}