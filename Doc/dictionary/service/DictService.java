package com.woting.dictionary.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.woting.dictionary.model.DictDetail;
import com.woting.dictionary.model.DictMaster;
import com.woting.dictionary.persistence.pojo.DictDetailPo;
import com.woting.dictionary.persistence.pojo.DictMasterPo;

/**
 * 字典信息服务类，主要功能是与关系型数据库交互，类似DAO。
 * 与Dao不同的是，此服务中的方法是按照基础业务逻辑来组合的，即对DAO功能的初步组合。
 * 主要涉及量个数据库表：PLAT_DICTM/PLAT_DICTD
 * 
 * @author wh
 */
public class DictService {
    @Resource(name="defaultDAO")
    private MybatisDAO<DictMasterPo> dictMDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<DictDetailPo> dictDDao;

    @PostConstruct
    public void initParam() {
        dictMDao.setNamespace("dMaster");
        dictDDao.setNamespace("dDetail");
    }

    /**
     * 得到系统字典组列表
     * @return 字典组列表
     */
    public List<DictMaster> getDictMListSys() {
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("ownerId", "0");
        param.put("ownerType", "0");
        param.put("sortByClause", "id");
        List<DictMasterPo> _l = dictMDao.queryForList(param);
        if (_l==null||_l.size()==0) return null;
        List<DictMaster> ret = new ArrayList<DictMaster>();
        for (DictMasterPo dmp: _l) {
            DictMaster dm = new DictMaster();
            dm.buildFromPo(dmp);
            ret.add(dm);
        }
        return ret.size()==0?null:ret;
    }

    /**
     * 得到系统字典项列表
     * @return 字典项列表
     */
    public List<DictDetail> getDictDListSys() {
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("ownerId", "0");
        param.put("ownerType", "0");
        List<DictDetailPo> _l = dictDDao.queryForList("getListByOnwer", param);
        if (_l==null||_l.size()==0) return null;
        List<DictDetail> ret = new ArrayList<DictDetail>();
        for (DictDetailPo ddp: _l) {
            DictDetail dd = new DictDetail();
            dd.buildFromPo(ddp);
            ret.add(dd);
        }
        return ret.size()==0?null:ret;
    }
}