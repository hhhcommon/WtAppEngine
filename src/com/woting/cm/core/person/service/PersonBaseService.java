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

public class PersonBaseService {
	@Resource(name = "defaultDAO")
	private MybatisDAO<PersonPo> personDao;
	@Resource(name = "defaultDAO")
	private MybatisDAO<PersonRefPo> personRefDao;
	
	@PostConstruct
	public void initParam() {
	    personDao.setNamespace("A_PERSON");
	    personRefDao.setNamespace("A_PERSONREF");
	}
	
	public PersonPo getPersonPo(String personId) {
		Map<String, Object> m = new HashMap<>();
		m.put("id", personId);
		List<PersonPo> pos = personDao.queryForList("getList", m);
		if (pos!=null && pos.size()>0) {
			return pos.get(0);
		}
		return null;
	}
	
	public List<PersonRefPo> getPersonRefs(String personId, String resTableName) {
		Map<String, Object> m = new HashMap<>();
		m.put("personId", personId);
		m.put("resTableName", resTableName);
		List<PersonRefPo> personRefPos = personRefDao.queryForList("getListBy", m);
		if (personRefPos!=null && personRefPos.size()>0) {
			return personRefPos;
		}
		return null;
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
