package com.woting.appengine.searchcrawler.service;

import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.appengine.content.service.ContentService;
import com.woting.appengine.mobile.MobileUDKey;
import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class LocalSearch extends Thread {

	private String searchStr;
    private MobileUDKey mUdk;

	public LocalSearch(String searchStr, MobileUDKey mUdk) {
		this.searchStr=searchStr;
		this.mUdk=mUdk;
	}

	private Map<String, Object> localService() {
		ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
		if (WebApplicationContextUtils.getWebApplicationContext(sc) != null) {
			ContentService contentService=(ContentService) WebApplicationContextUtils.getWebApplicationContext(sc).getBean("contentService");
			return contentService.searchAll(searchStr, 0, 0, mUdk);
		} else {
			return null;
		}
	}

	@Override
	public void run() {
		System.out.println("本地搜索开始");
		Map<String, Object> map=localService();
		try {
			if (map.get("ReturnType").equals("1001")) {
				List<Map<String, Object>> list=(List<Map<String, Object>>) map.get("List");
				for (Map<String, Object> m : list) {
					SearchUtils.addListInfo(searchStr, m);
				}
			}
		} catch (Exception e) {
			System.out.println("本地搜索异常");
		}
		finally {
			SearchUtils.updateSearchFinish(searchStr);
			System.out.println("本地搜索完成");
		}
	}
}
