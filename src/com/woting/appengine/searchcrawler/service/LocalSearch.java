package com.woting.appengine.searchcrawler.service;

import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.appengine.content.service.ContentService;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class LocalSearch extends Thread {

	private static String searchStr;
	private static int resultType;
    private static int pageType;
    private static MobileKey mk;

	public static void begin(String searchStr, int resultType, int pageType, MobileKey mk) {
		LocalSearch.searchStr=searchStr;
		LocalSearch.resultType=resultType;
        LocalSearch.pageType=pageType;
        LocalSearch.mk=mk;
		
		new LocalSearch().start();
	}

	private Map<String, Object> localService() {
		ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
		if (WebApplicationContextUtils.getWebApplicationContext(sc) != null) {
			ContentService contentService=(ContentService) WebApplicationContextUtils.getWebApplicationContext(sc).getBean("contentService");
			return contentService.searchAll(searchStr, resultType, pageType, mk);
		} else {
			return null;
		}
	}

	@Override
	public void run() {
		Map<String, Object> map=localService();
		try {
			if (map.get("ReturnType").equals("1001")) {
				List<Map<String, Object>> list=(List<Map<String, Object>>) map.get("List");
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
