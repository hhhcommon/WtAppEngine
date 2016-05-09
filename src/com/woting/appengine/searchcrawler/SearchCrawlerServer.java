package com.woting.appengine.searchcrawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchCrawlerServer extends Thread {
	public static Map<String, List<Map<String, Object>>> resultmap = new HashMap<String,List<Map<String, Object>>>();
	
	public List<Map<String, Object>> getContentList(String content){
		List<Map<String, Object>> resultlist = (List<Map<String, Object>>) resultmap.get(content);
		if(resultlist==null) {
			addContent(content);
			// 开启后台搜索 
		}
		return resultlist;
	}
	
	public static void addContent(String content){
		resultmap.put(content, new ArrayList<Map<String, Object>>());
	}
	
	public static void addContentInfo(String content,Object obj){
		List<Map<String, Object>> resultlist = (List<Map<String, Object>>) resultmap.get(content);
		resultlist.add((Map<String, Object>) obj);
	}

}
