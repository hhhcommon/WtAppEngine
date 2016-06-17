package com.woting.passport.UGA.persistence.pojo;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.spiritdata.framework.UGA.UgaUser;
import com.spiritdata.framework.util.StringUtils;

public class UserPo extends UgaUser {
    private static final long serialVersionUID = 400373602903981461L;

    private String userNum; //用户号，用于公开的号码
    private String mainPhoneNum; //用户主手机号码，用户可能有多个手机号码
    private String mailAddress; //用户邮箱
    private int userNature; //用户性质： 1=自然人用户;2=机构用户
    private int userType; //用户分类：1=用户;2=外围人员;3=管理员
    private int userState;//用户状态，0~2
    private String portraitBig;//用户头像大
    private String portraitMini;//用户头像小
    private String age; //用户年龄
    private String birthday; //生日
    private String sex; //性别
    private String homepage; //个人主页
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
    public int getUserNature() {
		return userNature;
	}
	public void setUserNature(int userNature) {
		this.userNature = userNature;
	}
	public String getAge() {
		return age;
	}
	public void setAge(String age) {
		this.age = age;
	}
	public String getBirthday() {
		return birthday;
	}
	public void setBirthday(String birthday) {
		this.birthday = birthday;
	}
	public String getSex() {
		return sex;
	}
	public void setSex(String sex) {
		this.sex = sex;
	}
	public String getHomepage() {
		return homepage;
	}
	public void setHomepage(String homepage) {
		this.homepage = homepage;
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

    public Map<String, Object> toHashMap4Mobile() {
        Map<String, Object> retM = new HashMap<String, Object>();
        if (!StringUtils.isNullOrEmptyOrSpace(this.userId)) retM.put("UserId", this.userId);
        if (!StringUtils.isNullOrEmptyOrSpace(this.userName)) retM.put("RealName", this.userName);
        if (!StringUtils.isNullOrEmptyOrSpace(this.userNum)) retM.put("UserNum", this.userNum);
        if (!StringUtils.isNullOrEmptyOrSpace(this.loginName)) retM.put("UserName", this.loginName);
        if (!StringUtils.isNullOrEmptyOrSpace(this.mainPhoneNum)) retM.put("PhoneNum", this.mainPhoneNum);
        if (!StringUtils.isNullOrEmptyOrSpace(this.mailAddress)) retM.put("Email", this.mailAddress);
        if (!StringUtils.isNullOrEmptyOrSpace(this.portraitBig)) retM.put("PortraitBig", this.portraitBig);
        if (!StringUtils.isNullOrEmptyOrSpace(this.portraitMini)) retM.put("PortraitMini", this.portraitMini);
        if (!StringUtils.isNullOrEmptyOrSpace(this.age)) retM.put("Age", this.age);
        if (!StringUtils.isNullOrEmptyOrSpace(this.birthday)) retM.put("Birthday", this.birthday);
        if (!StringUtils.isNullOrEmptyOrSpace(this.sex)) retM.put("Sex", this.sex);
        if (!StringUtils.isNullOrEmptyOrSpace(this.homepage)) retM.put("HomePage", this.homepage);
        if (!StringUtils.isNullOrEmptyOrSpace(this.descn)) retM.put("Descn", this.descn);
        return retM;
    }
}