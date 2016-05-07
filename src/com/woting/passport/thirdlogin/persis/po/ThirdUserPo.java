package com.woting.passport.thirdlogin.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class ThirdUserPo extends BaseObject {
    private static final long serialVersionUID = -1409869382330754969L;

    private String id;  //第三方用户对照表id
    private String userId;  //我方系统用户Id
    private String thirdUserId;  //第三方中的用户唯一标识
    private String thirdLoginType;  //第三方用户登录的类型
    private String thirdUserInfo;  //第三方用户数据，以Json格式存储
    private int thirdLoginCount=1;  //第三方用户登录的次数
    private Timestamp CTime; //创建时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getThirdUserId() {
        return thirdUserId;
    }
    public void setThirdUserId(String thirdUserId) {
        this.thirdUserId = thirdUserId;
    }
    public String getThirdLoginType() {
        return thirdLoginType;
    }
    public void setThirdLoginType(String thirdLoginType) {
        this.thirdLoginType = thirdLoginType;
    }
    public String getThirdUserInfo() {
        return thirdUserInfo;
    }
    public void setThirdUserInfo(String thirdUserInfo) {
        this.thirdUserInfo = thirdUserInfo;
    }
    public int getThirdLoginCount() {
        return thirdLoginCount;
    }
    public void setThirdLoginCount(int thirdLoginCount) {
        this.thirdLoginCount = thirdLoginCount;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime = cTime;
    }
}