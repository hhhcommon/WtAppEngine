package com.woting.appengine.passport.login.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.passport.login.persistence.pojo.MobileUsedPo;

/**
 * 移动设备用户使用情况
 * @author wh
 */
public class MobileUsedService {
    @Resource(name="defaultDAO")
    private MybatisDAO<MobileUsedPo> muDao;

    @PostConstruct
    public void initParam() {
        muDao.setNamespace("MOBILE_USED");
    }

    /**
     * 保存用户使用情况
     * @param mu
     */
    public void saveMobileUsed(MobileUsedPo mu) {
        try {
            mu.setMuId(SequenceUUID.getUUIDSubSegment(4));
            muDao.insert(mu);
        } catch(Exception e) {
            try {
                muDao.update("updateByIMEI", mu);
            } catch(Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * 根据手机串号，获取最后使用情况
     * @param imei 手机串号
     */
    public MobileUsedPo getUsedInfo(String imei) {
        return muDao.getInfoObject("getUsedInfo", imei);
    }
}