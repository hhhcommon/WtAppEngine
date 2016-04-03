package com.woting.appengine.content.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.DateUtils;
import com.woting.passport.UGA.persistence.pojo.GroupPo;

public class ContentService {
    //先用Group代替！！
    @Resource(name="defaultDAO")
    private MybatisDAO<GroupPo> groupDao;

    @PostConstruct
    public void initParam() {
        groupDao.setNamespace("WT_GROUP");
    }

    @Resource
    private DataSource dataSource;

//    private void runSql(Statement _st, effectively ResultSet _rs, String sql) {
//        new Thread() {
//            public void run() {
//                ResultSet res=null;
//                try {
//                    res=_st.executeQuery(sql);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    _rs=res;
//                }
//            }
//        }.start();
//    }
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
        Map<String, List<String>> typeMap=new HashMap<String, List<String>>();
        Map<String, List<String>> reBuildMap=new HashMap<String, List<String>>();
        Map<String, Object> paraM=new HashMap<String, Object>();
        //0.1-查找分类
        Connection conn=null;
        Statement st=null;
        ResultSet rs=null;
        String sql="select * from wt_ResDict_Ref where ";
        for (int k=0; k<_s.length; k++) {
            if (k==0) sql+=" title like '%"+_s[k]+"%'";
            else sql+=" or title like '%"+_s[k]+"%'";
        }
        List<Map<String, Object>> cataList=null;
        try {
            conn=dataSource.getConnection();
            st=conn.createStatement();
            rs=st.executeQuery(sql+" limit 0, 10");
            cataList=new ArrayList<Map<String, Object>>();
            while (rs!=null&&rs.next()) {
                Map<String, Object> oneData=new HashMap<String, Object>();
                oneData.put("id", rs.getString("id"));
                oneData.put("refName", rs.getString("refName"));
                oneData.put("resTableName", rs.getString("resTableName"));
                oneData.put("resId", rs.getString("resId"));
                cataList.add(oneData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
            if (st!=null) try {st.close();st=null;} catch(Exception e) {st=null;} finally {st=null;};
            if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
        }
        for (int i=0; i<cataList.size(); i++) {
            Map<String, Object> one=cataList.get(i);
            String resTableName=one.get("resTableName")+"";
            if (resTableName.equals("1")) resTableName="wt_Broadcast";
            if (typeMap.get(resTableName)==null) typeMap.put(resTableName, new ArrayList<String>());
            typeMap.get(resTableName).add(one.get("resId")+"");
        }
        //0.2-查找节目-查人员
        List<Map<String, Object>> personList=groupDao.queryForListAutoTranform("searchPerson", _s);
        for (int i=0; i<personList.size(); i++) {
            Map<String, Object> one=personList.get(i);
            String resTableName=one.get("resTableName")+"";
            if (typeMap.get(resTableName)==null) typeMap.put(resTableName, new ArrayList<String>());
            typeMap.get(resTableName).add(one.get("resTableName")+"");
        }

        List<Map<String, Object>> tempList=null;
        //1-查找电台
        paraM.put("searchArray", _s);
        String tempStr=getIds(typeMap.get("wt_Broadcast"));
        if (tempStr!=null) paraM.put("inIds", tempStr);
        tempList=groupDao.queryForListAutoTranform("searchBc", paraM);
        for (int i=0; i<tempList.size(); i++) {
            add(ret1, tempList.get(i));
            //为重构做数据准备
            if (reBuildMap.get("wt_Broadcast")==null) reBuildMap.put("wt_Broadcast", new ArrayList<String>());
            reBuildMap.get("wt_Broadcast").add(tempList.get(i).get("id")+"");
        }
        //2-查找单体节目
        tempStr=getIds(typeMap.get("wt_MediaAsset"));
        sql="select a.* from wt_MediaAsset a where ";
        for (int k=0; k<_s.length; k++) {
            if (k==0) sql+=" CONCAT(a.maTitle,'#S#',a.maPublisher,'#S#',a.subjectWords,'#S#',a.keyWords,'#S#',a.descn) like '%"+_s[k]+"%'";
            else sql+=" or CONCAT(a.maTitle,'#S#',a.maPublisher,'#S#',a.subjectWords,'#S#',a.keyWords,'#S#',a.descn) like '%"+_s[k]+"%'";
        }
        sql+=" limit 0, 20";
        if (tempStr!=null) sql+=" union select c.* from wt_MediaAsset c where c.id in ("+tempStr+") limit 0, 10";
        try {
            conn=dataSource.getConnection();
            st=conn.createStatement();
            st.setQueryTimeout(600);
            rs=st.executeQuery(sql);
            st.cancel();
            tempList=new ArrayList<Map<String, Object>>();
            while (rs.next()) {
                Map<String, Object> oneData=new HashMap<String, Object>();
                oneData.put("id", rs.getString("id"));
                oneData.put("maTitle", rs.getString("maTitle"));
                oneData.put("maPubType", rs.getInt("maPubType"));
                oneData.put("maPubId", rs.getString("maPubId"));
                oneData.put("maPublisher", rs.getString("maPublisher"));
                oneData.put("maPublishTime", rs.getTimestamp("maPublishTime"));
                oneData.put("maImg", rs.getString("maImg"));
                oneData.put("maURL", rs.getString("maURL"));
                oneData.put("subjectWords", rs.getString("subjectWords"));
                oneData.put("keyWords", rs.getString("keyWords"));
                oneData.put("timeLong", rs.getString("timeLong"));
                oneData.put("descn", rs.getString("descn"));
                oneData.put("pubCount", rs.getInt("pubCount"));
                oneData.put("cTime", rs.getTimestamp("cTime"));
                tempList.add(oneData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
            if (st!=null) try {st.close();st=null;} catch(Exception e) {st=null;} finally {st=null;};
            if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
        }

//        if (tempStr!=null) paraM.put("inIds", tempStr);
//        tempList=groupDao.queryForListAutoTranform("searchMa", paraM);
        for (int i=0; i<tempList.size(); i++) {
            add(ret2, tempList.get(i));
            //为重构做数据准备
            if (reBuildMap.get("wt_MediaAsset")==null) reBuildMap.put("wt_MediaAsset", new ArrayList<String>());
            reBuildMap.get("wt_MediaAsset").add(tempList.get(i).get("id")+"");
        }
        //3-查找系列节目
        tempStr=getIds(typeMap.get("wt_SeqMediaAsset"));
        sql="select a.*, case when b.count is null then 0 else b.count end as count from wt_SeqMediaAsset a left join (select sid, count(*) count from wt_SeqMA_Ref group by sid) b on a.id=b.sid where ";
        for (int k=0; k<_s.length; k++) {
            if (k==0) sql+="(CONCAT(a.smaTitle,'#S#',a.smaPublisher,'#S#',a.subjectWords,'#S#',a.keyWords,'#S#',a.descn) like '%"+_s[k]+"%'";
            else sql+=" or CONCAT(a.smaTitle,'#S#',a.smaPublisher,'#S#',a.subjectWords,'#S#',a.keyWords,'#S#',a.descn) like '%"+_s[k]+"%'";
        }
        sql+=") and b.count>0 limit 0,10";
        if (tempStr!=null) sql+=" union select c.*, case when d.count is null then 0 else d.count end as count from wt_SeqMediaAsset c left join (select sid, count(*) count from wt_SeqMA_Ref group by sid) d on c.id=d.sid where c.id in ("+
                tempStr+")limit 0, 10";
        try {
            conn=dataSource.getConnection();
            st=conn.createStatement();
            st.setQueryTimeout(100);
            rs=st.executeQuery(sql);
            st.cancel();
            tempList=new ArrayList<Map<String, Object>>();
            while (rs.next()) {
                Map<String, Object> oneData=new HashMap<String, Object>();
                oneData.put("id", rs.getString("id"));
                oneData.put("smaTitle", rs.getString("smaTitle"));
                oneData.put("smaPubType", rs.getInt("smaPubType"));
                oneData.put("smaPubId", rs.getString("smaPubId"));
                oneData.put("smaPublisher", rs.getString("smaPublisher"));
                oneData.put("smaPublishTime", rs.getTimestamp("smaPublishTime"));
                oneData.put("smaImg", rs.getString("smaImg"));
                oneData.put("smaAllCount", rs.getString("smaAllCount"));
                oneData.put("subjectWords", rs.getString("subjectWords"));
                oneData.put("keyWords", rs.getString("keyWords"));
                oneData.put("count", rs.getString("count"));
                oneData.put("descn", rs.getString("descn"));
                oneData.put("pubCount", rs.getInt("pubCount"));
                oneData.put("cTime", rs.getTimestamp("cTime"));
                tempList.add(oneData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
            if (st!=null) try {st.close();st=null;} catch(Exception e) {st=null;} finally {st=null;};
            if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
        }
//        if (tempStr!=null) paraM.put("inIds", tempStr);
//        tempList=groupDao.queryForListAutoTranform("searchSeqMa", paraM);
        for (int i=0; i<tempList.size(); i++) {
            add(ret3, tempList.get(i));
            //为重构做数据准备
            if (reBuildMap.get("wt_SeqMediaAsset")==null) reBuildMap.put("wt_SeqMediaAsset", new ArrayList<String>());
            reBuildMap.get("wt_SeqMediaAsset").add(tempList.get(i).get("id")+"");
        }
        if ((ret1==null||ret1.size()==0)&&(ret2==null||ret2.size()==0)&&(ret3==null||ret3.size()==0)) return null;

        //重构人员及分类列表
        paraM.clear();
        if (reBuildMap.get("wt_Broadcast")!=null&&reBuildMap.get("wt_Broadcast").size()>0) {
            paraM.put("bcIds", getIds(reBuildMap.get("wt_Broadcast")));
        }
        if (reBuildMap.get("wt_MediaAsset")!=null&&reBuildMap.get("wt_MediaAsset").size()>0) {
            paraM.put("maIds", getIds(reBuildMap.get("wt_MediaAsset")));
        }
        if (reBuildMap.get("wt_SeqMediaAsset")!=null&&reBuildMap.get("wt_SeqMediaAsset").size()>0) {
            paraM.put("smaIds", getIds(reBuildMap.get("wt_SeqMediaAsset")));
        }
        //重构人员
        personList=groupDao.queryForListAutoTranform("refPersonById", paraM);
        //重构分类
        cataList=groupDao.queryForListAutoTranform("refCataById", paraM);

        Map<String, Object> ret=new HashMap<String, Object>();
        ret.put("ResultType", resultType);
        ret.put("AllCount", (ret1==null?0:ret1.size())+(ret2==null?0:ret2.size())+(ret3==null?0:ret3.size()));
        Map<String, Object> oneMedia;
        int i=0;
        if (resultType==0) {//按一个列表获得
            List<Map<String, Object>> allList=new ArrayList<Map<String, Object>>();
            oneMedia=new HashMap<String, Object>();
            if (ret1!=null||ret1.size()>0) {
                for (; i<ret1.size(); i++) {
                    oneMedia=convert2MediaMap_1(ret1.get(i), cataList, personList);
                    add(allList, oneMedia);
                }
            }
            if (ret2!=null||ret2.size()>0) {
                for (i=0; i<ret2.size(); i++) {
                    oneMedia=convert2MediaMap_2(ret2.get(i), cataList, personList);
                    add(allList, oneMedia);
                }
            }
            if (ret3!=null||ret3.size()>0) {
                for (i=0; i<ret3.size(); i++) {
                    oneMedia=convert2MediaMap_3(ret3.get(i), cataList, personList);
                    add(allList, oneMedia);
                }
            }
            ret.put("List", allList);
        } else if (resultType==1) {//按分类列表获得
            if (ret1!=null||ret1.size()>0) {
                List<Map<String, Object>> resultList1=new ArrayList<Map<String, Object>>();
                for (i=0; i<ret1.size(); i++) {
                    oneMedia=convert2MediaMap_1(ret1.get(i), cataList, personList);
                    resultList1.add(oneMedia);
                }
                Map<String, Object> bcResult=new HashMap<String, Object>();
                bcResult.put("Count", ret1.size());
                bcResult.put("List", resultList1);
                ret.put("BcResult", bcResult);
            }
            if (ret2!=null||ret2.size()>0) {
                List<Map<String, Object>> resultList2=new ArrayList<Map<String, Object>>();
                for (i=0; i<ret2.size(); i++) {
                    oneMedia=convert2MediaMap_2(ret2.get(i), cataList, personList);
                    resultList2.add(oneMedia);
                }
                Map<String, Object> mResult=new HashMap<String, Object>();
                mResult.put("Count", ret2.size());
                mResult.put("List", resultList2);
                ret.put("mResult", mResult);
            }
            if (ret3!=null||ret3.size()>0) {
                List<Map<String, Object>> resultList3=new ArrayList<Map<String, Object>>();
                for (i=0; i<ret3.size(); i++) {
                    oneMedia=convert2MediaMap_3(ret3.get(i), cataList, personList);
                    resultList3.add(oneMedia);
                }
                Map<String, Object> smResult=new HashMap<String, Object>();
                smResult.put("Count", ret3.size());
                smResult.put("List", resultList3);
                ret.put("smResult", smResult);
            }
        }
        //结果放入组；
        return ret;
    }
    private String getIds(List<String> l) {
        if (l==null||l.size()==0) return null;
        String ret="";
        for (String sKey: l) ret+=",'"+sKey+"'";
        return ret.substring(1);
    }
    private void add(List<Map<String, Object>> ret, Map<String, Object> oneM) {
        int insertIndex=-1;
        for (int i=0; i<ret.size(); i++) {
            Map<String, Object> thisM=ret.get(i);
            try {
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
    //转换电台
    private Map<String, Object> convert2MediaMap_1(Map<String, Object> one, List<Map<String, Object>> cataList, List<Map<String, Object>> personList) {
        Map<String, Object> retM=new HashMap<String, Object>();

        retM.put("MediaType", "RADIO");

        retM.put("ContentId", one.get("id"));//P01-公共：ID
        retM.put("ContentName", one.get("bcTitle"));//P02-公共：名称
        retM.put("ContentPub", one.get("bcPublisher"));//P03-公共：发布者，集团名称
        retM.put("ContentImg", one.get("bcImg"));//P07-公共：相关图片
        retM.put("ContentPlay", one.get("flowURI"));//P08-公共：主播放Url
        retM.put("ContentSource", one.get("bcSource"));//P09-公共：来源名称
        retM.put("ContentURIS", null);//P10-公共：其他播放地址列表，目前为空
        retM.put("ContentDesc", one.get("descn"));//P11-公共：说明
        retM.put("ContentPersons", fetchPersons(personList, 1, retM.get("ContentId")+""));//P12-公共：相关人员列表
        retM.put("ContentCatalogs", fetchCatas(cataList, 1, retM.get("ContentId")+""));//P13-公共：所有分类列表

        retM.put("ContentFreq", one.get(""));//S01-特有：主频率，目前为空
        retM.put("ContentFreqs", one.get(""));//S02-特有：频率列表，目前为空
        retM.put("ContentList", one.get(""));//S03-特有：节目单列表，目前为空

        retM.put("CTime", one.get("cTime"));//A1-管控：节目创建时间，目前以此进行排序

        return retM;
    }
    //转换单媒体
    private Map<String, Object> convert2MediaMap_2(Map<String, Object> one, List<Map<String, Object>> cataList, List<Map<String, Object>> personList) {
        Map<String, Object> retM=new HashMap<String, Object>();

        retM.put("MediaType", "AUDIO");

        retM.put("ContentId", one.get("id"));//P01-公共：ID
        retM.put("ContentName", one.get("maTitle"));//P02-公共：名称
        retM.put("ContentSubjectWord", one.get("subjectWord"));//P03-公共：主题词
        retM.put("ContentKeyWord", one.get("keyWord"));//P04-公共：关键字
        retM.put("ContentPub", one.get("maPublisher"));//P05-公共：发布者，集团名称
        retM.put("ContentPubTime", one.get("maPublishTime"));//P06-公共：发布时间
        retM.put("ContentImg", one.get("maImg"));//P07-公共：相关图片
        retM.put("ContentPlay", one.get("maURL"));//P08-公共：主播放Url，这个应该从其他地方来，现在先这样//TODO
        retM.put("ContentURI", "content/getContentInfo.do?ContentId="+retM.get("ContentId"));//P08-公共：主播放Url，这个应该从其他地方来，现在先这样//TODO
//        retM.put("ContentSource", one.get("maSource"));//P09-公共：来源名称
//        retM.put("ContentURIS", null);//P10-公共：其他播放地址列表，目前为空
        retM.put("ContentDesc", one.get("descn"));//P11-公共：说明
        retM.put("ContentPersons", fetchPersons(personList, 2, retM.get("ContentId")+""));//P12-公共：相关人员列表
        retM.put("ContentCatalogs", fetchCatas(cataList, 2, retM.get("ContentId")+""));//P13-公共：所有分类列表

        retM.put("ContentTimes", one.get("timeLong"));//S01-特有：播放时长

        retM.put("CTime", one.get("cTime"));//A1-管控：节目创建时间，目前以此进行排序

        return retM;
    }
    //转换系列媒体
    private Map<String, Object> convert2MediaMap_3(Map<String, Object> one, List<Map<String, Object>> cataList, List<Map<String, Object>> personList) {
        Map<String, Object> retM=new HashMap<String, Object>();

        retM.put("MediaType", "SEQU");

        retM.put("ContentId", one.get("id"));//P01-公共：ID
        retM.put("ContentName", one.get("smaTitle"));//P02-公共：名称
        retM.put("ContentSubjectWord", one.get("subjectWord"));//P03-公共：主题词
        retM.put("ContentKeyWord", one.get("keyWord"));//P04-公共：关键字
        retM.put("ContentPub", one.get("smaPublisher"));//P05-公共：发布者，集团名称
        retM.put("ContentPubTime", one.get("smaPublishTime"));//P06-公共：发布时间
        retM.put("ContentImg", one.get("smaImg"));//P07-公共：相关图片
        retM.put("ContentURI", "content/getContentInfo.do?ContentId="+retM.get("ContentId"));//P08-公共：在此是获得系列节目列表的Url
        retM.put("ContentDesc", one.get("descn"));//P11-公共：说明
        retM.put("ContentPersons", fetchPersons(personList, 3, retM.get("ContentId")+""));//P12-公共：相关人员列表
        retM.put("ContentCatalogs", fetchCatas(cataList, 3, retM.get("ContentId")+""));//P13-公共：所有分类列表

        retM.put("ContentSubCount", one.get("count"));//S01-特有：下级节目的个数

        retM.put("CTime", one.get("cTime"));//A1-管控：节目创建时间，目前以此进行排序

        return retM;
    }
    private List<Map<String, Object>> fetchPersons(List<Map<String, Object>> personList, int resType, String resId) {
        if (personList==null||personList.size()==0) return null;
        Map<String, Object> onePerson=null;
        List<Map<String, Object>> ret=new ArrayList<Map<String, Object>>();
        for (Map<String, Object> _p: personList) {
            if ((_p.get("resType")+"").equals(resType+"")&&(_p.get("resId")+"").equals(resId)) {
                onePerson=new HashMap<String, Object>();
                onePerson.put("RefName", _p.get("cName"));//关系名称
                onePerson.put("PerName", _p.get("pName"));//人员名称
                ret.add(onePerson);
            }
        }
        return ret.size()>0?ret:null;
    }
    private List<Map<String, Object>> fetchCatas(List<Map<String, Object>> cataList, int resType, String resId) {
        if (cataList==null||cataList.size()==0) return null;
        Map<String, Object> oneCata=new HashMap<String, Object>();
        List<Map<String, Object>> ret=new ArrayList<Map<String, Object>>();
        for (Map<String, Object> _c: cataList) {
            if ((_c.get("resType")+"").equals(resType+"")&&(_c.get("resId")+"").equals(resId)) {
                oneCata=new HashMap<String, Object>();
                oneCata.put("CataMName", _c.get("dictMName"));//大分类名称
                oneCata.put("CataTitle", _c.get("pathNames"));//分类名称，树结构名称
                ret.add(oneCata);
            }
        }
        return ret.size()>0?ret:null;
    }

    /**
     * 获得主页信息
     * @param userId
     * @return
     */
    public Map<String, Object> getMainPage(String userId) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 获得主页信息
     * @param userId
     * @return
     */
    public Map<String, Object> getSeqMaInfo(String contentId, int pageSize, int page) {
        List<Map<String, Object>> cataList=null;//分类
        List<Map<String, Object>> personList=null;//人员
        Map<String, Object> paraM=new HashMap<String, Object>();

        //1、得主内容
        Map<String, Object> tempMap=groupDao.queryForObjectAutoTranform("getSmById", contentId);
        if (tempMap==null||tempMap.size()==0) return null;
        paraM.put("resType", "3");
        paraM.put("ids", contentId);
        cataList=groupDao.queryForListAutoTranform("getCataListByTypeAndIds", paraM);
        personList=groupDao.queryForListAutoTranform("getPersonListByTypeAndIds", paraM);
        Map<String, Object>  retInfo = convert2MediaMap_3(tempMap, cataList, personList);

        //2、得到明细内容
        List<Map<String, Object>> tempList=groupDao.queryForListAutoTranform("getSmSubMedias", contentId);
        if (tempList!=null&&tempList.size()>0) {
            String ids="";
            for (Map<String, Object> one: tempList) {
                if (one.get("id")!=null) ids+=",'"+one.get("id")+"'";
            }
            ids=ids.substring(1);
            paraM.clear();
            paraM.put("resType", "2");
            paraM.put("ids", ids);
            cataList=groupDao.queryForListAutoTranform("getCataListByTypeAndIds", paraM);
            personList=groupDao.queryForListAutoTranform("getPersonListByTypeAndIds", paraM);

            List<Map<String, Object>> subList=new ArrayList<Map<String, Object>>();
            //计算页数
            int begin=0, end=tempList.size();
            if (pageSize>0&&page>0) {
                begin=pageSize*(page-1);
                end=begin+pageSize;
                if (end>tempList.size()) end=tempList.size();
            }
            for (int i=begin; i<end; i++) {
                subList.add(convert2MediaMap_2(tempList.get(i), cataList, personList));
            }
            retInfo.put("SubList", subList);
            retInfo.put("PageSize", subList.size());
            retInfo.put("Page", page);
            retInfo.put("ContentSubCount", tempList.size());
        } 
        return retInfo;
    }
}
//测试的消息{"IMEI":"12356","UserId":"107fc906ae0f","ResultType":"0","SearchStr":"罗,电影,安徽"}
//http://localhost:808/wt/searchByVoice.do
//http://localhost:808/wt/content/getSeqMaInfo.do