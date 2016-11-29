package com.woting.cm.core.broadcast.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.woting.cm.core.broadcast.persis.po.BCProgrammePo;

@Service
public class BcProgrammeService {

	@Resource(name="defaultDAO")
	private MybatisDAO<BCProgrammePo> bcProDao;
	
	@PostConstruct
    public void initParam() {
        bcProDao.setNamespace("A_BCPROGRAMME");
    }
	
	public List<BCProgrammePo> getBCProgrammeListByTime(String bcId, int weekDay, long time) {
		Map<String, Object> m = new HashMap<>();
		m.put("bcId", bcId);
		m.put("weekDay", weekDay);
		m.put("sort", "0");
		m.put("wheresql", "cTime < '"+new Timestamp(time)+"'");
		m.put("orderByClause", "beginTime");
		List<BCProgrammePo> bcps = bcProDao.queryForList("getList", m);
		if (bcps!=null && bcps.size()>0) {
			return bcps;
		}
		return null;
	}
	
	public void insertBCProgrammeList(List<BCProgrammePo> bcProlist) {
		bcProDao.insert("insertList", bcProlist);
	}
	
	public void deleteById(String id) {
		bcProDao.delete("deleteById", id);
	}
}
