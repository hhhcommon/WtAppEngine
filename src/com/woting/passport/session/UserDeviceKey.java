package com.woting.passport.session;

import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.mobile.model.MobileKey;

/**
 * 用户
 * @author wanghui
 */
public class UserDeviceKey {
    private static final long serialVersionUID = 8584805045595806786L;

    private String deviceId; //设备Id，移动设备就是IMEI
    private String userId; //用户Id，若未登录，则用户Id为IMEI
    private int PCDType; //设备类型；1是

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
            return obj.hashCode()==this.hashCode();
        } catch(Exception e) {}
        return false;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
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