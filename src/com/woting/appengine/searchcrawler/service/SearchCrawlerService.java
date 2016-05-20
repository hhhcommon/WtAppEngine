package com.woting.appengine.searchcrawler.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class SearchCrawlerService {

	/**
	 * 
	 * @param searchStr
	 * @param resultType
	 * @param pageType
	 * @param page
	 * @param pageSize
	 *            默认为10
	 * @return
	 */
	public Map<String, Object> searchCrawler(String searchStr, int resultType, int pageType, int page, int pageSize) {
		Map<String, Object> map = new HashMap<String, Object>();
		List<Map<String, Object>> list = SearchUtils.getListPage(searchStr, page, pageSize);
		if (list==null) {
			long a = System.currentTimeMillis(), b, num;
			if (SearchUtils.getListNum(searchStr) == 0) {
				SearchUtils.searchContent(searchStr);
				System.out.println("开启搜索");
				while (true) {
					try {
						Thread.sleep(50);
						if ((num = SearchUtils.getListNum(searchStr)) > 0) {
						if (num >= pageSize) break;
					    }
					    b = System.currentTimeMillis() - a;
					    if (b > 3 * 1000) break;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		list = SearchUtils.getListPage(searchStr, page, pageSize);
		if(list!=null&&list.size()>0){
			map.put("AllCount", list.size());
		    map.put("List", list);
	        map.put("ResultType", resultType);
		}
		return map;
	}
}
