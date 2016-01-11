package com.woting.appengine.mobile.mediaflow.mem;

import com.woting.appengine.mobile.mediaflow.model.WholeTalk;
import com.woting.appengine.mobile.model.MobileKey;

public class TalkMemoryManage {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static TalkMemoryManage instance = new TalkMemoryManage();
    }
    public static TalkMemoryManage getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    //数据区
    protected TalkMemory tm;

    /*
     * 构造方法，初始化消息推送的内存结构
     */
    private TalkMemoryManage() {
        tm=TalkMemory.getInstance();
    }

    public WholeTalk getWholeTalk(MobileKey mk) {
        return this.tm.talkMap.get(mk.toString());
    }

    public void removeWt(WholeTalk wt) {
        this.tm.talkMap.remove(wt.getTalkerMk().toString());
    }
}