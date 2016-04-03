package com.woting.cm.core.tracklog.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.woting.cm.core.common.model.Owner;
import com.woting.cm.core.tracklog.model.TrackLog;
import com.woting.cm.core.tracklog.persis.po.TrackLogPo;

@Service
public class TrackLogService {
    @Resource(name="defaultDAO")
    private MybatisDAO<TrackLogPo> trackLogDao;

    @PostConstruct
    public void initParam() {
        trackLogDao.setNamespace("A_TLOG");
    }

    /**
     * 根据业务键获得对应的最新一条日志
     * @param tableName 表名称
     * @param o 所属源
     * @param data 数据
     * @return 对应的日志列表
     */
    public TrackLog getTLogByBizKey(String tableName, Owner o, String data) {
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("tableName", tableName);
        param.put("ownerType", o.getOwnerType());
        param.put("ownerId", o.getOwnerId());
        param.put("dataMd5", data);
        param.put("sortByClause", "cTime desc");
        List<TrackLogPo> tlPoL=trackLogDao.queryForList("getByBizKey", param);
        if (tlPoL!=null&&!tlPoL.isEmpty()) {
            TrackLog ret=new TrackLog();
            ret.buildFromPo(tlPoL.get(0));
            return ret;
        }
        return null;
    }

    /**
     * 跟踪日志加入数据库
     * @param tlog
     */
    public void insert(TrackLog tlog) {
        trackLogDao.insert(tlog.convert2Po());
    }
}