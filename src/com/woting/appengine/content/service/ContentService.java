package com.woting.appengine.content.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.util.DateUtils;
import com.spiritdata.framework.util.StringUtils;
import com.woting.WtAppEngineConstants;
import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;
import com.woting.appengine.searchcrawler.service.kl_s_Service;
import com.woting.appengine.searchcrawler.utils.DataTransform;
import com.woting.cm.core.channel.mem._CacheChannel;
import com.woting.cm.core.dict.mem._CacheDictionary;
import com.woting.cm.core.dict.model.DictModel;
import com.woting.passport.UGA.persistence.pojo.GroupPo;

@Lazy(true)
@Service
public class ContentService {
    //先用Group代替！！
    @Resource(name="defaultDAO")
    private MybatisDAO<GroupPo> groupDao;
    @Resource
    private DataSource dataSource;

    kl_s_Service kl_s = new kl_s_Service();  //wbq 考拉搜索
    DataTransform dataT = new DataTransform();//wbq 数据类型转换
    
    private _CacheDictionary _cd=null;
    private _CacheChannel _cc=null;

    @PostConstruct
    public void initParam() {
        groupDao.setNamespace("WT_GROUP");
        _cd=((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)).getContent();
        _cc=((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)).getContent();
    }

    public Map<String, Object> searchByCrawl(String searchStr, int resultType, int pageType) {
        String __s[]=searchStr.split(",");
        String _s[]=new String[__s.length];
        for (int i=0; i<__s.length; i++) _s[i]=__s[i].trim();
        //wbq 考拉搜索        
        Map<String, Object> map_kl = new HashMap<String,Object>();

        //按照0::0处理
        for(int i = 0;i<_s.length;i++){
        	Map<String, Object> map_kl_s =  kl_s.kaolaService(_s[i]);
	        List<Festival> list_kl_festival = (List<Festival>) map_kl_s.get("KL_F");
	        List<Station> list_kl_station = (List<Station>) map_kl_s.get("KL_S");
	        map_kl.put("AllCount", list_kl_festival.size()+list_kl_station.size());
	        map_kl.put("List", dataT.datas2Audio(list_kl_festival,list_kl_station,0));
	        map_kl.put("ResultType", resultType);
        }
   
        return map_kl;
    }
    /**
     * 查找内容，此内容无排序，按照创建时间的先后顺序排序，最新的在最前面
     * @param searchStr 查找串
     * @param resultType 返回类型,0把所有结果按照一个列表返回；1按照“单体节目、系列节目、电台”的顺序分类给出
     * @return 创建用户成功返回1，否则返回0
     */
    public Map<String, Object> searchAll(String searchStr, int resultType, int pageType) {
        String __s[]=searchStr.split(",");
        String _s[]=new String[__s.length];
        for (int i=0; i<__s.length; i++) _s[i]=__s[i].trim();

        List<Map<String, Object>> ret1=new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> ret2=new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> ret3=new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> ret4=new ArrayList<Map<String, Object>>();//只有当pageType=0时，此列表才有用

        Map<String, List<String>> typeMap=new HashMap<String, List<String>>();
        Map<String, List<String>> reBuildMap=new HashMap<String, List<String>>();
        Map<String, Object> paraM=new HashMap<String, Object>();

        //0.1-查找分类
        Connection conn=null;
        PreparedStatement ps=null;
        PreparedStatement ps2=null;
        ResultSet rs=null;
        String sql="select * from wt_ResDict_Ref where ";
        for (int k=0; k<_s.length; k++) {
            if (k==0) sql+=" title like '%"+_s[k]+"%'";
            else sql+=" or title like '%"+_s[k]+"%'";
        }
        List<Map<String, Object>> cataList=null;
        try {
            conn=dataSource.getConnection();
            ps=conn.prepareStatement(sql+" limit 0, 10");
            rs=ps.executeQuery();
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
            if (ps!=null) try {ps.close();ps=null;} catch(Exception e) {ps=null;} finally {ps=null;};
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
            typeMap.get(resTableName).add(one.get("resId")+"");
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
            ps=conn.prepareStatement(sql);
            ps.setQueryTimeout(10);
            rs=ps.executeQuery();
            tempList=new ArrayList<Map<String, Object>>();
            while (rs!=null&&rs.next()) {
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
                add(ret2, oneData);
                if (reBuildMap.get("wt_MediaAsset")==null) reBuildMap.put("wt_MediaAsset", new ArrayList<String>());
                reBuildMap.get("wt_MediaAsset").add(oneData.get("id")+"");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
            if (ps!=null) try {ps.close();ps=null;} catch(Exception e) {ps=null;} finally {ps=null;};
            if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
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
            ps=conn.prepareStatement(sql);
            ps.setQueryTimeout(10);
            rs=ps.executeQuery();
            tempList=new ArrayList<Map<String, Object>>();
            String _orSql="";
            while (rs!=null&&rs.next()) {
                Map<String, Object> oneData=new HashMap<String, Object>();
                oneData.put("id", rs.getString("id"));
                if (pageType==0) _orSql+=" or sma.sId='"+rs.getString("id")+"'";
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
                add(ret3, oneData);
                if (reBuildMap.get("wt_SeqMediaAsset")==null) reBuildMap.put("wt_SeqMediaAsset", new ArrayList<String>());
                reBuildMap.get("wt_SeqMediaAsset").add(oneData.get("id")+"");
            }
            rs.close(); rs=null;
            if (pageType==0&&!StringUtils.isNullOrEmptyOrSpace(_orSql)) {//为提升速度，提取单体节目
                _orSql="select sma.sId, ma.* from wt_MediaAsset as ma, (select * from( select * from  wt_SeqMA_Ref where columnNum=(select b.columnNum from vWt_FirstMaInSequ as b where wt_SeqMA_Ref.sId=b.sId)) as a group by a.sId) as sma"
                      +" where ma.id=sma.mId and ("+_orSql.substring(4)+")";
                ps2=conn.prepareStatement(_orSql);
                ps.setQueryTimeout(10);
                rs=ps2.executeQuery();
                while (rs!=null&&rs.next()) {
                    String maId=rs.getString("id");
                    boolean find=false;
                    for (Map<String, Object> _o2: ret2) {
                        if (maId.equals(_o2.get("id"))) {
                            find=true;
                            _o2.put("sId", rs.getString("sId"));
                            break;
                        }
                    }
                    if (!find) {
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
                        oneData.put("sId", rs.getString("sId"));
                        add(ret4, oneData);
                        if (reBuildMap.get("wt_MediaAsset")==null) reBuildMap.put("wt_MediaAsset", new ArrayList<String>());
                        reBuildMap.get("wt_MediaAsset").add(maId+"");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
            if (ps!=null) try {ps.close();ps=null;} catch(Exception e) {ps=null;} finally {ps=null;};
            if (ps2!=null) try {ps2.close();ps2=null;} catch(Exception e) {ps2=null;} finally {ps2=null;};
            if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
        }

        if ((ret1==null||ret1.size()==0)&&(ret2==null||ret2.size()==0)&&(ret3==null||ret3.size()==0)) return null;

        //重构人员及分类列表
        paraM.clear();
        if (reBuildMap.get("wt_Broadcast")!=null&&reBuildMap.get("wt_Broadcast").size()>0) paraM.put("bcIds", getIds(reBuildMap.get("wt_Broadcast")));
        if (reBuildMap.get("wt_MediaAsset")!=null&&reBuildMap.get("wt_MediaAsset").size()>0) paraM.put("maIds", getIds(reBuildMap.get("wt_MediaAsset")));
        if (reBuildMap.get("wt_SeqMediaAsset")!=null&&reBuildMap.get("wt_SeqMediaAsset").size()>0) paraM.put("smaIds", getIds(reBuildMap.get("wt_SeqMediaAsset")));

        //重构人员
        personList=groupDao.queryForListAutoTranform("refPersonById", paraM);
        //重构分类
        cataList=groupDao.queryForListAutoTranform("refCataById", paraM);

        Map<String, Object> ret=new HashMap<String, Object>();
        ret.put("ResultType", resultType);
        Map<String, Object> oneMedia;
        int i=0;
        if (resultType==0) {//按一个列表获得
            List<Map<String, Object>> allList=new ArrayList<Map<String, Object>>();
            if (ret1!=null||ret1.size()>0) {
                for (; i<ret1.size(); i++) {
                    oneMedia=convert2MediaMap_1(ret1.get(i), cataList, personList);
                    add(allList, oneMedia);
                }
            }
            if (ret2!=null||ret2.size()>0) {
                for (i=0; i<ret2.size(); i++) {
                    oneMedia=convert2MediaMap_2(ret2.get(i), cataList, personList);
                    if (pageType==0&&ret2.get(i).get("sId")!=null) {
                        for (int j=0; j<ret3.size(); j++) {
                            if (ret3.get(j).get("id").equals(ret2.get(i).get("sId"))) {
                                Map<String, Object> _oneMedia=convert2MediaMap_3(ret3.get(j), cataList, personList);
                                oneMedia.put("SeqInfo", _oneMedia);
                            }
                        }
                    }
                    add(allList, oneMedia);
                }
            }
            if (pageType==0&&ret4!=null) {
                for (i=0; i<ret4.size(); i++) {
                    oneMedia=convert2MediaMap_2(ret4.get(i), cataList, personList);
                    for (int j=0; j<ret3.size(); j++) {
                        if (ret3.get(j).get("id").equals(ret4.get(i).get("sId"))) {
                            Map<String, Object> _oneMedia=convert2MediaMap_3(ret3.get(i), cataList, personList);
                            oneMedia.put("SeqInfo", _oneMedia);
                        }
                    }
                    add(allList, oneMedia);
                }
            } else {
                if (ret3!=null||ret3.size()>0) {//系列节目
                    for (i=0; i<ret3.size(); i++) {
                        oneMedia=convert2MediaMap_3(ret3.get(i), cataList, personList);
                        add(allList, oneMedia);
                    }
                }
            }
            ret.put("List", allList);
            ret.put("AllCount", allList.size());
        } else if (resultType==2) {//按分类列表获得
            ret.put("AllCount", (ret1==null?0:ret1.size())+(ret2==null?0:ret2.size())+(ret3==null?0:ret3.size()));
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
            if (ret3!=null||ret3.size()>0) {//系列节目
                List<Map<String, Object>> resultList3=new ArrayList<Map<String, Object>>();
                if (pageType==0) {
                    if (ret2!=null||ret2.size()>0) {
                        for (i=0; i<ret2.size(); i++) {
                            if (pageType==0&&ret2.get(i).get("sId")!=null) {
                                for (int j=0; j<ret3.size(); j++) {
                                    if (ret3.get(j).get("id").equals(ret2.get(i).get("sId"))) {
                                        oneMedia=convert2MediaMap_2(ret2.get(i), cataList, personList);
                                        Map<String, Object> _oneMedia=convert2MediaMap_3(ret3.get(j), cataList, personList);
                                        oneMedia.put("SeqInfo", _oneMedia);
                                        resultList3.add(oneMedia);
                                    }
                                }
                            }
                        }
                    }
                    if (ret4!=null||ret4.size()>0) {
                        for (i=0; i<ret4.size(); i++) {
                            oneMedia=convert2MediaMap_2(ret4.get(i), cataList, personList);
                            for (int j=0; j<ret3.size(); j++) {
                                if (ret3.get(j).get("id").equals(ret4.get(i).get("sId"))) {
                                    Map<String, Object> _oneMedia=convert2MediaMap_3(ret3.get(i), cataList, personList);
                                    oneMedia.put("SeqInfo", _oneMedia);
                                }
                            }
                            resultList3.add(oneMedia);
                        }
                    }
                } else {
                    for (i=0; i<ret3.size(); i++) {
                        oneMedia=convert2MediaMap_3(ret3.get(i), cataList, personList);
                        resultList3.add(oneMedia);
                    }
                }
                Map<String, Object> smResult=new HashMap<String, Object>();
                smResult.put("Count", ret3.size());
                smResult.put("List", resultList3);
                ret.put("smResult", smResult);
            }
        }
        //对返回信息中的专辑进行处理
        
        return ret;
    }
    /**
     * 获得主页信息
     * @param userId
     * @return
     */
    public Map<String, Object> getMainPage(String userId, int pageType, int pageSize, int page) {
        return getContents("-1", null, 0, null, 3, pageSize, page, null, pageType);
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
        paraM.put("resTableName", "wt_SeqMediaAsset");
        paraM.put("ids", "'"+contentId+"'");
        cataList=groupDao.queryForListAutoTranform("getCataListByTypeAndIds", paraM);
        personList=groupDao.queryForListAutoTranform("getPersonListByTypeAndIds", paraM);
        Map<String, Object>  retInfo=convert2MediaMap_3(tempMap, cataList, personList);

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

    public Map<String, Object> getContents(String catalogType, String catalogId, int resultType, String mediaType, int perSize, int pageSize, int page, String beginCatalogId, int pageType) {
        Map<String, Object> ret=new HashMap<String, Object>();
        //首先根据参数获得范围
        //根据分类获得根
        TreeNode<? extends TreeNodeBean> root=null;
        if (catalogType.equals("-1")) {
            root=_cc.channelTree;
        } else {
            DictModel dm=_cd.getDictModelById(catalogType);
            if (dm!=null&&dm.dictTree!=null) root=dm.dictTree;
        }
        //获得相应的结点，通过查找
        if (root!=null&&catalogId!=null) root=root.findNode(catalogId);
        if (root==null) return null;
        if (root.isLeaf()) resultType=0;

        //得到分类id的语句
        String idCName="dictDid", typeCName="resTableName", resIdCName="resId";
        if (catalogType.equals("-1")) {
            idCName="channelId";typeCName="assetType";resIdCName="assetId";
        }

        //得到媒体类型过滤串
        String mediaFilterSql="";
        if (!StringUtils.isNullOrEmptyOrSpace(mediaType)) {
            String[] _mt=mediaType.split(",");
            for (int i=0; i<_mt.length; i++) {
                if (_mt[i].equals("RADIO")&&(mediaFilterSql.indexOf("wt_Broadcast")==-1)) {
                    mediaFilterSql+="or "+typeCName+"='wt_Broadcast'";
                } else if (_mt[i].equals("AUDIO")&&(mediaFilterSql.indexOf("wt_MediaAsset")==-1)) {
                    mediaFilterSql+="or "+typeCName+"='wt_MediaAsset'";
                } else if (_mt[i].equals("SEQU")&&(mediaFilterSql.indexOf("wt_SeqMediaAsset")==-1)) {
                    mediaFilterSql+="or "+typeCName+"='wt_SeqMediaAsset'";
                }
            }
            if (mediaFilterSql.indexOf("wt_Broadcast")!=-1&&mediaFilterSql.indexOf("wt_MediaAsset")!=-1&&mediaFilterSql.indexOf("wt_SeqMediaAsset")!=-1) mediaFilterSql="";
            else mediaFilterSql=mediaFilterSql.substring(3);
        }

        if (resultType==0) { //按列表处理
            //得到所有下级结点的Id
            List<TreeNode<? extends TreeNodeBean>> allTn=com.woting.common.TreeUtils.getDeepList(root);
            //得到分类id的语句
            String orSql="";
            orSql+=" or "+idCName+"='"+root.getId()+"'";
            if (allTn!=null&&!allTn.isEmpty()) {
                for (TreeNode<? extends TreeNodeBean> tn: allTn) {
                    orSql+=" or "+idCName+"='"+tn.getId()+"'";
                }
            }
            orSql=orSql.substring(4);

            //得到获得内容Id的Sql
            String sql="select * from wt_ChannelAsset where isValidate=1 and flowFlag=2 and ("+orSql+")";//栏目
            if (!catalogType.equals("-1")) {//分类
                sql="select * from wt_ResDict_Ref where dictMid="+catalogType;
                sql+=" and ("+orSql+")";
            }
            if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
            String sqlCount="select count(*)"+sql.substring(8);
            if (catalogType.equals("-1")) sql+=" order by sort desc, pubTime desc";//栏目
            else sql+=" order by cTime desc";//分类
            sql+=" limit "+(((page<=0?1:page)-1)*pageSize)+","+pageSize; //分页

            //执行得到具体内容Id的SQL
            List<String> sortIdList=new ArrayList<String>();
            String bcSqlSign="", maSqlSign="", smaSqlSign="";
            String bcSqlSign1="", maSqlSign1="", smaSqlSign1="";
            Connection conn=null;
            PreparedStatement ps=null;//获得所需的记录的id
            PreparedStatement ps1=null;//如果需要提取专辑中的第一条记录，按此处理
            ResultSet rs=null;
            try {
                conn=dataSource.getConnection();
                //获得总条数
                long count=0l;
                ps=conn.prepareStatement(sqlCount);
                rs=ps.executeQuery(sqlCount);
                while (rs!=null&&rs.next()) {
                    count=rs.getLong(1);
                }
                rs.close(); rs=null;
                ps.close(); ps=null;
                String sma2msInsql="";
                //获得记录
                ps=conn.prepareStatement(sql);
                rs=ps.executeQuery();
                while (rs!=null&&rs.next()) {
                    sortIdList.add(rs.getString(typeCName)+"::"+rs.getString(resIdCName));
                    if (rs.getString(typeCName).equals("wt_Broadcast")) {
                        bcSqlSign+=" or a.id='"+rs.getString(resIdCName)+"'";
                        bcSqlSign1+=",'"+rs.getString(resIdCName)+"'";
                    } else if (rs.getString(typeCName).equals("wt_MediaAsset")) {
                        maSqlSign+=" or id='"+rs.getString(resIdCName)+"'";
                        maSqlSign1+=",'"+rs.getString(resIdCName)+"'";
                    } else if (rs.getString(typeCName).equals("wt_SeqMediaAsset")) {
                        smaSqlSign+=" or id='"+rs.getString(resIdCName)+"'";
                        smaSqlSign1+=",'"+rs.getString(resIdCName)+"'";
                        if (pageType==0) sma2msInsql+=" or sma.sId='"+rs.getString(resIdCName)+"'";
                    }
                }
                rs.close(); rs=null;
                ps.close(); ps=null;
                if (sortIdList!=null&&!sortIdList.isEmpty()) {
                    List<Map<String, Object>> ret4=new ArrayList<Map<String, Object>>();//只有当pageType=0时，此列表才有用
                    boolean samExtractHas=false;
                    if (sma2msInsql.length()>0) {
                        String _orSql="select sma.sId, ma.* from wt_MediaAsset as ma, (select * from( select * from  wt_SeqMA_Ref where columnNum=(select b.columnNum from vWt_FirstMaInSequ as b where wt_SeqMA_Ref.sId=b.sId)) as a group by a.sId) as sma"
                                +" where ma.id=sma.mId and ("+sma2msInsql.substring(4)+")";
                        //获得提取出的单体节目
                        String _smaSqlSign1="";
                        ps1=conn.prepareStatement(_orSql);
                        rs=ps1.executeQuery();
                        while (rs!=null&&rs.next()) {
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
                            oneData.put("sId", rs.getString("sId"));
                            add(ret4, oneData);
                            if (maSqlSign1.indexOf(""+rs.getString("id"))>1) {
                                _smaSqlSign1+=","+rs.getString("id");
                                int pos=maSqlSign.indexOf(""+rs.getString("id"));
                                maSqlSign=maSqlSign.substring(0, pos-1)+maSqlSign.substring(pos+(""+rs.getString("id")).length());
                            }
                        }
                        if (_smaSqlSign1.length()>0) maSqlSign1=maSqlSign1+_smaSqlSign1;
                        rs.close(); rs=null;
                        ps1.close(); ps1=null;
                        samExtractHas=!ret4.isEmpty();
                    }

                    Map<String, Object> paraM=new HashMap<String, Object>();
                    if (!StringUtils.isNullOrEmptyOrSpace(bcSqlSign)) {
                        bcSqlSign=bcSqlSign.substring(4);
                        paraM.put("bcIds", bcSqlSign1.substring(1));
                    }
                    if (!StringUtils.isNullOrEmptyOrSpace(maSqlSign)) {
                        maSqlSign=maSqlSign.substring(4);
                        paraM.put("maIds", maSqlSign1.substring(1));
                    }
                    if (!StringUtils.isNullOrEmptyOrSpace(smaSqlSign)) {//专辑处理
                        smaSqlSign=smaSqlSign.substring(4);
                        paraM.put("smaIds", smaSqlSign1.substring(1));
                    }
                    //重构人员及分类列表
                    List<Map<String, Object>> cataList=groupDao.queryForListAutoTranform("refCataById", paraM);

                    List<Map<String, Object>> _ret=new ArrayList<Map<String, Object>>();
                    for (int j=0; j<sortIdList.size(); j++) _ret.add(null);

                    if (!StringUtils.isNullOrEmptyOrSpace(bcSqlSign)) {
                        ps=conn.prepareStatement("select a.*, b.bcSource, b.flowURI from wt_Broadcast a left join wt_BCLiveFlow b on a.id=b.bcId and b.isMain=1 where "+bcSqlSign);
                        rs=ps.executeQuery();
                        while (rs!=null&&rs.next()) {
                            Map<String, Object> oneData=new HashMap<String, Object>();
                            oneData.put("id", rs.getString("id"));
                            oneData.put("bcTitle", rs.getString("bcTitle"));
                            oneData.put("bcPubType", rs.getInt("bcPubType"));
                            oneData.put("bcPubId", rs.getString("bcPubId"));
                            oneData.put("bcPublisher", rs.getString("bcPublisher"));
                            oneData.put("bcImg", rs.getString("bcImg"));
                            oneData.put("bcURL", rs.getString("bcURL"));
                            oneData.put("descn", rs.getString("descn"));
                            oneData.put("pubCount", rs.getInt("pubCount"));
                            oneData.put("bcSource", rs.getString("bcSource"));
                            oneData.put("flowURI", rs.getString("flowURI"));
                            oneData.put("cTime", rs.getTimestamp("cTime"));

                            Map<String, Object> oneMedia=convert2MediaMap_1(oneData, cataList, null);
                            int i=0;
                            for (; i<sortIdList.size(); i++) {
                                if (sortIdList.get(i).equals("wt_Broadcast::"+oneMedia.get("ContentId"))) break;
                            }
                            _ret.set(i, oneMedia);
                        }
                        rs.close(); rs=null;
                        ps.close(); ps=null;
                    }
                    if (!StringUtils.isNullOrEmptyOrSpace(maSqlSign)) {
                        ps=conn.prepareStatement("select * from wt_MediaAsset where "+maSqlSign);
                        rs=ps.executeQuery();
                        while (rs!=null&&rs.next()) {
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

                            Map<String, Object> oneMedia=convert2MediaMap_2(oneData, cataList, null);
                            int i=0;
                            for (; i<sortIdList.size(); i++) {
                                if (sortIdList.get(i).equals("wt_MediaAsset::"+oneMedia.get("ContentId"))) break;
                            }
                            _ret.set(i, oneMedia);
                        }
                        rs.close(); rs=null;
                        ps.close(); ps=null;
                    }
                    if (!StringUtils.isNullOrEmptyOrSpace(smaSqlSign)) {
                        ps=conn.prepareStatement("select a.*, case when b.count is null then 0 else b.count end as count from wt_SeqMediaAsset a left join (select sid, count(*) count from wt_SeqMA_Ref group by sid) b on a.id=b.sid where "+smaSqlSign);
                        rs=ps.executeQuery();
                        while (rs!=null&&rs.next()) {
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

                            Map<String, Object> oneMedia=convert2MediaMap_3(oneData, cataList, null);
                            int i=0;
                            for (; i<sortIdList.size(); i++) {
                                if (sortIdList.get(i).equals("wt_SeqMediaAsset::"+oneMedia.get("ContentId"))) break;
                            }
                            boolean hasAdd=false;
                            if (pageType==0&&samExtractHas) {
                                for (Map<String, Object> _o: ret4) {
                                    if ((""+oneMedia.get("ContentId")).equals(""+_o.get("sId"))) {
                                        Map<String, Object> newOne=convert2MediaMap_2(_o, cataList, null);
                                        newOne.put("SeqInfo", oneMedia);
                                        _ret.set(i, newOne);
                                        hasAdd=true;
                                    }
                                }
                            }
                            if (!hasAdd) _ret.set(i, oneMedia);
                        }
                        rs.close(); rs=null;
                        ps.close(); ps=null;
                    }
                    ret.put("ResultType", resultType);
                    ret.put("AllCount", count);
                    ret.put("Page", page);
                    ret.put("PageSize", _ret.size());
                    ret.put("List", _ret);
                    return ret;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
                if (ps!=null) try {ps.close();ps=null;} catch(Exception e) {ps=null;} finally {ps=null;};
                if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
            }
        } else if (resultType==2){ //按媒体类型
            //TODO 暂不实现
        } else if (resultType==1){ //按下级分类
            int pageCount=0;
            List<Map<String, Object>> retCataList=new ArrayList<Map<String, Object>>();

            Connection conn=null;
            PreparedStatement ps1=null;
            PreparedStatement ps2=null;
            PreparedStatement psBc=null;
            PreparedStatement psMa=null;
            PreparedStatement psSma=null;
            ResultSet rs=null;
            String sql="", sqlCount="", sqlBc="select a.*, b.bcSource, b.flowURI from wt_Broadcast a left join wt_BCLiveFlow b on a.id=b.bcId and b.isMain=1 where SQL"
                    , sqlMa="select * from wt_MediaAsset where SQL", sqlSma="select a.*, case when b.count is null then 0 else b.count end as count from wt_SeqMediaAsset a left join (select sid, count(*) count from wt_SeqMA_Ref group by sid) b on a.id=b.sid where SQL";
            if (catalogType.equals("-1")) {
                sql="select * from wt_ChannelAsset where isValidate=1 and flowFlag=2 and (SQL) "+(StringUtils.isNullOrEmptyOrSpace(mediaType)?"":(" and ("+mediaFilterSql+")"))+"order by sort desc, pubTime desc limit "+perSize;
                sqlCount="select count(*) from wt_ChannelAsset where isValidate=1 and flowFlag=2 and (SQL) "+(StringUtils.isNullOrEmptyOrSpace(mediaType)?"":(" and ("+mediaFilterSql+")"));
            } else {
                sql="select * from wt_ResDict_Ref where dictMid="+catalogType+" and (SQL) "+(StringUtils.isNullOrEmptyOrSpace(mediaType)?"":(" and ("+mediaFilterSql+")"))+"order by cTime desc limit "+perSize;
                sqlCount="select count(*) from wt_ResDict_Ref where dictMid="+catalogType+" and (SQL) "+(StringUtils.isNullOrEmptyOrSpace(mediaType)?"":(" and ("+mediaFilterSql+")"));
            }
            try {
                conn=dataSource.getConnection();
                ps1=conn.prepareStatement(sql);
                ps2=conn.prepareStatement(sqlCount);
                psBc=conn.prepareStatement(sqlBc);
                psMa=conn.prepareStatement(sqlMa);
                psSma=conn.prepareStatement(sqlSma);

                //这是一个复杂的结构，一层层的处理下去
                boolean getBeginCatalogId=false, isBegin=false;
                if (beginCatalogId==null) page=1;
                for (TreeNode<? extends TreeNodeBean> _stn: root.getChildren()) {
                    if (!(_stn.getId().equals(beginCatalogId))&&beginCatalogId!=null&&!isBegin) continue;
                    isBegin=true;
                    if (beginCatalogId!=null) if (beginCatalogId.equals("ENDEND")) break;

                    if (!getBeginCatalogId) {
                        String bcSqlSign="", maSqlSign="", smaSqlSign="";
                        String bcSqlSign1="", maSqlSign1="", smaSqlSign1="";
                        //开始循环处理
                        String tempStr=idCName+"='"+_stn.getId()+"'";
                        List<TreeNode<? extends TreeNodeBean>> subAllTn=com.woting.common.TreeUtils.getDeepList(_stn);
                        if (subAllTn!=null&&!subAllTn.isEmpty()) {
                            for (TreeNode<? extends TreeNodeBean> tn: subAllTn) {
                                tempStr+=" or "+idCName+"='"+tn.getId()+"'";
                            }
                        }
                        long count=0l;
                        rs=ps2.executeQuery(sqlCount.replaceAll("SQL", tempStr));
                        while (rs!=null&&rs.next()) {
                            count=rs.getLong(1);
                        }
                        rs.close();rs=null;
                        if (count==0) continue;

                        rs=ps1.executeQuery(sql.replaceAll("SQL", tempStr));
                        List<String> sortIdList=new ArrayList<String>();
                        while (rs!=null&&rs.next()) {
                            sortIdList.add(rs.getString(typeCName)+"::"+rs.getString(resIdCName));
                            if (rs.getString(typeCName).equals("wt_Broadcast")) {
                                bcSqlSign+=" or a.id='"+rs.getString(resIdCName)+"'";
                                bcSqlSign1+=",'"+rs.getString(resIdCName)+"'";
                            } else if (rs.getString(typeCName).equals("wt_MediaAsset")) {
                                maSqlSign+=" or id='"+rs.getString(resIdCName)+"'";
                                maSqlSign1+=",'"+rs.getString(resIdCName)+"'";
                            } else if (rs.getString(typeCName).equals("wt_SeqMediaAsset")) {
                                smaSqlSign+=" or id='"+rs.getString(resIdCName)+"'";
                                smaSqlSign1+=",'"+rs.getString(resIdCName)+"'";
                            }
                        }
                        rs.close();rs=null;

                        if (sortIdList!=null&&!sortIdList.isEmpty()) {
                            Map<String, Object> paraM=new HashMap<String, Object>();
                            if (!StringUtils.isNullOrEmptyOrSpace(bcSqlSign)) {
                                bcSqlSign=bcSqlSign.substring(4);
                                paraM.put("bcIds", bcSqlSign1.substring(1));
                            }
                            if (!StringUtils.isNullOrEmptyOrSpace(maSqlSign)) {
                                maSqlSign=maSqlSign.substring(4);
                                paraM.put("maIds", maSqlSign1.substring(1));
                            }
                            if (!StringUtils.isNullOrEmptyOrSpace(smaSqlSign)) {
                                smaSqlSign=smaSqlSign.substring(4);
                                paraM.put("smaIds", smaSqlSign1.substring(1));
                            }
                            //重构人员及分类列表
                            List<Map<String, Object>> cataList=groupDao.queryForListAutoTranform("refCataById", paraM);

                            List<Map<String, Object>> _ret=new ArrayList<Map<String, Object>>();
                            for (int j=0; j<sortIdList.size(); j++) {
                                _ret.add(null);
                            }
                            if (!StringUtils.isNullOrEmptyOrSpace(bcSqlSign)) {
                                rs=psBc.executeQuery(sqlBc.replace("SQL", bcSqlSign));
                                while (rs!=null&&rs.next()) {
                                    Map<String, Object> oneData=new HashMap<String, Object>();
                                    oneData.put("id", rs.getString("id"));
                                    oneData.put("bcTitle", rs.getString("bcTitle"));
                                    oneData.put("bcPubType", rs.getInt("bcPubType"));
                                    oneData.put("bcPubId", rs.getString("bcPubId"));
                                    oneData.put("bcPublisher", rs.getString("bcPublisher"));
                                    oneData.put("bcImg", rs.getString("bcImg"));
                                    oneData.put("bcURL", rs.getString("bcURL"));
                                    oneData.put("descn", rs.getString("descn"));
                                    oneData.put("pubCount", rs.getInt("pubCount"));
                                    oneData.put("bcSource", rs.getString("bcSource"));
                                    oneData.put("flowURI", rs.getString("flowURI"));
                                    oneData.put("cTime", rs.getTimestamp("cTime"));

                                    Map<String, Object> oneMedia=convert2MediaMap_1(oneData, cataList, null);
                                    int i=0;
                                    for (; i<sortIdList.size(); i++) {
                                        if (sortIdList.get(i).equals("wt_Broadcast::"+oneMedia.get("ContentId"))) break;
                                    }
                                    _ret.set(i, oneMedia);
                                }
                                rs.close(); rs=null;
                            }
                            if (!StringUtils.isNullOrEmptyOrSpace(maSqlSign)) {
                                rs=psMa.executeQuery(sqlMa.replace("SQL", maSqlSign));
                                while (rs!=null&&rs.next()) {
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

                                    Map<String, Object> oneMedia=convert2MediaMap_2(oneData, cataList, null);
                                    int i=0;
                                    for (; i<sortIdList.size(); i++) {
                                        if (sortIdList.get(i).equals("wt_MediaAsset::"+oneMedia.get("ContentId"))) break;
                                    }
                                    _ret.set(i, oneMedia);
                                }
                                rs.close(); rs=null;
                            }
                            if (!StringUtils.isNullOrEmptyOrSpace(smaSqlSign)) {
                                rs=psSma.executeQuery(sqlSma.replace("SQL", smaSqlSign));
                                while (rs!=null&&rs.next()) {
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

                                    Map<String, Object> oneMedia=convert2MediaMap_3(oneData, cataList, null);
                                    int i=0;
                                    for (; i<sortIdList.size(); i++) {
                                        if (sortIdList.get(i).equals("wt_SeqMediaAsset::"+oneMedia.get("ContentId"))) break;
                                    }
                                    _ret.set(i, oneMedia);
                                }
                            }

                            Map<String, Object> oneCatalog=new HashMap<String, Object>();
                            oneCatalog.put("CatalogType", catalogType);
                            oneCatalog.put("CatalogId", _stn.getId());
                            oneCatalog.put("CatalogName", _stn.getNodeName());
                            oneCatalog.put("AllCount",count);
                            oneCatalog.put("List", _ret);
                            retCataList.add(oneCatalog);
                            pageCount+=sortIdList.size();
                        }
                        if (pageCount>=pageSize) {
                            getBeginCatalogId=true;
                        }
                    } else {
                        beginCatalogId=_stn.getId();
                        break;
                    }
                }
                if (pageCount<pageSize) beginCatalogId="ENDEND";
                ret.put("BeginCatalogId", beginCatalogId);
                ret.put("ResultType", resultType);
                ret.put("Page", page);
                ret.put("PageSize", pageCount);
                ret.put("List", retCataList);
                return ret;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
                if (ps1!=null) try {ps1.close();ps1=null;} catch(Exception e) {ps1=null;} finally {ps1=null;};
                if (ps2!=null) try {ps2.close();ps2=null;} catch(Exception e) {ps2=null;} finally {ps2=null;};
                if (psBc!=null) try {psBc.close();psBc=null;} catch(Exception e) {psBc=null;} finally {psBc=null;};
                if (psMa!=null) try {psMa.close();psMa=null;} catch(Exception e) {psMa=null;} finally {psMa=null;};
                if (psSma!=null) try {psSma.close();psSma=null;} catch(Exception e) {psSma=null;} finally {psSma=null;};
                if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
            }
        }
        return null;
    }

    //私有方法
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
        retM.put("PlayCount", "1234");//P14-公共：播放次数

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
        retM.put("ContentURI", "content/getContentInfo.do?MediaType=AUDIO&ContentId="+retM.get("ContentId"));//P08-公共：主播放Url，这个应该从其他地方来，现在先这样//TODO
//        retM.put("ContentSource", one.get("maSource"));//P09-公共：来源名称
//        retM.put("ContentURIS", null);//P10-公共：其他播放地址列表，目前为空
        retM.put("ContentDesc", one.get("descn"));//P11-公共：说明
        retM.put("ContentPersons", fetchPersons(personList, 2, retM.get("ContentId")+""));//P12-公共：相关人员列表
        retM.put("ContentCatalogs", fetchCatas(cataList, 2, retM.get("ContentId")+""));//P13-公共：所有分类列表
        retM.put("PlayCount", "1234");//P14-公共：播放次数

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
        retM.put("ContentURI", "content/getContentInfo.do?MediaType=SEQU&ContentId="+retM.get("ContentId"));//P08-公共：在此是获得系列节目列表的Url
        retM.put("ContentDesc", one.get("descn"));//P11-公共：说明
        retM.put("ContentPersons", fetchPersons(personList, 3, retM.get("ContentId")+""));//P12-公共：相关人员列表
        retM.put("ContentCatalogs", fetchCatas(cataList, 3, retM.get("ContentId")+""));//P13-公共：所有分类列表
        retM.put("PlayCount", "1234");//P14-公共：播放次数

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

}
//测试的消息{"IMEI":"12356","UserId":"107fc906ae0f","ResultType":"0","SearchStr":"罗,电影,安徽"}=
//http://localhost:808/wt/searchByVoice.do
//http://localhost:808/wt/content/getSeqMaInfo.do