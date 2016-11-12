package com.woting.cm.core.broadcast.service;


import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.woting.cm.core.broadcast.persis.po.BroadcastPo;

@Service
public class BroadcastService {
	@Resource(name="defaultDAO")
	private MybatisDAO<BroadcastPo> bcDao;
	
	@PostConstruct
    public void initParam() {
        bcDao.setNamespace("A_BROADCAST");
    }
	
	public List<BroadcastPo> getBroadcastList() {
		List<BroadcastPo> bclist = bcDao.queryForList("getBroadcastList");
		return bclist;
	}
	
	public void insertBroadcastList(List<BroadcastPo> bclist) {
		bcDao.insert("insertList", bclist);
	}
	
	public void updateBroadcastList(List<BroadcastPo> bclist) {
		bcDao.update("updateList", bclist);
	}

    public List<Map<String, Object>> getListByWhereStr(String where) {
        return bcDao.queryForListAutoTranform("getListByWhere", where);
    }
}
