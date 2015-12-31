package com.woting.appengine.mobile.model;

import com.spiritdata.framework.core.model.BaseObject;
import com.spiritdata.framework.util.StringUtils;
/**
 * 移动端公共参数，包括：<br/>
 * <pre>
 * mType:设备型号;
 * imei:设备串号;
 * gps:GPS信息;
 * screenSize:屏幕尺寸;
 * sessionId:会话ID;Web应用用到
 * </pre>
 * @author wh
 */
public class MobileParam extends BaseObject {
    private static final long serialVersionUID = 4432329028811656556L;

    private String MType;//设备型号
    private String imei;//设备串号
    private String machine;//机器名称
    private String userId;//用户
    private String gps;//GPS信息
    private String screenSize;//屏幕尺寸
    private String sessionId;//会话ID

    public String getMType() {
        return MType;
    }
    public void setMType(String MType) {
        this.MType = MType;
    }
    public String getImei() {
        return imei;
    }
    public void setImei(String imei) {
        this.imei = imei;
    }
    public String getMachine() {
        return machine;
    }
    public void setMachine(String machine) {
        this.machine = machine;
    }
    public String getGps() {
        return gps;
    }
    public void setGps(String gps) {
        this.gps = gps;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getScreenSize() {
        return screenSize;
    }
    public void setScreenSize(String screenSize) {
        this.screenSize = screenSize;
    }
    public String getSessionId() {
        return sessionId;
    }
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获得对应的SessionKey，若IMEI为空，则返回空
     * @return
     */
    public MobileKey getMobileKey() {
        if (StringUtils.isNullOrEmptyOrSpace(this.imei)) return null;
        MobileKey sk = new MobileKey();
        sk.setMobileId(this.imei);
        sk.setUserId(StringUtils.isNullOrEmptyOrSpace(this.userId)?this.imei:this.userId);
        return sk;
    }
}