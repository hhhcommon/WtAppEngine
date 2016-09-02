package com.woting.appengine.mobile;

import java.util.Map;
import com.spiritdata.framework.util.StringUtils;

public abstract class MobileUtils {
    /**
     * 根据Map生成移动端参数数据
     * @param m Map参数
     * @return 移动端参数数据
     */
    public static MobileParam getMobileParam(Map<String, Object> m) {
        if (m==null||m.size()==0) return null;

        MobileParam mp=new MobileParam();

        Object o=m.get("IMEI");
        String __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setImei(__tmp);
        o=m.get("PCDType");
        __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setPCDType(__tmp);
        o=m.get("MobileClass");
        o=m.get("GPS-longitude");
        __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setGpsLongitude(__tmp);
        o=m.get("GPS-Latitude");
        __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setGpsLatitude(__tmp);
        o=m.get("ScreenSize");
        __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setScreenSize(__tmp);
        o=m.get("SessionId");
        __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setSessionId(__tmp);
        o=m.get("UserId");
        __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setUserId(__tmp);

        if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getGpsLongitude())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getGpsLatitude())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getScreenSize())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getSessionId())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getUserId())) {
            return null;
        } else {
           
        }
        return mp;
    }
}
