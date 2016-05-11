package com.woting.appengine.searchcrawler.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class SearchCrawlerService {

	public Map<String, Object> searchCrawler(String searchStr, int resultType, int pageType) {
		Map<String, Object> map = new HashMap<String, Object>();
		List<String> liststr = SearchUtils.getListInfo(searchStr);
		if (liststr == null) {
		//	threadService.searchWebAndLocal(searchStr, resultType, pageType);
			long a=System.currentTimeMillis();
			KaoLaService.begin(searchStr);
			XiMaLaYaService.begin(searchStr);
			QingTingService.begin(searchStr);
			while(true){
				if((liststr = SearchUtils.getListInfo(searchStr))!=null)
//					a=System.currentTimeMillis()-a;
					if (liststr.size()>2) {
						break;
					}
			}
		}
		map.put("AllCount", liststr.size());
		map.put("List", liststr);
		map.put("ResultType", resultType);
		return map;
	}
}
