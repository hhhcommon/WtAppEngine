package com.woting.cm.core.broadcast.service;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.broadcast.persis.po.BCProgrammePo;

@Service
public class BcProgrammeService {

	@Resource(name="defaultDAO")
	private MybatisDAO<BCProgrammePo> bcProDao;
	
	@PostConstruct
    public void initParam() {
        bcProDao.setNamespace("A_BCPROGRAMME");
    }
	
	public List<BCProgrammePo> getBCProgrammeListByTime(String bcId, int weekDay, long time, long validTime, String ordersql, int limitnum) {
		Map<String, Object> m = new HashMap<>();
		m.put("bcId", bcId);
		m.put("weekDay", weekDay);
		m.put("sort", 0);
		if (validTime!=0) {
			m.put("validTime", new Timestamp(validTime));
		}
		if (time!=0) {
			m.put("wheresql", " validTime < '"+new Timestamp(time)+"'");
		}
		if (ordersql!=null) {
			m.put("orderByClause", ordersql);
		}
		if (limitnum!=0) {
			m.put("limitNum", limitnum);
		}
		List<BCProgrammePo> bcps = bcProDao.queryForList("getList", m);
		if (bcps!=null && bcps.size()>0) {
			return bcps;
		}
		return null;
	}
	
	public String getBcIsPlaying(String bcId, int weekDay, String timestr, long time) {
		Map<String, Object> m = new HashMap<>();
		m.put("bcId", bcId);
		m.put("weekDay", weekDay);
		m.put("sort", 0);
		m.put("wheresql", "'"+timestr + "' BETWEEN beginTime and endTime");
		m.put("orderByClause", " validTime");
		m.put("limitNum", "1");
		BCProgrammePo bcPo = bcProDao.getInfoObject("getList", m);
		if (bcPo!=null) {
			return bcPo.getTitle();
		}
		return null;
	}

	/**
	 * 得到电台组放节目的节目单
     * @param bcIds 电台Id的列表,用逗号隔开
     * @param time 当前时间的时间戳（当前时间由发送者确定，目前不支持世界时区)
	 * @return Map对象，key是电台的Id，value是电台的节目名称
	 */
    public Map<String, String> getBcsPlaying(String bcIds, long time) {
        if (StringUtils.isNullOrEmptyOrSpace(bcIds)) return null;
        Calendar cal = Calendar.getInstance();//得到当前的时间(服务器标准)
        Date date = new Date(time);
        cal.setTime(date);
        int week = cal.get(Calendar.DAY_OF_WEEK);
        DateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String timestr = sdf.format(date);

        String orSql=bcIds.replaceAll(",", "' or bcId='");
        orSql="bcId='"+orSql+"'";
        
        Map<String, Object> m = new HashMap<>();
        m.put("weekDay", week);
        m.put("sort", 0);
        m.put("orderByClause", " bcId, validTime");
        m.put("wheresql", "("+orSql+") and ('"+timestr + "'>beginTime and '"+timestr + "'<endTime)");
        List<BCProgrammePo> bcPoList=bcProDao.queryForList("getList", m);

        if (bcPoList==null||bcPoList.isEmpty()) return null;
        Map<String, String> ret=new HashMap<String, String>();
        for (BCProgrammePo bcPo: bcPoList) {
            ret.put(bcPo.getBcId(), bcPo.getTitle());
        }
        return ret;
    }
	
	public void insertBCProgrammeList(List<BCProgrammePo> bcProlist) {
		bcProDao.insert("insertList", bcProlist);
	}
	
	public void deleteById(String id) {
		bcProDao.delete("deleteById", id);
	}
}
