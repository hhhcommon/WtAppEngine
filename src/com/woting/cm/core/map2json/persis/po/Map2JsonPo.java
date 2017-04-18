package com.woting.cm.core.map2json.persis.po;

import java.util.Map;

import com.spiritdata.framework.util.JsonUtils;

public class Map2JsonPo {

	private String replaceStr;
	private Map<String, Object> replaceMap;
	
	public String getReplaceStr() {
		return replaceStr;
	}
	public void setReplaceStr(String replaceStr) {
		this.replaceStr = replaceStr;
	}
	public Map<String, Object> getReplaceMap() {
		replaceMap = (Map<String, Object>) JsonUtils.jsonToObj(replaceStr, Map.class);
		return replaceMap;
	}
	public void setReplaceMap(Map<String, Object> replaceMap) {
		this.replaceMap = replaceMap;
	}
}
