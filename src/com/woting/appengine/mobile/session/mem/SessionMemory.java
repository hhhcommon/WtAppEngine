package com.woting.appengine.mobile.session.mem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.mobile.session.model.MobileSession;

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

    protected Map<String, MobileSession> mSessionMap;//移动会话Map
    protected Map<String, List<MobileSession>> mUserSessionListMap;//移动会话Map

    public SessionMemory() {
        this.mSessionMap = new ConcurrentHashMap<String, MobileSession>();
        this.mUserSessionListMap = new ConcurrentHashMap<String, List<MobileSession>>();
    }

    /**
     * 增加一个Session结构
     * @param ms
     */
    public void add(MobileSession ms) {
        mSessionMap.put(ms.getKey().toString(), ms);
        String userId=ms.getKey().getUserId();
        if (!StringUtils.isNullOrEmptyOrSpace(userId)) {
            List<MobileSession> ul=mUserSessionListMap.get(userId);
            if (ul==null) {
                ul=new ArrayList<MobileSession>();
                mUserSessionListMap.put(userId, ul);
            }
            ul.add(ms);
        }
    }
}