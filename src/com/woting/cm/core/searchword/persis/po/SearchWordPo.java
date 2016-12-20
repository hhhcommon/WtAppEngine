package com.woting.cm.core.searchword.persis.po;

import com.spiritdata.framework.core.model.BaseObject;

public class SearchWordPo extends BaseObject {

	private static final long serialVersionUID = -7444818736244027208L;
	private String id;
	private String word;
	private String deviceId;
	private String userId;
	private String pcdType;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getPcdType() {
		return pcdType;
	}
	public void setPcdType(String pcdType) {
		this.pcdType = pcdType;
	}
}
