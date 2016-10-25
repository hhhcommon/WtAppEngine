package com.woting.dataanal.gather.API.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.dataanal.gather.API.persis.pojo.ApiLogPo;

/**
 * 访问日志管理服务类，提供如下服务：<br/>
 * <pre>
 * 1-与持久化数据交互的功能在这个服务中提供
 * 2-用户切换功能
 * 3-根据日志分类获得分类服务
 * </pre>
 * @author wh
 */
@Service
public class ApiLogService {
    @Resource(name="defaultDAO")
    private MybatisDAO<ApiLogPo> apiLogDao; //api调用

    @PostConstruct
    public void initParam() {
        apiLogDao.setNamespace("ApiLog");
    }

    /**
     * 保存访问日志信息到数据库
     * @param vlp 访问日志信息
     */
    public void Save2DB(ApiLogPo alp) {
        if (StringUtils.isNullOrEmptyOrSpace(alp.getId())) alp.setId(SequenceUUID.getPureUUID());
        apiLogDao.insert(alp);
    }
}