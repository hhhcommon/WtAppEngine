package com.woting.appengine.searchcrawler.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.ext.spring.redis.RedisOperService;
import com.woting.appengine.searchcrawler.utils.SearchUtils;
import com.woting.passport.mobile.MobileUDKey;

public class SearchCrawlerService {

	/**
	 * @param searchStr
	 * @param resultType
	 * @param pageType
	 * @param page
	 * @param pageSize 默认为10
	 * @return
	 */
	public Map<String, Object> searchCrawler(String searchStr, int resultType, int pageType, int page, int pageSize, MobileUDKey mUdk) {
		Map<String, Object> map = new HashMap<String, Object>();
		List<Map<String, Object>> list=null;
        ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
            JedisConnectionFactory conn=(JedisConnectionFactory)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("connectionFactory123");
            RedisOperService roService=new RedisOperService(conn);
            list=SearchUtils.getListPage(searchStr, page, pageSize, roService); // 保存到在redis里key为constr的list里
            if (list==null) {
                long a = System.currentTimeMillis(), num;
                if ( SearchUtils.getListNum(searchStr, roService)== 0) {
                    SearchUtils.searchContent(searchStr, mUdk, roService);
                    System.out.println("开启搜索");
                    while (true) {
                        try {
                            Thread.sleep(50);
                            if ((num = SearchUtils.getListNum(searchStr, roService)) > 0)
                                if (num >= pageSize) break;
                            if ((System.currentTimeMillis() - a) > 3 * 1000) break;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            list = SearchUtils.getListPage(searchStr, page, pageSize, roService);
//            if(page==1) {
//                for (Map<String, Object> m : list) {
//                    if(m.get("MediaType").equals("TTS")) {
//                        Map<String, Object> m2 = SearchUtils.getNewsInfo(m.get("ContentId")+"",roService);
//                        if(m2!=null&&m2.size()>0) {
//                            m.put("ContentURI", m2.get("ContentURI"));
//                            break;
//                        }   
//                    }
//                }
//            }
        }
		if(list!=null&&list.size()>0){
			map.put("AllCount", list.size());
		    map.put("List", list);
	        map.put("ResultType", resultType);
		}
		return map;
	}
}
