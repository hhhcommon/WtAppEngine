package com.woting.appengine.dictionary.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.woting.appengine.dictionary.model.DictDetail;
import com.woting.appengine.dictionary.model.DictMaster;
import com.woting.appengine.dictionary.persistence.pojo.DictDetailPo;
import com.woting.appengine.dictionary.persistence.pojo.DictMasterPo;

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

    //以下为字典组相关操作
    /**
     * 获得字典组逻辑对象列表
     * @param paramDm 字典组持久化对象，此对象中的值将是条件，这些条件是And的关系
     * @return 字典组逻辑对象列表
     */
    public List<DictMaster> getDictMList(DictMasterPo paramDm) {
        List<DictMasterPo> _l = dictMDao.queryForList(paramDm.toHashMapAsBean());
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
     * 根据所有者Id获得字典组逻辑对象列表
     * @param ownerId 所有者Id
     * @return 字典组逻辑对象列表
     */
    public List<DictMaster> getDictMListByOwnerId(String ownerId) {
        DictMasterPo paramDm = new DictMasterPo();
        paramDm.setOwnerId(ownerId);
        return this.getDictMList(paramDm);
    }

    /**
     * 新增字典组信息
     * @param dm 字典组逻辑对象信息
     */
    public void addDictMaster(DictMaster dm) {
        DictMasterPo newDmp = dm.convert2Po();
        //以下两个设置其实是没有必要的，在myBatis的定义中insert时就不涉及这两个字段，这两个字段是在mysql的表定义中定义，会自动填入
        //之所以加入，是为了提醒读代码的人员注意时间的设置，同时，若更换了orm的框架，或数据表的定义，这个设置可能就有用了
        /*
        newDmp.setCTime(new Timestamp(new Date().getTime()));
        newDmp.setLmTime(new Timestamp(new Date().getTime()));
        */
        dictMDao.insert(newDmp);
    }

    //以下为字典项相关操作
    /**
     * 获得字典项逻辑对象列表
     * @param paramDd 字典项持久化对象，此对象中的值将是条件，这些条件是And的关系
     * @return 字典项逻辑对象列表
     */
    public List<DictDetail> getDictDList(DictDetailPo paramDd) {
        List<DictDetailPo> _l = dictDDao.queryForList(paramDd.toHashMapAsBean());
        if (_l==null||_l.size()==0) return null;
        List<DictDetail> ret = new ArrayList<DictDetail>();
        for (DictDetailPo ddp: _l) {
            DictDetail dd = new DictDetail();
            dd.buildFromPo(ddp);
            ret.add(dd);
        }
        return ret.size()==0?null:ret;
    }

    /**
     * 根据所有者Id获得字典项逻辑对象列表
     * @param ownerId 所有者Id
     * @return 字典项逻辑对象列表
     */
    public List<DictDetail> getDictDListByOwnerId(String ownerId) {
        Map<String, String> param = new HashMap<String, String>();
        param.put("ownerId", ownerId);
        List<DictDetailPo> _l = dictDDao.queryForList("getListByOwnerId", param);
        if (_l==null||_l.size()==0) return null;
        List<DictDetail> ret = new ArrayList<DictDetail>();
        for (DictDetailPo ddp: _l) {
            DictDetail dd = new DictDetail();
            dd.buildFromPo(ddp);
            ret.add(dd);
        }
        return ret.size()==0?null:ret;
    }

    /**
     * 新增字典项
     * @param dd 字典项信息
     */
    public void addDictDetail(DictDetail dd) {
        DictDetailPo newDdp = dd.convert2Po();
        //以下两个设置其实是没有必要的，在myBatis的定义中insert时就不涉及这两个字段，这两个字段是在mysql的表定义中定义，会自动填入
        //之所以加入，是为了提醒读代码的人员注意时间的设置，同时，若更换了orm的框架，或数据表的定义，这个设置可能就有用了
        /*
        newDdp.setCTime(new Timestamp(new Date().getTime()));
        newDdp.setLmTime(new Timestamp(new Date().getTime()));
        */
        dictDDao.insert(newDdp);
    }
}