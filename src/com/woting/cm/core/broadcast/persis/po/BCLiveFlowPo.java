package com.woting.cm.core.broadcast.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class BCLiveFlowPo extends BaseObject {

	private static final long serialVersionUID = -7423874552168829872L;
	private String id;
	private String bcId;
	private int bcSrcType;
	private String bcSrcId;
	private String bcSource;
	private String flowURI;
	private String bcSrcChannelId;
	private int isMain;
	private String descn;
	private Timestamp cTime;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Timestamp getcTime() {
		return cTime;
	}
	public void setcTime(Timestamp cTime) {
		this.cTime = cTime;
	}
	public String getBcSrcChannelId() {
		return bcSrcChannelId;
	}
	public void setBcSrcChannelId(String bcSrcChannelId) {
		this.bcSrcChannelId = bcSrcChannelId;
	}
	public String getBcId() {
		return bcId;
	}
	public void setBcId(String bcId) {
		this.bcId = bcId;
	}
	public int getBcSrcType() {
		return bcSrcType;
	}
	public void setBcSrcType(int bcSrcType) {
		this.bcSrcType = bcSrcType;
	}
	public String getBcSrcId() {
		return bcSrcId;
	}
	public void setBcSrcId(String bcSrcId) {
		this.bcSrcId = bcSrcId;
	}
	public String getBcSource() {
		return bcSource;
	}
	public void setBcSource(String bcSource) {
		this.bcSource = bcSource;
	}
	public String getFlowURI() {
		return flowURI;
	}
	public void setFlowURI(String flowURI) {
		this.flowURI = flowURI;
	}
	public int getIsMain() {
		return isMain;
	}
	public void setIsMain(int isMain) {
		this.isMain = isMain;
	}
	public String getDescn() {
		return descn;
	}
	public void setDescn(String descn) {
		this.descn = descn;
	}
}
