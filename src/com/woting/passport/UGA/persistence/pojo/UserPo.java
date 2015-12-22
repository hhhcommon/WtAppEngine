package com.woting.passport.UGA.persistence.pojo;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.spiritdata.framework.UGA.UgaUser;
import com.spiritdata.framework.util.StringUtils;

public class UserPo extends UgaUser {
    private static final long serialVersionUID = 400373602903981461L;

    private String mainPhoneNum; //用户主手机号码，用户可能有多个手机号码
    private String mailAddress; //用户邮箱
    private int userType; //用户分类：1=普通用户;2=编辑用户
    private int userState;//用户状态，0~2
    private String protraitBig;//用户头像大
    private String protraitMini;//用户头像小
    private String innerPhoneNum; //内部号码
    private String descn; //用户描述
    private Timestamp CTime; //记录创建时间
    private Timestamp lmTime; //最后修改时间:last modify time

    public String getMainPhoneNum() {
        return mainPhoneNum;
    }
    public void setMainPhoneNum(String mainPhoneNum) {
        this.mainPhoneNum = mainPhoneNum;
    }
    public String getMailAddress() {
        return mailAddress;
    }
    public void setMailAddress(String mailAddress) {
        this.mailAddress = mailAddress;
    }
    public int getUserType() {
        return userType;
    }
    public void setUserType(int userType) {
        this.userType = userType;
    }
    public int getUserState() {
        return userState;
    }
    public void setUserState(int userState) {
        this.userState = userState;
    }
    public String getProtraitBig() {
        return protraitBig;
    }
    public void setProtraitBig(String protraitBig) {
        this.protraitBig = protraitBig;
    }
    public String getProtraitMini() {
        return protraitMini;
    }
    public void setProtraitMini(String protraitMini) {
        this.protraitMini = protraitMini;
    }
    public String getDescn() {
        return descn;
    }
    public void setDescn(String descn) {
        this.descn = descn;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime = cTime;
    }
    public Timestamp getLmTime() {
        return lmTime;
    }
    public void setLmTime(Timestamp lmTime) {
        this.lmTime = lmTime;
    }
    public String getInnerPhoneNum() {
        if (StringUtils.isNullOrEmptyOrSpace(this.innerPhoneNum)) return "3000";
        return innerPhoneNum;
    }
    public void setInnerPhoneNum(String innerPhoneNum) {
        this.innerPhoneNum = innerPhoneNum;
    }

    public Map<String, Object> toHashMap4Mobile() {
        Map<String, Object> retM = new HashMap<String, Object>();
        if (!StringUtils.isNullOrEmptyOrSpace(this.userId)) retM.put("UserId", this.userId);
        if (!StringUtils.isNullOrEmptyOrSpace(this.userName)) retM.put("RealName", this.userName);
        if (!StringUtils.isNullOrEmptyOrSpace(this.loginName)) retM.put("UserName", this.loginName);
        if (!StringUtils.isNullOrEmptyOrSpace(this.mainPhoneNum)) retM.put("PhoneNum", this.mainPhoneNum);
        if (!StringUtils.isNullOrEmptyOrSpace(this.mailAddress)) retM.put("Email", this.mailAddress);
        if (!StringUtils.isNullOrEmptyOrSpace(this.descn)) retM.put("Descript", this.descn);
        //if (!StringUtils.isNullOrEmptyOrSpace(this.innerPhoneNum)) retM.put("InnerPhoneNum", this.innerPhoneNum);
        retM.put("InnerPhoneNum", this.getInnerPhoneNum());
        return retM;
    }
}