package com.woting.version.core.service;

import javax.annotation.Resource;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.StringUtils;
import com.woting.version.core.model.Version;
import com.woting.version.core.model.VersionConfig;

@Lazy(true)
@Service
public class VersionService {
    @Resource(name="defaultDAO")
    private MybatisDAO<Version> verDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<VersionConfig> verCfgDao;

    //版本配置======================================================================================================
    /**
     * 得到版本配置信息
     * @return
     */
    public VersionConfig getVerConfig() {
        return verCfgDao.getInfoObject("getCfgList");
    }

    /**
     * 根据所给版本号version，获得该版本号详细信息
     * @param version 所给版本号
     * @return 版本信息，若所给版本号不存在，返回null
     */
    public Version getVersion(String version) {
        if (StringUtils.isNullOrEmptyOrSpace(version)) return null;

        return null;
    }
}
