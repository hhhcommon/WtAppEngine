package com.woting.appengine.searchcrawler.service;

import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.appengine.content.service.ContentService;
import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class LocalSearch extends Thread {

	private static String searchStr;
	private static int resultType;
	private static int pageType;

	public static void begin(String searchStr, int resultType, int pageType) {
		LocalSearch.searchStr = searchStr;
		LocalSearch.resultType = resultType;
		LocalSearch.pageType = pageType;
		new LocalSearch().start();
	}

	public Map<String, Object> localService(String searchStr, int resultType, int pageType) {
		ServletContext sc = (ServletContext) SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent();
		if (WebApplicationContextUtils.getWebApplicationContext(sc) != null) {
			ContentService contentService = (ContentService) WebApplicationContextUtils.getWebApplicationContext(sc).getBean("contentService");
			return contentService.searchAll(searchStr, resultType, pageType);
		} else {
			return null;
		}
	}

	@Override
	public void run() {
		Map<String, Object> map = localService(searchStr, resultType, pageType);
		try {
			if (map.get("ReturnType").equals("1001")) {
				List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("List");
				for (Map<String, Object> m : list) {
					SearchUtils.addListInfo(searchStr, m);
				}
			}
		} catch (Exception e) {}
		finally {
			SearchUtils.updateSearchFinish(searchStr);
			System.out.println("本地搜索完成");
		}
	}
}
