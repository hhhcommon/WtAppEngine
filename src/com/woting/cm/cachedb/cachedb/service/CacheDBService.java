package com.woting.cm.cachedb.cachedb.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.JsonUtils;
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
	
	/**
	 * 获得节目列表信息（排序默认List排序）
	 * @param maIds
	 * @return
	 */
	public List<Map<String, Object>> getCacheDBAudios(List<String> maIds) {
		if(maIds!=null && maIds.size()>0) {
			String sql = "SELECT cdb.id,cdb.`value`,pdb.playCount FROM wt_CacheDB cdb"
					+ " LEFT JOIN wt_PlayCountDB pdb "
					+ " ON pdb.id = CONCAT('AUDIO_',cdb.resId,'_PLAYCOUNT')";
			String orIdStr = "";
			String orderByStr = "";
			for (String id : maIds) {
				orIdStr += " or cdb.id = '"+id+"_INFO'";
				orderByStr += ",'"+id+"_INFO'";
			}
			orIdStr = orIdStr.substring(3);
			sql += " where "+orIdStr;
			orderByStr  = orderByStr.substring(1);
			orderByStr = " ORDER BY FIELD(cdb.id, "+orderByStr+")";
			sql += orderByStr;
			Map<String, Object> m = new HashMap<>();
			m.put("SQLClauseBy", sql);
			List<Map<String, Object>> cdbmaps = cacheDBDao.queryForListAutoTranform("getListBySQL", m);
			if (cdbmaps!=null && cdbmaps.size()>0) {
				List<Map<String, Object>> retLs = new ArrayList<>();
				for (Map<String, Object> map : cdbmaps) {
					try {
						String retStr = map.get("value").toString();
						long playNum = Long.valueOf(map.get("playCount").toString());
						if (retStr!=null && retStr.length()>0) {
							map = (Map<String, Object>) JsonUtils.jsonToObj(retStr, Map.class);
							map.put("PlayCount", playNum);
							retLs.add(map);
						}
					} catch (Exception e) {
						continue;
					}
				}
				if (retLs!=null && retLs.size()>0) {
					return retLs;
				}
			}
		}
		return null;
	}
}
