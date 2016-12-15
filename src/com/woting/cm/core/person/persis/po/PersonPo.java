package com.woting.cm.core.person.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class PersonPo extends BaseObject {
	private static final long serialVersionUID = -5920073354877155250L;
	private String id;
	private String pName;
	private String pSource;
	private String pSrcId;
	private String age;
	private String birthday;
	private String constellation;
	private String descn;
	private String phoneNum;
	private String email;
	private String pSrcHomePage;
	private String portrait;
	private int isVerified;
	private Timestamp cTime;
	private Timestamp lmTime;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getpName() {
		return pName;
	}
	public void setpName(String pName) {
		this.pName = pName;
	}
	public String getpSource() {
		return pSource;
	}
	public void setpSource(String pSource) {
		this.pSource = pSource;
	}
	public String getpSrcId() {
		return pSrcId;
	}
	public void setpSrcId(String pSrcId) {
		this.pSrcId = pSrcId;
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
	public String getConstellation() {
		return constellation;
	}
	public void setConstellation(String constellation) {
		this.constellation = constellation;
	}
	public String getDescn() {
		return descn;
	}
	public void setDescn(String descn) {
		this.descn = descn;
	}
	public String getPhoneNum() {
		return phoneNum;
	}
	public void setPhoneNum(String phoneNum) {
		this.phoneNum = phoneNum;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getpSrcHomePage() {
		return pSrcHomePage;
	}
	public void setpSrcHomePage(String pSrcHomePage) {
		this.pSrcHomePage = pSrcHomePage;
	}
	public String getPortrait() {
		return portrait;
	}
	public void setPortrait(String portrait) {
		this.portrait = portrait;
	}
	public int getIsVerified() {
		return isVerified;
	}
	public void setIsVerified(int isVerified) {
		this.isVerified = isVerified;
	}
	public Timestamp getcTime() {
		return cTime;
	}
	public void setcTime(Timestamp cTime) {
		this.cTime = cTime;
	}
	public Timestamp getLmTime() {
		return lmTime;
	}
	public void setLmTime(Timestamp lmTime) {
		this.lmTime = lmTime;
	}
}
