package com.woting.appengine.mobile.session.mem;

import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.util.StringUtils;
import com.woting.WtAppEngineConstants;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.session.model.MobileSession;
import com.woting.passport.UGA.persistence.pojo.UserPo;

/**
 * 会话管理类，也就是会话的服务类
 * @author wh
 */
public class SessionMemoryManage {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static SessionMemoryManage instance = new SessionMemoryManage();
    }
    public static SessionMemoryManage getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    /**
     * 移动会话内存数据
     */
    protected SessionMemory sm = null;

    /*
     * 构造方法，设置移动会话内存数据
     */
    private SessionMemoryManage() {
        this.sm = SessionMemory.getInstance();
    }

    /**
     * 清除过期的会话
     */
    public void clean() {
        //读取版本号
        SystemCache.setCache(
            new CacheEle<String>(WtAppEngineConstants.APP_VERSION, "移动端版本", MobileUtils.getVersion())
        );
        //清除会话
        if (this.sm.mSessionMap!=null&&!this.sm.mSessionMap.isEmpty()) {
            for (String sKey: this.sm.mSessionMap.keySet()) {
                MobileSession ms = this.sm.mSessionMap.get(sKey);
                if (ms.expired()) this.sm.mSessionMap.remove(sKey);
            }
        }
    }

    /**
     * 把所有IMEI对应的Session设置为过期
     * @param imei
     */
    public void expireAllSessionByIMEI(String imei) {
        if (this.sm.mSessionMap!=null&&this.sm.mSessionMap.size()>0
            &&!StringUtils.isNullOrEmptyOrSpace(imei)) {
            for (String sKey: this.sm.mSessionMap.keySet()) {
                MobileSession ms = this.sm.mSessionMap.get(sKey);
                if (ms.getKey().getMobileId().equals(imei)) {
                    ms.expire();
                }
            }
        }
    }

    /**
     * 加入一个Session
     * @param ms 移动会话
     */
    public void addOneSession(MobileSession ms) {
        this.sm.add(ms);
    }

    /**
     * 得到MobileKey对应的Session
     * @param mk MobileKey
     * @return 对应的Session，若没有或者过期，返回null
     */
    public MobileSession getSession(MobileKey mk) {
        if (mk==null) return null;
        MobileSession ms=this.sm.mSessionMap.get(mk.toString());
        if (ms!=null&&ms.expired()) return null;
        return ms;
    }

    /**
     * 根据IuserId获得
     * @param userId 用户Id
     * @return 对应的Session
     */
    public MobileSession getUserSessionByUserId(String userId) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return null;
        if (this.sm.mSessionMap!=null&&this.sm.mSessionMap.size()>0) {
            for (String sKey: this.sm.mSessionMap.keySet()) {
                MobileSession ms = this.sm.mSessionMap.get(sKey);
                if (ms.getKey().getUserId().equals(userId)) {
                    UserPo u = (UserPo)ms.getAttribute("user");
                    if (u!=null) return ms;
                }
            }
        }
        return null;
    }
}