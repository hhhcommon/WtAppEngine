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

    /**
     * 加入内存
     * @param wt
     * @return 返回内存中与这个对讲对应的结构，若内存中已经存在，则返回内存中的结构，否则返回这个新结构
     */
    public WholeTalk addWt(WholeTalk wt) {
        WholeTalk ret=this.tm.talkMap.get(wt.getTalkerMk().toString());
        this.tm.talkMap.put(wt.getTalkerMk().toString(), wt);
        ret=wt;
        return ret;
    }
}