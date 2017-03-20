package com.woting.appengine.searchcrawler.service;

import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.ext.spring.redis.RedisOperService;
import com.woting.appengine.content.service.ContentService;
import com.woting.appengine.searchcrawler.utils.SearchUtils;
import com.woting.passport.mobile.MobileUDKey;

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
	            ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
	            if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
	                JedisConnectionFactory conn=(JedisConnectionFactory)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("connectionFactory123");
	                RedisOperService roService=new RedisOperService(conn);
	                for (Map<String, Object> m : list) {
	                    SearchUtils.addListInfo(searchStr, m, roService);
	                }
	            }
			}
		} catch (Exception e) {
			System.out.println("本地搜索异常");
		}
		finally {
            ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
            if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                JedisConnectionFactory conn=(JedisConnectionFactory)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("connectionFactory123");
                RedisOperService roService=new RedisOperService(conn);
                SearchUtils.updateSearchFinish(searchStr, roService);
            } else {
            	System.out.println("本地搜索完成结束异常");
            }
			System.out.println("本地搜索完成");
		}
	}
}
