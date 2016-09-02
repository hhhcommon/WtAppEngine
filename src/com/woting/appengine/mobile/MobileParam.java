package com.woting.appengine.mobile;

import java.util.Map;

import com.spiritdata.framework.core.model.BaseObject;
import com.spiritdata.framework.util.StringUtils;
import com.woting.passport.session.key.GetUserDeviceKey;
/**
 * 移动端公共参数，包括：<br/>
 * <pre>
 * imei:设备串号;
 * PCDType:设备类型;
 * mClass:设备型号;
 * gps:GPS信息;
 * screenSize:屏幕尺寸;
 * sessionId:会话ID;Web应用用到
 * </pre>
 * @author wh
 */
public class MobileParam extends BaseObject implements GetUserDeviceKey {
    private static final long serialVersionUID=4432329028811656556L;

    private String imei;//设备串号
    private String PCDType;//设备类型
    private String MClass;//设备型号，自制设备的型号；手机的型号；PC机的型号；Pad的型号等
    private String userId;//用户
    private String gpsLongitude;//GPS信息-经度
    private String gpsLatitude;//GPS信息-纬度
    private String screenSize;//屏幕尺寸
    private String sessionId;//会话ID，或UserId

    public String getImei() {
        return imei;
    }
    public void setImei(String imei) {
        this.imei=imei;
    }
    public String getPCDType() {
        return PCDType;
    }
    public void setPCDType(String PCDType) {
        this.PCDType=PCDType;
    }
    public String getMClass() {
        return MClass;
    }
    public void setMClass(String MClass) {
        this.MClass=MClass;
    }
    public String getGpsLongitude() {
        return gpsLongitude;
    }
    public void setGpsLongitude(String gpsLongitude) {
        this.gpsLongitude=gpsLongitude;
    }
    public String getGpsLatitude() {
        return gpsLatitude;
    }
    public void setGpsLatitude(String gpsLatitude) {
        this.gpsLatitude=gpsLatitude;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId=userId;
    }
    public String getScreenSize() {
        return screenSize;
    }
    public void setScreenSize(String screenSize) {
        this.screenSize=screenSize;
    }
    public String getSessionId() {
        return sessionId;
    }
    public void setSessionId(String sessionId) {
        this.sessionId=sessionId;
    }

    /**
     * 获得对应的移动端用户设备Key——MobileUDKey<br/>
     * <pre>
     * 若IMEI为空，则返回空；
     * 若PCDType为空，则认为是手机，若其是一个未知值，则返回空；
     * </pre>
     * @return 移动端用户设备Key
     */
    public MobileUDKey getUserDeviceKey() {
        if (StringUtils.isNullOrEmptyOrSpace(this.imei)) return null;
        int _pcdType=0;
        if (StringUtils.isNullOrEmptyOrSpace(this.PCDType)) _pcdType=1;
        else {
            try {
                _pcdType=Integer.parseInt(this.PCDType); 
             } catch(Exception e) {
             }
             if (_pcdType<=0)  return null;
        }

        MobileUDKey udk=new MobileUDKey();
        udk.setDeviceId(this.imei);
        udk.setPCDType(_pcdType);
        udk.setUserId(this.userId);

        return udk;
    }
}