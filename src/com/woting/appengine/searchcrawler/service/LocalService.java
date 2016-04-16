package com.woting.appengine.searchcrawler.service;

import java.util.Map;
import java.util.concurrent.Callable;

import javax.servlet.ServletContext;

import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.appengine.content.service.ContentService;

/**
 * 对contentservice进行线程调用
 * @author wbq
 *
 */
public class LocalService implements Callable<Map<String, Object>> {

	private String searchStr; 
	private int resultType;
	private int pageType;
	
	public LocalService() {
		// TODO Auto-generated constructor stub
	}
	
	public LocalService(String searchStr, int resultType, int pageType) {
		this.searchStr = searchStr;
		this.resultType = resultType;
		this.pageType = pageType;
	}
	
	public Map<String, Object> localService(String searchStr, int resultType, int pageType){
		ServletContext sc = (ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent();
		if(WebApplicationContextUtils.getWebApplicationContext(sc)!=null){
			ContentService contentService = (ContentService) WebApplicationContextUtils.getWebApplicationContext(sc).getBean("contentService");
			return contentService.searchAll(searchStr, resultType, pageType);
		}else {
			return null;
		}
	}

	@Override
	public Map<String, Object> call() throws Exception {
		return localService(searchStr, resultType, pageType);
	}
}
