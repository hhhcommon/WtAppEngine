package com.woting.appengine.mobile.session.mem;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.util.StringUtils;
import com.woting.WtAppEngineConstants;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.session.model.MobileSession;
import com.woting.version.core.model.VersionConfig;
import com.woting.version.core.service.VersionService;

/**
 * 会话管理类，也就是会话的服务类
 * @author wh
 */
public class SessionMemoryManage {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static SessionMemoryManage instance=new SessionMemoryManage();
    }
    public static SessionMemoryManage getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    /**
     * 移动会话内存数据
     */
    protected SessionMemory sm=null;

    private VersionService versionService=null;
    
    /*
     * 构造方法，设置移动会话内存数据
     */
    private SessionMemoryManage() {
        this.sm=SessionMemory.getInstance();
    }

    /**
     * 清除过期的会话
     */
    public void clean() {
        //加载版本配置
        if (versionService==null) {
            //加载版本配置
            ServletContext sc=(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent();
            if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                versionService=(VersionService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("versonService");
            }

        } else {
            SystemCache.setCache(
                new CacheEle<VersionConfig>(WtAppEngineConstants.APP_VERSIONCONFIG, "版本设置", versionService.getVerConfig())
            );
        }
        //清除会话
        List<MobileSession> beRemovedList= new ArrayList<MobileSession>();
        if (this.sm.mSessionMap!=null&&!this.sm.mSessionMap.isEmpty()) {
            for (String sKey: this.sm.mSessionMap.keySet()) {
                MobileSession ms=this.sm.mSessionMap.get(sKey);
                if (ms.expired()) {
                    beRemovedList.add(this.sm.mSessionMap.remove(sKey));
                }
            }
        }
        if (!beRemovedList.isEmpty()) {
            for (MobileSession ms: beRemovedList) {
                List<MobileSession> ul=this.sm.mUserSessionListMap.get(ms.getKey().getUserId());
                if (ul!=null&&!ul.isEmpty()) {
                    for (MobileSession _ms: ul) {
                        if (_ms.hashCode()==ms.hashCode()) ul.remove(_ms);
                    }
                    if (ul.isEmpty()) this.sm.mUserSessionListMap.remove(ms.getKey().getUserId());
                }
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
                MobileSession ms=this.sm.mSessionMap.get(sKey);
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
     * 根据userId获得可用激活的Session。设备优先
     * @param userId 用户Id
     * @return 对应的Session
     */
    public MobileSession getActivedUserSessionByUserId(String userId) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return null;

        List<MobileSession> ul=this.sm.mUserSessionListMap.get(userId);
        if (ul!=null&&!ul.isEmpty()) {
            MobileSession retMs=ul.get(0);
            for (int i=1;i<ul.size()-1;i++) {
                MobileSession ms=ul.get(i);
                if (ms.getKey().getPCDType()==2&&retMs.getKey().getPCDType()==1) retMs=ms;
                else if (ms.getKey().getPCDType()==retMs.getKey().getPCDType()) {
                    if (ms.getLastAccessedTime()>retMs.getLastAccessedTime()) retMs=ms;
                }
            }
            return retMs;
        }
        return null;
    }
}