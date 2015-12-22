package com.woting.mobile.session.mem;

import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.StringUtils;
import com.woting.mobile.session.model.MobileSession;
import com.woting.mobile.model.MobileKey;
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
        SessionMemoryManage smm = InstanceHolder.instance;
        return smm;
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
        //System.out.println("【"+(new java.util.Date())+"】：清除过期会话");
        //清除会话
        if (this.sm.mSessionMap!=null&&!this.sm.mSessionMap.isEmpty()) {
            for (MobileKey sKey: this.sm.mSessionMap.keySet()) {
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
            for (MobileKey sKey: this.sm.mSessionMap.keySet()) {
                if (sKey.getMobileId().equals(imei)) {
                    MobileSession ms = this.sm.mSessionMap.get(sKey);
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
     * 得到SessionKey对应的Session
     * @param sk Session的key
     * @return 对应的Session，若没有或者过期，返回null
     */
    public MobileSession getSession(MobileKey sk) {
        if (sk==null) return null;
        MobileSession ms = null;
        if (this.sm.mSessionMap!=null&&this.sm.mSessionMap.size()>0) {
            for (MobileKey sKey: this.sm.mSessionMap.keySet()) {
                if (sKey.equals(sk)) {
                    ms=this.sm.mSessionMap.get(sKey);
                    break;
                }
            }
        }
        if (ms!=null&&ms.expired()) return null;
        return ms;
    }

    /**
     * 根据Imei和userId获得
     * @param userId 用户Id
     * @param imei 手机串号
     * @return 对应的Session
     */
    public MobileSession getUserSession(String userId, String imei) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)||StringUtils.isNullOrEmptyOrSpace(imei)) return null;
        if (this.sm.mSessionMap!=null&&this.sm.mSessionMap.size()>0) {
            for (MobileKey sKey: this.sm.mSessionMap.keySet()) {
                if (sKey.getMobileId().equals(imei)) {
                    MobileSession ms = this.sm.mSessionMap.get(sKey);
                    UserPo u = (UserPo)ms.getAttribute("user");
                    if (u!=null&&u.getUserId().equals(userId)&&!ms.expired()) return ms;
                }
            }
        }
        return null;
    }

    /**
     * 把内存情况转换为Json串，便于调试
     * @return
     */
    public String Mem2Json() {
        return JsonUtils.objToJson(this.sm.mSessionMap);
    }
}