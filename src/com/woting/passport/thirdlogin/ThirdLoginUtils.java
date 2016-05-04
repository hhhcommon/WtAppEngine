package com.woting.passport.thirdlogin;

import java.util.Map;

import com.spiritdata.framework.util.StringUtils;
import com.woting.passport.UGA.persistence.pojo.UserPo;

public abstract class ThirdLoginUtils {

    /**
     * 根据数据userInfo和第三方类型type，填全up信息
     * @param up UserPo对象
     * @param type 第三方类型
     * @param userInfo 用户数据
     */
    public static void fillUserInfo(UserPo up, String type, Map<String, Object> userInfo) {
        if (up==null) return;
        if (StringUtils.isNullOrEmptyOrSpace(type)) return;
        if (userInfo==null||userInfo.isEmpty()) return;

        if (type.equals("微信")) fillUserInfo_Weixin(up, userInfo);
        else 
        if (type.equals("微博")) fillUserInfo_Weibo(up, userInfo);
        else 
        if (type.equals("QQ")) fillUserInfo_QQ(up, userInfo);
    }

    private static void fillUserInfo_Weixin(UserPo up, Map<String, Object> userInfo) {
        
    }

    private static void fillUserInfo_Weibo(UserPo up, Map<String, Object> userInfo) {
        
    }

    private static void fillUserInfo_QQ(UserPo up, Map<String, Object> userInfo) {
        
    }
}