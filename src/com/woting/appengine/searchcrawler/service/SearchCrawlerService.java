package com.woting.appengine.searchcrawler.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class SearchCrawlerService {

	/**
	 * @param searchStr
	 * @param resultType
	 * @param pageType
	 * @param page
	 * @param pageSize 默认为10
	 * @return
	 */
	public Map<String, Object> searchCrawler(String searchStr, int resultType, int pageType, int page, int pageSize, MobileKey mk) {
		Map<String, Object> map = new HashMap<String, Object>();
		List<Map<String, Object>> list = SearchUtils.getListPage(searchStr, page, pageSize);
		if (list==null) {
			long a = System.currentTimeMillis(), num;
			if ( SearchUtils.getListNum(searchStr)== 0) {
				SearchUtils.searchContent(searchStr, mk);
				System.out.println("开启搜索");
				while (true) {
					try {
						Thread.sleep(50);
						if ((num = SearchUtils.getListNum(searchStr)) > 0)
						    if (num >= pageSize) break;
					    if ((System.currentTimeMillis() - a) > 3 * 1000) break;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		list = SearchUtils.getListPage(searchStr, page, pageSize);
		if(page==1) {
			for (Map<String, Object> m : list) {
				if(m.get("MediaType").equals("TTS")) {
					Map<String, Object> m2 = SearchUtils.getNewsInfo(m.get("ContentId")+"");
					if(m2!=null&&m2.size()>0) {
						m.put("ContentURI", m2.get("ContentURI"));
						break;
					}   
				}
			}
		}
		if(list!=null&&list.size()>0){
			map.put("AllCount", list.size());
		    map.put("List", list);
	        map.put("ResultType", resultType);
		}
		return map;
	}
}
