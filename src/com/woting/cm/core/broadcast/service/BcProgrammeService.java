package com.woting.cm.core.broadcast.service;

import java.util.List;
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
        bcProDao.setNamespace("A_PROGRAMME");
    }
	
	public void insertBCProgrammeList(List<BCProgrammePo> bcProlist) {
		bcProDao.insert("insertList", bcProlist);
	}
	
	public void deleteById(String id) {
		bcProDao.delete("deleteById", id);
	}
}
