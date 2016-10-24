package com.woting.appengine.appopinion.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.appopinion.model.AppOpinion;
import com.woting.appengine.appopinion.persis.pojo.AppOpinionPo;
import com.woting.appengine.appopinion.persis.pojo.AppReOpinionPo;

public class AppOpinionService {
    @Resource(name="defaultDAO")
    private MybatisDAO<AppOpinionPo> opinionDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<AppReOpinionPo> reOpinionDao;

    @PostConstruct
    public void initParam() {
        opinionDao.setNamespace("WT_APPOPINION");
        reOpinionDao.setNamespace("WT_APPREOPINION");
    }

    /**
     * 得到意见
     * @param opinion 意见信息
     * @return 创建用户成功返回1，否则返回0
     */
    public List<AppOpinionPo> getDuplicates(AppOpinionPo opinion) {
        try {
            return opinionDao.queryForList("getDuplicates", opinion.toHashMapAsBean());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 保存用户所提意见
     * @param opinion 意信息
     * @return 创建用户成功返回1，否则返回0
     */
    public int insertOpinion(AppOpinionPo opinion) {
        int i=0;
        try {
            opinion.setId(SequenceUUID.getUUIDSubSegment(4));
            opinionDao.insert(opinion);
            i=1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * 根据用户指标（userId或Imei）得到意见及反馈列表
     * @param userId 用户Id
     * @param imei 设备编码
     * @return 意见及反馈列表
     */
    public List<AppOpinion> getOpinionsByOnwerId(String userId, String imei) {
        try {
            Map<String, String> param = new HashMap<String, String>();
            param.put("userId", userId);
            param.put("imei", imei);
            List<AppOpinionPo> ol = this.opinionDao.queryForList("getListByUserId", param);
            if (ol!=null&&ol.size()>0) {
                List<AppOpinion> ret = new ArrayList<AppOpinion>();
                AppOpinion item = null;
                List<AppReOpinionPo> rol = this.reOpinionDao.queryForList("getListByUserId", param);
                if (rol!=null&&rol.size()>0) {
                    int i=0;
                    AppReOpinionPo arop=rol.get(i);
                    for (AppOpinionPo op: ol) {
                        item=new AppOpinion();
                        item.buildFromPo(op);
                        if (i<rol.size()) {
                            while (arop.getOpinionId().equals(op.getId())) {
                                item.addOneRe(arop);
                                if (++i==rol.size()) break;
                                arop=rol.get(i);
                            }
                        }
                        ret.add(item);
                    }
                } else {
                    for (AppOpinionPo op: ol) {
                        item=new AppOpinion();
                        item.buildFromPo(op);
                        ret.add(item);
                    }
                }
                return ret;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}