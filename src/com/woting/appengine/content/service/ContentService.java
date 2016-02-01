package com.woting.appengine.content.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.DateUtils;
import com.woting.passport.UGA.persistence.pojo.GroupPo;

public class ContentService {
    //先用Group代替
    @Resource(name="defaultDAO")
    private MybatisDAO<GroupPo> groupDao;

    @PostConstruct
    public void initParam() {
        groupDao.setNamespace("WT_GROUP");
    }

    /**
     * 查找内容，此内容无排序，按照创建时间的先后顺序排序，最新的在最前面
     * @param searchStr 查找串
     * @param resultType 返回类型,0把所有结果按照一个列表返回；1按照“单体节目、系列节目、电台”的顺序分类给出
     * @return 创建用户成功返回1，否则返回0
     */
    public Map<String, Object> searchAll(String searchStr, int resultType) {
        String __s[]=searchStr.split(",");
        String _s[]=new String[__s.length];
        for (int i=0; i<__s.length; i++) _s[i]=__s[i].trim();

        List<Map<String, Object>> ret1 = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> ret2 = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> ret3 = new ArrayList<Map<String, Object>>();
        Map<String, Map<String, Object>> typeMap=new HashMap<String, Map<String, Object>>();
        Map<String, Object> paraM=new HashMap<String, Object>();
        //0.1-查找分类
        List<Map<String, Object>> tempList=groupDao.queryForListAutoTranform("searchCata", _s);
        for (int i=0; i<tempList.size(); i++) {
            Map<String, Object> one=tempList.get(i);
            String resType=one.get("resType")+"";
            if (typeMap.get(resType)==null) typeMap.put(resType, new HashMap<String, Object>());
            typeMap.get(resType).put(one.get("resId")+"", one.get("resId"));
        }
        //0.2-查找节目-查人员
        List<Map<String, String>> personList=groupDao.queryForListAutoTranform("searchPerson", _s);
        for (int i=0; i<personList.size(); i++) {
            Map<String, String> one=personList.get(i);
            String resType=one.get("rType");
            if (typeMap.get(resType)==null) typeMap.put(resType, new HashMap<String, Object>());
            typeMap.get(resType).put(one.get("rId")+"",one.get("rId"));
        }
        //1-查找电台
        paraM.put("searchArray", _s);
        String tempStr=getIds(typeMap.get("1"));
        if (tempStr!=null) paraM.put("inIds", tempStr);
        tempList=groupDao.queryForListAutoTranform("searchBc", paraM);
        for (int i=0; i<tempList.size(); i++) {
            add(ret1, tempList.get(i));
        }
        //2-查找单体节目
        tempStr=getIds(typeMap.get("2"));
        if (tempStr!=null) paraM.put("inIds", tempStr);
        tempList=groupDao.queryForListAutoTranform("searchMa", paraM);
        for (int i=0; i<tempList.size(); i++) {
            add(ret2, tempList.get(i));
        }
        //3-查找系列节目
        tempStr=getIds(typeMap.get("3"));
        if (tempStr!=null) paraM.put("inIds", tempStr);
        tempList=groupDao.queryForListAutoTranform("searchSeqMa", paraM);
        for (int i=0; i<tempList.size(); i++) {
            add(ret3, tempList.get(i));
        }
        if ((ret1==null||ret1.size()==0)&&(ret2==null||ret2.size()==0)&&(ret3==null||ret3.size()==0)) return null;
        Map<String, Object> ret=new HashMap<String, Object>();
        ret.put("ResultType", resultType);
        ret.put("AllCount", (ret1==null?0:ret1.size())+(ret2==null?0:ret2.size())+(ret3==null?0:ret3.size()));
        //结果放入组；
        return null;
    }
    private String getIds(Map<String, Object> m) {
        if (m==null||m.size()==0) return null;
        String ret="";
        for (String sKey: m.keySet()) ret+=",'"+sKey+"'";
        return ret.substring(1);
    }
    private void add(List<Map<String, Object>> ret, Map<String, Object> oneM) {
        int insertIndex=-1;
        String tempStr="";
        for (int i=0; i<ret.size(); i++) {
            Map<String, Object> thisM=ret.get(i);
            try {
                thisM.get("cTime");
                
                Date thisD=DateUtils.getDateTime("yyyy-MM-dd HH:mm:ss", thisM.get("cTime")+"");
                Date oneD=DateUtils.getDateTime("yyyy-MM-dd HH:mm:ss", oneM.get("cTime").toString());
                if (thisD.before(oneD)) {
                    insertIndex=i;
                    break;
                }
            } catch (ParseException e) {
            }
        }
        insertIndex=insertIndex==-1?ret.size():insertIndex;
        ret.add(insertIndex, oneM);
    }
}