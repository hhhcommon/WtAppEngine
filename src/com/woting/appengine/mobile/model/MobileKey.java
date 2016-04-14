package com.woting.appengine.mobile.model;

import java.io.Serializable;

import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.common.util.MobileUtils;

/**
 * 会话key，包括设备ID和用户Id
 * @author wh
 */
public class MobileKey implements Serializable {
    private static final long serialVersionUID = 8584805045595806786L;

    private String mobileId; //设备Id，IMEI
    private String userId; //用户Id，若未登录，则用户Id为IMEI
    private int PCDType; //设备类型

    public String getMobileId() {
        return mobileId;
    }
    public void setMobileId(String mobileId) {
        this.mobileId = mobileId;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public int getPCDType() {
        return PCDType;
    }
    public void setPCDType(int PCDType) {
        this.PCDType = PCDType;
    }

    /**
     * 是否是用户
     */
    public boolean isUser() {
        return MobileUtils.isValidUserId(this.userId);
    }
    /**
     * 是否是设备
     */
    public boolean isMobile() {
        return !MobileUtils.isValidUserId(this.userId);
    }
    /**
     * 是否是自制设备
     */
    public boolean isWTDevice() {
        return this.PCDType==2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj==null||!(obj instanceof MobileKey)) return false;
        try {
            MobileKey _mk=(MobileKey)obj;
            if (this.mobileId.equals(_mk.getMobileId())&&this.userId.equals(_mk.getUserId())&&this.PCDType==_mk.getPCDType()) return true;
        } catch(Exception e) {}
        return false;
    }

    /**
     * 获得SessionId，SessionId就是UserId
     * @return
     */
    public String getSessionId() {
        return StringUtils.isNullOrEmptyOrSpace(this.userId)?this.mobileId:this.userId;
    }

    /**
     * 获得SessionId，SessionId就是UserId
     * @return
     */
    public String toString() {
        return this.mobileId+"::"+this.PCDType+"::"+this.userId;
    }
}