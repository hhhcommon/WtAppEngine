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
		SearchUtils.judgeLock(searchStr);
		List<String> liststr = SearchUtils.getListPage(searchStr, page, pageSize);
		if (liststr == null) {
			long a = System.currentTimeMillis(), b, num;
			SearchUtils.searchContent(searchStr);
			while (true) {
				if ((num = SearchUtils.getListNum(searchStr)) > 0) {
					if (num >= 10) {
						SearchUtils.unlockRedisKey(searchStr);
						break;
					}
				}
				b = System.currentTimeMillis() - a;
				if (b > 2 * 1000) {
					SearchUtils.unlockRedisKey(searchStr);
					break;
				}
			}
		}
		liststr = SearchUtils.getListPage(searchStr, page, pageSize);
		map.put("AllCount", liststr.size());
		map.put("List", liststr);
		map.put("ResultType", resultType);
		return map;
	}
}
