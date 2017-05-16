package com.woting.cm.cachedb.playcountdb.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.woting.cm.cachedb.playcountdb.persis.po.PlayCountDBPo;

public class PlayCountDBService {
	@Resource(name="defaultDAO_CacheDB")
	private MybatisDAO<PlayCountDBPo> playCountDBDao;
	
	@PostConstruct
    public void initParam() {
		playCountDBDao.setNamespace("A_PLAYCOUNTDB");
    }
	
	public PlayCountDBPo getPlayCountDBPoById(String id) {
		PlayCountDBPo playCountDBPo = playCountDBDao.getInfoObject("getInfoById", id);
		if (playCountDBPo!=null) {
			return playCountDBPo;
		}
		return null;
	}
	
	public void insertPlayCountDBPo(PlayCountDBPo playCountDBPo) {
		if (playCountDBPo!=null) {
			deleteById(playCountDBPo.getId());
			playCountDBDao.insert(playCountDBPo);
		}
	}
	
	public void deleteById(String id) {
		playCountDBDao.delete("delete", id);
	}
	
	public long getPlayCountNum(String id) {
		PlayCountDBPo playCountDBPo = playCountDBDao.getInfoObject("getInfoById", id);
		if (playCountDBPo!=null) {
			return playCountDBPo.getPlayCount();
		}
		return 0;
	}

    public List<PlayCountDBPo> getPlayCountList(List<String> ids) {
        if (ids==null||ids.isEmpty()) return null;
        //return playCountDBDao.queryForList("getList", ids);
        Map<String, Object> p=new HashMap<String, Object>();
        p.put("ids", ids);
        return playCountDBDao.queryForList("getListById", p);
    }
}