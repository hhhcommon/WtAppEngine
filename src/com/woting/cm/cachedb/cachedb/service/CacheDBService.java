package com.woting.cm.cachedb.cachedb.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.woting.cm.cachedb.cachedb.persis.po.CacheDBPo;

public class CacheDBService {
	@Resource(name="defaultDAO_CacheDB")
	private MybatisDAO<CacheDBPo> cacheDBDao;
	
	@PostConstruct
    public void initParam() {
		cacheDBDao.setNamespace("A_CACHEDB");
    }
	
	public CacheDBPo getCacheDBPoById(String id) {
		CacheDBPo cacheDBPo = cacheDBDao.getInfoObject("getInfoById", id);
		if (cacheDBPo!=null) {
			return cacheDBPo;
		}
		return null;
	}
	
	public void insertCacheDBPo(CacheDBPo cacheDBPo) {
		if (cacheDBPo!=null) {
			deleteById(cacheDBPo.getId());
			cacheDBDao.insert(cacheDBPo);
		}
	}
	
	public void deleteById(String id) {
		cacheDBDao.delete("delete", id);
	}
	
	public String getCacheDBInfo(String id) {
		CacheDBPo cacheDBPo = cacheDBDao.getInfoObject("getInfoById", id);
		if (cacheDBPo!=null) {
			return cacheDBPo.getValue();
		}
		return null;
	}
}
