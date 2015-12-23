package com.woting.mobile.session.mem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.woting.mobile.session.MobileSessionConfig;
import com.woting.mobile.session.model.MobileSession;
import com.woting.mobile.model.MobileKey;

/**
 * Session在内存中的对象
 * @author wanghui
 */
public class SessionMemory {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static SessionMemory instance = new SessionMemory();
    }
    public static SessionMemory getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    protected Map<MobileKey, MobileSession> mSessionMap;//移动会话Map

    public SessionMemory() {
        this.mSessionMap = new ConcurrentHashMap<MobileKey, MobileSession>();
    }

    /**
     * 增加一个Session结构
     * @param ms
     */
    public void add(MobileSession ms) {
        mSessionMap.put(ms.getKey(), ms);
    }
}