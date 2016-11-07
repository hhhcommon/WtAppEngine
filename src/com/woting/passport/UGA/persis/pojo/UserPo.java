package com.woting.passport.UGA.persis.pojo;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.spiritdata.framework.UGA.UgaUser;
import com.spiritdata.framework.util.StringUtils;

public class UserPo extends UgaUser {
    private static final long serialVersionUID = 400373602903981461L;

    private String nickName; //昵称
    private String userNum; //用户号，用于公开的号码
    private String userSign; //用户签名
    private Timestamp birthday; //生日
    private String starSign; //星座
    private String homePage; //用户主页
    private String mainPhoneNum; //用户主手机号码，用户可能有多个手机号码
    private String mailAddress; //用户邮箱
    private int userType; //用户分类：1=普通用户;2=编辑用户
    private int userClass; //用户分类：1=普通用户;2=编辑用户
    private int userState;//用户状态，0~2
    private String portraitBig;//用户头像大
    private String portraitMini;//用户头像小
    private String homepage; //用户主页
    private String descn; //用户描述
    private Timestamp CTime; //记录创建时间
    private Timestamp lmTime; //最后修改时间:last modify time

    public String getUserNum() {
        return userNum;
    }
    public void setUserNum(String userNum) {
        this.userNum = userNum;
    }
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
    public int getUserClass() {
        return userClass;
    }
    public void setUserClass(int userClass) {
        this.userClass = userClass;
    }
    public int getUserState() {
        return userState;
    }
    public void setUserState(int userState) {
        this.userState = userState;
    }
    public String getPortraitBig() {
        return portraitBig;
    }
    public void setPortraitBig(String portraitBig) {
        this.portraitBig = portraitBig;
    }
    public String getPortraitMini() {
        return portraitMini;
    }
    public void setPortraitMini(String portraitMini) {
        this.portraitMini = portraitMini;
    }
    public String getDescn() {
        return descn;
    }
    public void setDescn(String descn) {
        this.descn = descn;
    }
    public String getHomepage() {
        return homepage;
    }
    public void setHomepage(String homepage) {
        this.homepage = homepage;
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
    public String getNickName() {
        return nickName;
    }
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
    public String getUserSign() {
        return userSign;
    }
    public void setUserSign(String userSign) {
        this.userSign = userSign;
    }
    public Timestamp getBirthday() {
        return birthday;
    }
    public void setBirthday(Timestamp birthday) {
        this.birthday = birthday;
    }
    public String getStarSign() {
        return starSign;
    }
    public void setStarSign(String starSign) {
        this.starSign = starSign;
    }
    public String getHomePage() {
        return homePage;
    }
    public void setHomePage(String homePage) {
        this.homePage = homePage;
    }

    public Map<String, Object> toHashMap4Mobile() {
        Map<String, Object> retM = new HashMap<String, Object>();
        if (!StringUtils.isNullOrEmptyOrSpace(this.userId)) retM.put("UserId", this.userId);
        if (!StringUtils.isNullOrEmptyOrSpace(this.userName)) retM.put("RealName", this.userName);
        if (!StringUtils.isNullOrEmptyOrSpace(this.userNum)) retM.put("UserNum", this.userNum);
        if (!StringUtils.isNullOrEmptyOrSpace(this.loginName)) retM.put("UserName", this.loginName);
        if (!StringUtils.isNullOrEmptyOrSpace(this.mainPhoneNum)) retM.put("PhoneNum", this.mainPhoneNum);
        if (!StringUtils.isNullOrEmptyOrSpace(this.mailAddress)) retM.put("Email", this.mailAddress);
        if (!StringUtils.isNullOrEmptyOrSpace(this.descn)) retM.put("Descn", this.descn);
        if (!StringUtils.isNullOrEmptyOrSpace(this.portraitBig)) retM.put("PortraitBig", this.portraitBig);
        if (!StringUtils.isNullOrEmptyOrSpace(this.portraitMini)) retM.put("PortraitMini", this.portraitMini);
        return retM;
    }

    /**
     * 转换为详细信息
     * @return 详细信息内容，形式如下：
     * 
     */
    public Map<String, Object> toDetailInfo() {
        Map<String, Object> retM = new HashMap<String, Object>();
        if (!StringUtils.isNullOrEmptyOrSpace(this.userId)) retM.put("UserId", this.userId);
        if (!StringUtils.isNullOrEmptyOrSpace(this.userNum)) retM.put("UserNum", this.userNum);
        if (!StringUtils.isNullOrEmptyOrSpace(this.loginName)) retM.put("UserName", this.loginName);
        if (!StringUtils.isNullOrEmptyOrSpace(this.userName)) retM.put("RealName", this.userName);
        if (!StringUtils.isNullOrEmptyOrSpace(this.nickName)) retM.put("NickName", this.nickName);
        if (!StringUtils.isNullOrEmptyOrSpace(this.userSign)) retM.put("UserSign", this.userSign);
        //Sex
        //Region
        if (this.birthday!=null) retM.put("Birthday", this.birthday.getTime());
        //Age
        if (!StringUtils.isNullOrEmptyOrSpace(this.starSign)) retM.put("StarSign", this.starSign);
        if (!StringUtils.isNullOrEmptyOrSpace(this.mainPhoneNum)) retM.put("PhoneNum", this.mainPhoneNum);
        if (!StringUtils.isNullOrEmptyOrSpace(this.mailAddress)) retM.put("Email", this.mailAddress);
        if (!StringUtils.isNullOrEmptyOrSpace(this.portraitBig)) retM.put("PortraitBig", this.portraitBig);
        if (!StringUtils.isNullOrEmptyOrSpace(this.portraitMini)) retM.put("PortraitMini", this.portraitMini);
        if (!StringUtils.isNullOrEmptyOrSpace(this.descn)) retM.put("Descn", this.descn);
        return retM;
    }
}