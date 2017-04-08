package com.woting.cm.cachedb.playcountdb.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class PlayCountDBPo extends BaseObject {

	private static final long serialVersionUID = -6463532273546692783L;

	private String id;
	private String resTableName;
	private String resId;
	private long playCount;
	private Timestamp cTime;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getResTableName() {
		return resTableName;
	}
	public void setResTableName(String resTableName) {
		this.resTableName = resTableName;
	}
	public String getResId() {
		return resId;
	}
	public void setResId(String resId) {
		this.resId = resId;
	}
	public long getPlayCount() {
		return playCount;
	}
	public void setPlayCount(long playCount) {
		this.playCount = playCount;
	}
	public Timestamp getcTime() {
		return cTime;
	}
	public void setcTime(Timestamp cTime) {
		this.cTime = cTime;
	}
}
