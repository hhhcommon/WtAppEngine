package com.woting.cm.core.person.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.woting.cm.core.person.persis.po.PersonPo;
import com.woting.cm.core.person.persis.po.PersonRefPo;

public class PersonService {
	@Resource(name = "defaultDAO")
	private MybatisDAO<PersonPo> personDao;
	@Resource(name = "defaultDAO")
	private MybatisDAO<PersonRefPo> personRefDao;
	
	@PostConstruct
	public void initParam() {
	    personDao.setNamespace("A_PERSON");
	    personRefDao.setNamespace("A_PERSONREF");
	}

	public PersonRefPo getPersonRefBy(String resTableName, String resId) {
		Map<String, Object> m = new HashMap<>();
		m.put("resTableName", resTableName);
		m.put("resId", resId);
		List<PersonRefPo> pfs = personRefDao.queryForList("getListBy", m);
		if (pfs!=null && pfs.size()>0) {
			return pfs.get(0);
		}
		return null;
	}
	
	public void insertPerson(List<PersonPo> ps) {
		Map<String, Object> m = new HashMap<>();
		m.put("list", ps);
		personDao.insert("insertList", m);
	}
	
	public void insertPerson(PersonPo po) {
		List<PersonPo> pos = new ArrayList<>();
		pos.add(po);
		Map<String, Object> m = new HashMap<>();
		m.put("list", pos);
		personDao.insert("insertList", m);
	}
	
	public void insertPersonRef(List<PersonRefPo> pfs) {
		Map<String, Object> m = new HashMap<>();
		m.put("list", pfs);
		personRefDao.insert("insertList", m);
	}
	
	public void insertPersonRef(PersonRefPo pf) {
		List<PersonRefPo> pfs = new ArrayList<>();
		pfs.add(pf);
		Map<String, Object> m = new HashMap<>();
		m.put("list", pfs);
		personRefDao.insert("insertList", m);
	}
	
}
