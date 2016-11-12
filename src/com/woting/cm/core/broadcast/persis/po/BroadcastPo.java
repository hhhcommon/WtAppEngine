package com.woting.cm.core.broadcast.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class BroadcastPo extends BaseObject {

	private static final long serialVersionUID = -2205931022881432586L;
	private String id;
	private String bcTitle;
	private int bcPubType;
	private String bcPubId;
	private String bcPublisher;
	private String bcImg;
	private String bcURL;
	private String descn;
	private int pubCount;
	private Timestamp cTime;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getBcTitle() {
		return bcTitle;
	}
	public void setBcTitle(String bcTitle) {
		this.bcTitle = bcTitle;
	}
	public int getBcPubType() {
		return bcPubType;
	}
	public void setBcPubType(int bcPubType) {
		this.bcPubType = bcPubType;
	}
	public String getBcPubId() {
		return bcPubId;
	}
	public void setBcPubId(String bcPubId) {
		this.bcPubId = bcPubId;
	}
	public String getBcPublisher() {
		return bcPublisher;
	}
	public void setBcPublisher(String bcPublisher) {
		this.bcPublisher = bcPublisher;
	}
	public String getBcImg() {
		return bcImg;
	}
	public void setBcImg(String bcImg) {
		this.bcImg = bcImg;
	}
	public String getBcURL() {
		return bcURL;
	}
	public void setBcURL(String bcURL) {
		this.bcURL = bcURL;
	}
	public String getDescn() {
		return descn;
	}
	public void setDescn(String descn) {
		this.descn = descn;
	}
	public int getPubCount() {
		return pubCount;
	}
	public void setPubCount(int pubCount) {
		this.pubCount = pubCount;
	}
	public Timestamp getcTime() {
		return cTime;
	}
	public void setcTime(Timestamp cTime) {
		this.cTime = cTime;
	}
}
