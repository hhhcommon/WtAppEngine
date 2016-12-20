package com.woting.cm.core.searchword.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.woting.cm.core.searchword.persis.po.SearchWordPo;

public class SearchWordService {
	@Resource(name="defaultDAO")
    private MybatisDAO<SearchWordPo> searchWordDao;
	
	@PostConstruct
    public void initParam() {
		searchWordDao.setNamespace("A_SEARCHWORD");
	}
	
	public void insertSearchWord(SearchWordPo sw) {
		searchWordDao.insert(sw);
	}
}
