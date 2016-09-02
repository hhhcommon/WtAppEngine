package com.woting.appengine.mobile;

import java.io.Serializable;

import com.spiritdata.framework.util.StringUtils;
import com.woting.passport.session.key.UserDeviceKey;

/**
 * 移动端设备用户key
 * @author wanghui
 */
public class MobileUDKey extends UserDeviceKey implements Serializable {
    private static final long serialVersionUID = -1794652738025588641L;

    /**
     * 判断此Key是否是User用户
     * @return
     */
    public boolean isUser() {
        return !StringUtils.isNullOrEmptyOrSpace(this.userId);
    }

    /**
     * 获得SessionId
     * @return
     */
    public String getSessionId() {
        return StringUtils.isNullOrEmptyOrSpace(this.userId)?this.deviceId:this.userId;
    }
}