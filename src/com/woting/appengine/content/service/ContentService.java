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
import com.spiritdata.framework.util.TreeUtils;
import com.woting.WtAppEngineConstants;
import com.woting.cm.core.utils.ContentUtils;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.cm.core.channel.mem._CacheChannel;
import com.woting.cm.core.channel.service.ChannelService;
import com.woting.cm.core.dict.mem._CacheDictionary;
import com.woting.cm.core.dict.model.DictModel;
import com.woting.favorite.persis.po.UserFavoritePo;
import com.woting.favorite.service.FavoriteService;
import com.woting.passport.UGA.persistence.pojo.GroupPo;

@Lazy(true)
@Service
public class ContentService {
    //先用Group代替！！
    @Resource(name="defaultDAO")
    private MybatisDAO<GroupPo> groupDao;
    @Resource
    private DataSource dataSource;
    @Resource
    private FavoriteService favoriteService;
    @Resource
    private ChannelService channelService;

    private _CacheDictionary _cd=null;
    private _CacheChannel _cc=null;

    @PostConstruct
    public void initParam() {
        groupDao.setNamespace("WT_GROUP");
        _cd=(SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)==null?null:((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)).getContent());
        _cc=(SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)==null?null:((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)).getContent());
    }

    /**
     * 查找内容，此内容无排序，按照创建时间的先后顺序排序，最新的在最前面
     * @param searchStr 查找串
     * @param resultType 返回类型,0把所有结果按照一个列表返回；1按照“单体节目、系列节目、电台”的顺序分类给出
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return 创建用户成功返回1，否则返回0
     */
    public Map<String, Object> searchAll(String searchStr, int resultType, int pageType, MobileKey mk) {
        //得到喜欢列表
        List<UserFavoritePo> _fList=favoriteService.getPureFavoriteList(mk);
        List<Map<String, Object>> fList=null;
        if (_fList!=null&&!_fList.isEmpty()) {
            fList=new ArrayList<Map<String, Object>>();
            for (UserFavoritePo ufPo: _fList) {
                fList.add(ufPo.toHashMapAsBean());
            }
        }

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
        String sql="select wt_ResDict_Ref.*, plat_DictD.ddName title from wt_ResDict_Ref, plat_DictD where wt_ResDict_Ref.dictMid=plat_DictD.mId and wt_ResDict_Ref.dictDid=plat_DictD.id";
        String likeSql="";
        for (int k=0; k<_s.length; k++) {
            if (k==0) sql+=" and (plat_DictD.ddName like '%"+_s[k]+"%'";
            else sql+=" or plat_DictD.ddName like '%"+_s[k]+"%'";
        }
        List<Map<String, Object>> cataList=null;
        try {
            conn=dataSource.getConnection();
            ps=conn.prepareStatement(sql+") limit 0, 10");
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
        List<Map<String, Object>> assetList=new ArrayList<Map<String, Object>>();
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

            boolean canAdd=true;
            if (assetList!=null&&!assetList.isEmpty()) {
                for (Map<String, Object> _anAsset: assetList) {
                    if ((_anAsset.get("resTableName")+"").equals("wt_Broadcast")&&(_anAsset.get("resId")+"").equals(tempList.get(i).get("id"))) {
                        canAdd=false;
                        break;
                    }
                }
            }
            if (canAdd) {
                Map<String, Object> oneAsset=new HashMap<String, Object>();
                oneAsset.put("resId", tempList.get(i).get("id"));
                oneAsset.put("resTableName", "wt_Broadcast");
                assetList.add(oneAsset);
            }
        }
        //2-查找单体节目
        tempStr=getIds(typeMap.get("wt_MediaAsset"));
        sql="select a.* from wt_MediaAsset a where ";
        likeSql="";
        for (int k=0; k<_s.length; k++) {
            if (k!=0) likeSql+=" or ";
            likeSql+="(CONCAT(IFNULL(a.maTitle,''),'#S#',IFNULL(a.maPublisher,''),'#S#',IFNULL(a.subjectWords,''),'#S#',IFNULL(a.keyWords,''),'#S#',IFNULL(a.descn,'')) like '%"+_s[k]+"%')";
        }
        sql+=(likeSql.length()>0?"("+likeSql+")":"")+" limit 0, 20";
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

                boolean canAdd=true;
                if (assetList!=null&&!assetList.isEmpty()) {
                    for (Map<String, Object> _anAsset: assetList) {
                        if ((_anAsset.get("resTableName")+"").equals("wt_MediaAsset")&&(_anAsset.get("resId")+"").equals(rs.getString("id"))) {
                            canAdd=false;
                            break;
                        }
                    }
                }
                if (canAdd) {
                    Map<String, Object> oneAsset=new HashMap<String, Object>();
                    oneAsset.put("resId", rs.getString("id"));
                    oneAsset.put("resTableName", "wt_MediaAsset");
                    assetList.add(oneAsset);
                }

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
        likeSql="";
        for (int k=0; k<_s.length; k++) {
            if (k!=0) likeSql+=" or ";
            likeSql+="(CONCAT(IFNULL(a.smaTitle,''),'#S#',IFNULL(a.smaPublisher,''),'#S#',IFNULL(a.subjectWords,''),'#S#',IFNULL(a.keyWords,''),'#S#',IFNULL(a.descn,'')) like '%"+_s[k]+"%')";
        }
        sql+=(likeSql.length()>0?"("+likeSql+")":"")+" and b.count>0 limit 0,10";
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

                boolean canAdd=true;
                if (assetList!=null&&!assetList.isEmpty()) {
                    for (Map<String, Object> _anAsset: assetList) {
                        if ((_anAsset.get("resTableName")+"").equals("wt_SeqMediaAsset")&&(_anAsset.get("resId")+"").equals(rs.getString("id"))) {
                            canAdd=false;
                            break;
                        }
                    }
                }
                if (canAdd) {
                    Map<String, Object> oneAsset=new HashMap<String, Object>();
                    oneAsset.put("resId", rs.getString("id"));
                    oneAsset.put("resTableName", "wt_SeqMediaAsset");
                    assetList.add(oneAsset);
                }
            }
            rs.close(); rs=null;
            if (pageType==0&&!StringUtils.isNullOrEmptyOrSpace(_orSql)) {//为提升速度，提取单体节目
                _orSql="select b.sId, ma.* from wt_MediaAsset as ma, ("+
                        "select max(a.mId) as mId, a.sId from wt_SeqMA_Ref as a, vWt_FirstMaInSequ as sma "+
                        "where CONCAT('SID:', a.sId, '|C:', 10000+a.columnNum,'|D:', a.cTime)=CONCAT('SID:', sma.sId, '|', sma.firstMa) and ("+_orSql.substring(4)+") group by a.sId "+
                      ") as b where ma.id=b.mId";
                
                ps2=conn.prepareStatement(_orSql);
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

                        //去掉重复的
                        boolean canAdd=true;
                        if (assetList!=null&&!assetList.isEmpty()) {
                            for (Map<String, Object> _anAsset: assetList) {
                                if ((_anAsset.get("resTableName")+"").equals("wt_MediaAsset")&&(_anAsset.get("resId")+"").equals(rs.getString("id"))) {
                                    canAdd=false;
                                    break;
                                }
                            }
                        }
                        if (canAdd) {
                            Map<String, Object> oneAsset=new HashMap<String, Object>();
                            oneAsset.put("resId", rs.getString("id"));
                            oneAsset.put("resTableName", "wt_MediaAsset");
                            assetList.add(oneAsset);
                        }
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
        //得到发布列表
        List<Map<String, Object>> pubChannelList=channelService.getPubChannelList(assetList);
        //获得播放地址列表
        //List<Map<String, Object>> playUriList=groupDao.queryForListAutoTranform("getPlayListByIds", paraM);

        Map<String, Object> ret=new HashMap<String, Object>();
        ret.put("ResultType", resultType);
        Map<String, Object> oneMedia;
        int i=0;
        if (resultType==0) {//按一个列表获得
            List<Map<String, Object>> allList=new ArrayList<Map<String, Object>>();
            if (ret1!=null||ret1.size()>0) {
                for (; i<ret1.size(); i++) {
                    oneMedia=ContentUtils.convert2Bc(ret1.get(i), personList, cataList, pubChannelList, fList);
                    add(allList, oneMedia);
                }
            }
            if (ret2!=null||ret2.size()>0) {
                for (i=0; i<ret2.size(); i++) {
                    oneMedia=ContentUtils.convert2Ma(ret2.get(i), personList, cataList, pubChannelList, fList);
                    if (pageType==0&&ret2.get(i).get("sId")!=null) {
                        for (int j=0; j<ret3.size(); j++) {
                            if (ret3.get(j).get("id").equals(ret2.get(i).get("sId"))) {
                                Map<String, Object> _oneMedia=ContentUtils.convert2Sma(ret3.get(j), personList, cataList, pubChannelList, fList);
                                oneMedia.put("SeqInfo", _oneMedia);
                            }
                        }
                    }
                    add(allList, oneMedia);
                }
            }
            if (pageType==0&&ret4!=null) {
                for (i=0; i<ret4.size(); i++) {
                    oneMedia=ContentUtils.convert2Ma(ret4.get(i), personList, cataList, pubChannelList, fList);
                    for (int j=0; j<ret3.size(); j++) {
                        if (ret3.get(j).get("id").equals(ret4.get(i).get("sId"))) {
                            Map<String, Object> _oneMedia=ContentUtils.convert2Sma(ret3.get(j), personList, cataList, pubChannelList, fList);
                            oneMedia.put("SeqInfo", _oneMedia);
                        }
                    }
                    add(allList, oneMedia);
                }
            } else {
                if (ret3!=null||ret3.size()>0) {//系列节目
                    for (i=0; i<ret3.size(); i++) {
                        oneMedia=ContentUtils.convert2Sma(ret3.get(i), personList, cataList, pubChannelList, fList);
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
                    oneMedia=ContentUtils.convert2Bc(ret1.get(i), personList, cataList , pubChannelList, fList);
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
                    oneMedia=ContentUtils.convert2Ma(ret2.get(i), personList, cataList , pubChannelList, fList);
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
                                        oneMedia=ContentUtils.convert2Ma(ret2.get(i) ,personList, cataList, pubChannelList, fList);
                                        Map<String, Object> _oneMedia=ContentUtils.convert2Sma(ret3.get(j) ,personList, cataList, pubChannelList, fList);
                                        oneMedia.put("SeqInfo", _oneMedia);
                                        resultList3.add(oneMedia);
                                    }
                                }
                            }
                        }
                    }
                    if (ret4!=null||ret4.size()>0) {
                        for (i=0; i<ret4.size(); i++) {
                            oneMedia=ContentUtils.convert2Ma(ret4.get(i) ,personList, cataList, pubChannelList, fList);
                            for (int j=0; j<ret3.size(); j++) {
                                if (ret3.get(j).get("id").equals(ret4.get(i).get("sId"))) {
                                    Map<String, Object> _oneMedia=ContentUtils.convert2Sma(ret3.get(i) ,personList, cataList, pubChannelList, fList);
                                    oneMedia.put("SeqInfo", _oneMedia);
                                }
                            }
                            resultList3.add(oneMedia);
                        }
                    }
                } else {
                    for (i=0; i<ret3.size(); i++) {
                        oneMedia=ContentUtils.convert2Sma(ret3.get(i) ,personList, cataList, pubChannelList, fList);
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
     * @param userId 用户Id
     * @return
     */
    public Map<String, Object> getMainPage(String userId, int pageType, int pageSize, int page, MobileKey mk) {
        return getContents("-1", null, 3, null, 3, pageSize, page, null, pageType, mk, null);
    }

    /**
     * 获得专辑信息
     * @param contentId 专辑内容Id
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return
     */
    public Map<String, Object> getSeqMaInfo(String contentId, int pageSize, int page, MobileKey mk) {
        List<Map<String, Object>> cataList=null;//分类
        List<Map<String, Object>> personList=null;//人员
        Map<String, Object> paraM=new HashMap<String, Object>();

        //0、得到喜欢列表
        List<UserFavoritePo> _fList=favoriteService.getPureFavoriteList(mk);
        List<Map<String, Object>> fList=null;
        if (_fList!=null&&!_fList.isEmpty()) {
            fList=new ArrayList<Map<String, Object>>();
            for (UserFavoritePo ufPo: _fList) {
                fList.add(ufPo.toHashMapAsBean());
            }
        }
        //1、得主内容
        Map<String, Object> tempMap=groupDao.queryForObjectAutoTranform("getSmById", contentId);
        if (tempMap==null||tempMap.size()==0) return null;
        paraM.put("resTableName", "wt_SeqMediaAsset");
        paraM.put("ids", "'"+contentId+"'");
        cataList=groupDao.queryForListAutoTranform("getCataListByTypeAndIds", paraM);
        personList=groupDao.queryForListAutoTranform("getPersonListByTypeAndIds", paraM);
        //2、得到明细内容
        List<Map<String, Object>> tempList=groupDao.queryForListAutoTranform("getSmSubMedias", contentId);
        String subMedisIds="";
        //3、得到发布情况
        List<Map<String, Object>> assetList=new ArrayList<Map<String, Object>>();
        if (tempList!=null&&tempList.size()>0) {
            for (Map<String, Object> one: tempList) {
                subMedisIds+=",'"+one.get("id")+"'";
                Map<String, Object> oneAsset=new HashMap<String, Object>();
                oneAsset.put("resId", one.get("id"));
                oneAsset.put("resTableName", "wt_MediaAsset");
                assetList.add(oneAsset);
            }
        }
        Map<String, Object> oneAsset=new HashMap<String, Object>();
        oneAsset.put("resId", contentId);
        oneAsset.put("resTableName", "wt_SeqMediaAsset");
        assetList.add(oneAsset);
        List<Map<String, Object>> pubChannelList=channelService.getPubChannelList(assetList);
//        List<Map<String, Object>> playUriList=null;
//        if (!StringUtils.isNullOrEmptyOrSpace(subMedisIds)) {
//            paraM.put("maIds", subMedisIds.substring(1));
//            playUriList=groupDao.queryForListAutoTranform("getPlayListByIds", paraM);
//        }
        
        //4、组装内容
        Map<String, Object> retInfo=ContentUtils.convert2Sma(tempMap, personList, cataList, pubChannelList, fList);

        if (tempList!=null&&tempList.size()>0) {
            String ids="";
            for (Map<String, Object> one: tempList) {
                if (one.get("id")!=null) ids+=",'"+one.get("id")+"'"; 
            }
            ids=ids.substring(1);
            paraM.clear();
            paraM.put("resTableName", "wt_MediaAsset");
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
                subList.add(ContentUtils.convert2Ma(tempList.get(i), personList, cataList, pubChannelList, fList));
            }
            retInfo.put("SubList", subList);
            retInfo.put("PageSize", subList.size());
            retInfo.put("Page", page);
            retInfo.put("ContentSubCount", tempList.size());
        }
        return retInfo;
    }
    /**
     * 获得专辑信息
     * @param contentId 专辑内容Id
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return
     */
    public Map<String, Object> getMaInfo(String contentId, MobileKey mk) {
        List<Map<String, Object>> cataList=null;//分类
        List<Map<String, Object>> personList=null;//人员
        Map<String, Object> paraM=new HashMap<String, Object>();

        //0、得到喜欢列表
        List<UserFavoritePo> _fList=favoriteService.getPureFavoriteList(mk);
        List<Map<String, Object>> fList=null;
        if (_fList!=null&&!_fList.isEmpty()) {
            fList=new ArrayList<Map<String, Object>>();
            for (UserFavoritePo ufPo: _fList) {
                fList.add(ufPo.toHashMapAsBean());
            }
        }
        //1、得主内容
        Map<String, Object> tempMap=groupDao.queryForObjectAutoTranform("getMediaById", contentId);
        if (tempMap==null||tempMap.size()==0) return null;
        paraM.put("resTableName", "wt_MediaAsset");
        paraM.put("ids", "'"+contentId+"'");
        cataList=groupDao.queryForListAutoTranform("getCataListByTypeAndIds", paraM);
        personList=groupDao.queryForListAutoTranform("getPersonListByTypeAndIds", paraM);
//        List<Map<String, Object>> playUriList=null;
//        paraM.put("maIds", "'"+contentId+"'");
//        playUriList=groupDao.queryForListAutoTranform("getPlayListByIds", paraM);
        //2、得到发布情况
        List<Map<String, Object>> assetList=new ArrayList<Map<String, Object>>();
        Map<String, Object> oneAsset=new HashMap<String, Object>();
        oneAsset.put("resId", contentId);
        oneAsset.put("resTableName", "wt_SeqMediaAsset");
        assetList.add(oneAsset);
        List<Map<String, Object>> pubChannelList=channelService.getPubChannelList(assetList);
        //3、组装内容
        Map<String, Object> retInfo=ContentUtils.convert2Ma(tempMap, personList, cataList, pubChannelList, fList);

        return retInfo;
    }

    public Map<String, Object> getContents(String catalogType, String catalogId, int resultType, String mediaType, int perSize, int pageSize, int page, String beginCatalogId, int pageType,
                                            MobileKey mk, Map<String, Object> filterData) {
        //1-得到喜欢列表
        List<UserFavoritePo> _fList=favoriteService.getPureFavoriteList(mk);
        List<Map<String, Object>> fList=null;
        if (_fList!=null&&!_fList.isEmpty()) {
            fList=new ArrayList<Map<String, Object>>();
            for (UserFavoritePo ufPo: _fList) {
                fList.add(ufPo.toHashMapAsBean());
            }
        }

        Map<String, Object> ret=new HashMap<String, Object>();
        //2-根据参数获得范围
        //2.1-根据分类获得根
        TreeNode<? extends TreeNodeBean> root=null;
        if (catalogType.equals("-1")) {
            root=_cc.channelTree;
        } else {
            DictModel dm=_cd.getDictModelById(catalogType);
            if (dm!=null&&dm.dictTree!=null) root=dm.dictTree;
        }
        //2.2-获得相应的结点，通过查找
        if (root!=null&&catalogId!=null) root=root.findNode(catalogId);
        if (root==null) return null;
        if (root.isLeaf()) resultType=3;
        //3-得到分类id的语句
        String idCName="dictDid", typeCName="resTableName", resIdCName="resId";
        if (catalogType.equals("-1")) {
            idCName="channelId";typeCName="assetType";resIdCName="assetId";
        }
        //4-获得过滤内容
        String filterStr="";
        List<TreeNode<? extends TreeNodeBean>> allTn=null;
        String filterSql_inwhere="";
        String f_catalogType="", f_catalogId="";
        if (filterData!=null) {//若有过滤，类别过滤
            f_catalogType=filterData.get("CatalogType")==null?"-1":(filterData.get("CatalogType")+"");
            f_catalogId=filterData.get("CatalogId")==null?null:(filterData.get("CatalogId")+"");

            TreeNode<? extends TreeNodeBean> _root=null;
            if (!StringUtils.isNullOrEmptyOrSpace(f_catalogType)) {
                //根据分类获得根
                if (f_catalogType.equals("-1")) {
                    _root=_cc.channelTree;
                } else {
                    DictModel dm=_cd.getDictModelById(f_catalogType);
                    if (dm!=null&&dm.dictTree!=null) _root=dm.dictTree;
                }
                if (_root!=null&&!StringUtils.isNullOrEmptyOrSpace(f_catalogId)) _root=_root.findNode(f_catalogId);
            }
            if (_root!=null) {
                String _idCName=f_catalogType.equals("-1")?"channelId":"dictDid";
                if (!f_catalogType.equals("-1"))  filterSql_inwhere="b.dictMid='"+f_catalogType+"' and (";
                filterSql_inwhere+="b."+_idCName+"='"+_root.getId()+"'";
                allTn=TreeUtils.getDeepList(_root);
                if (allTn!=null&&!allTn.isEmpty()) {
                    for (TreeNode<? extends TreeNodeBean> tn: allTn) {
                        filterSql_inwhere+=" or b."+_idCName+"='"+tn.getId()+"'";
                    }
                }
                if (!f_catalogType.equals("-1")) filterSql_inwhere+=")";
                filterSql_inwhere="("+filterSql_inwhere+")";
            }
        }
        //5-得到媒体类型过滤串
        String mediaFilterSql="";
        if (!StringUtils.isNullOrEmptyOrSpace(mediaType)) {
            String[] _mt=mediaType.split(",");
            for (int i=0; i<_mt.length; i++) {
                if (_mt[i].trim().equals("RADIO")&&(mediaFilterSql.indexOf("wt_Broadcast")==-1)) {
                    mediaFilterSql+="or a."+typeCName+"='wt_Broadcast'";
                } else if (_mt[i].trim().equals("AUDIO")&&(mediaFilterSql.indexOf("wt_MediaAsset")==-1)) {
                    mediaFilterSql+="or a."+typeCName+"='wt_MediaAsset'";
                } else if (_mt[i].trim().equals("SEQU")&&(mediaFilterSql.indexOf("wt_SeqMediaAsset")==-1)) {
                    mediaFilterSql+="or a."+typeCName+"='wt_SeqMediaAsset'";
                }
            }
            if (mediaFilterSql.indexOf("wt_Broadcast")!=-1&&mediaFilterSql.indexOf("wt_MediaAsset")!=-1&&mediaFilterSql.indexOf("wt_SeqMediaAsset")!=-1) mediaFilterSql="";
            else mediaFilterSql=mediaFilterSql.substring(3);
        }

        List<Map<String, Object>> assetList=new ArrayList<Map<String, Object>>();//指标列表
        if (resultType==3) {//按列表处理
            //得到所有下级结点的Id
            allTn=TreeUtils.getDeepList(root);
            //得到分类id的语句
            String orSql="";
            orSql+=" or a."+idCName+"='"+root.getId()+"'";
            if (allTn!=null&&!allTn.isEmpty()) {
                for (TreeNode<? extends TreeNodeBean> tn: allTn) {
                    orSql+=" or a."+idCName+"='"+tn.getId()+"'";
                }
            }
            if (orSql.length()>0) orSql=orSql.substring(4);

            //得到获得数据条数的Sql
            String sqlCount=null;
            if (catalogType.equals("-1")) {//按发布栏目
                sqlCount="select count(distinct a.assetType,a.assetId) from wt_ChannelAsset a where a.isValidate=1 and a.flowFlag=2";
            } else {//按分类
                sqlCount="select count(distinct a.resTableName,a.resId) from wt_ResDict_Ref a where a.dictMid='"+catalogType+"'";
            }
            if (orSql.length()>0) sqlCount+=" and ("+orSql+")";
            if (mediaFilterSql.length()>0) sqlCount+=" and ("+mediaFilterSql+")";

            //得到获得具体数据的Sql
            String sql=null;
            if (StringUtils.isNullOrEmptyOrSpace(filterSql_inwhere)) {
                if (catalogType.equals("-1")) {//按发布栏目
                    sql="select a.assetType,a.assetId, max(a.pubTime) pubTime, max(a.sort) sort, a.flowFlag from wt_ChannelAsset a where a.isValidate=1 and a.flowFlag=2";
                } else {//按分类
                    sql="select distinct a.resTableName,a.resId from wt_ResDict_Ref a where a.dictMid='"+catalogType+"'";
                }
                if (orSql.length()>0) sql+=" and ("+orSql+")";
                if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                if (catalogType.equals("-1")) sql+=" group by a.assetType,a.assetId,a.flowFlag order by a.sort desc, a.pubTime desc";//栏目
                else sql+=" order by a.cTime desc";//分类
            } else {
                if (catalogType.equals("-1")&&f_catalogType.equals("-1")) {//按发布栏目,用发布栏目过滤
                    sql="select * from ((select a.assetType,a.assetId, max(a.pubTime) pubTime, max(a.sort) sort, a.flowFlag from wt_ChannelAsset a, wt_ChannelAsset b "
                       +"where a.isValidate=1 and a.flowFlag=2 and b.isValidate=1 and b.flowFlag=2 and a.assetType=b.assetType and a.assetId=b.assetId";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    if (orSql.length()>0) sql+=" and ("+orSql+")";
                    sql+=" and "+filterSql_inwhere;
                    sql+=" group by a.assetType,a.assetId,a.flowFlag order by a.sort desc, a.pubTime desc) union (";
                    sql+="select a.assetType,a.assetId, max(a.pubTime) pubTime, max(a.sort) sort, a.flowFlag from wt_ChannelAsset a ";
                    sql+="left join wt_ChannelAsset b on a.assetType=b.assetType and a.assetId=b.assetId and b.isValidate=1 and b.flowFlag=2";
                    sql+=" and ("+filterSql_inwhere+")";
                    sql+=" where a.isValidate=1 and a.flowFlag=2";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    if (orSql.length()>0) sql+=" and ("+orSql+")";
                    sql+=" and b.id is null group by a.assetType,a.assetId,a.flowFlag order by a.sort desc, a.pubTime desc)) as ul";
                } else if (catalogType.equals("-1")&&!f_catalogType.equals("-1")) {//按发布栏目,用字典分类过滤
                    sql="select * from ((select a.assetType,a.assetId, max(a.pubTime) pubTime, max(a.sort) sort, a.flowFlag from wt_ChannelAsset a, wt_ResDict_Ref b "
                       +"where a.isValidate=1 and a.flowFlag=2 and (a.assetType=b.resTableName and a.assetId=b.resId)";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    if (orSql.length()>0) sql+=" and ("+orSql+")";
                    sql+=" and "+filterSql_inwhere;
                    sql+=" group by a.assetType,a.assetId,a.flowFlag order by a.sort desc, a.pubTime desc) union (";
                    sql+="select a.assetType,a.assetId, max(a.pubTime) pubTime, max(a.sort) sort, a.flowFlag from wt_ChannelAsset a ";
                    sql+="left join wt_ResDict_Ref b on (a.assetType=b.resTableName and a.assetId=b.resId)";
                    sql+=" and "+filterSql_inwhere;
                    sql+=" where a.isValidate=1 and a.flowFlag=2";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    if (orSql.length()>0) sql+=" and ("+orSql+")";
                    sql+=" and b.id is null group by a.assetType,a.assetId,a.flowFlag order by a.sort desc, a.pubTime desc)) as ul";
                } else if (!catalogType.equals("-1")&&f_catalogType.equals("-1")) {//按字典分类,用发布栏目过滤
                    sql="select * from ((select distinct a.resTableName,a.resId from wt_ResDict_Ref a, wt_ChannelAsset b "
                      +"where a.dictMid='"+catalogType+"' and b.isValidate=1 and b.flowFlag=2 and (b.assetType=a.resTableName and b.assetId=a.resId)";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    if (orSql.length()>0) sql+=" and ("+orSql+")";
                    sql+=" and "+filterSql_inwhere;
                    sql+=" order by a.cTime desc) union (";
                    sql+="select distinct a.resTableName,a.resId from wt_ResDict_Ref a ";
                    sql+="left join wt_ChannelAsset b on (b.assetType=a.resTableName and b.assetId=a.resId and b.isValidate=1 and b.flowFlag=2)";
                    sql+=" and "+filterSql_inwhere;
                    sql+=" where a.dictMid='"+catalogType+"'";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    if (orSql.length()>0) sql+=" and ("+orSql+")";
                    sql+=" and b.id is null order by a.cTime desc)) as ul";
                } else {//按字典分类,用字典分类过滤
                    sql="select * from ((select distinct a.resTableName,a.resId from wt_ResDict_Ref a, wt_ResDict_Ref b where a.dictMid='"+catalogType+"'";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    if (orSql.length()>0) sql+=" and ("+orSql+")";
                    sql+=" and "+filterSql_inwhere;
                    sql+=" and a.resTableName=b.resTableName and a.resId=b.resId";
                    sql+=" order by a.cTime desc) union (";
                    sql+="select  distinct a.resTableName,a.resId from wt_ResDict_Ref a ";
                    sql+="left join wt_ResDict_Ref b on a.resTableName=b.resTableName and a.resId=b.resId";
                    sql+=" and "+filterSql_inwhere;
                    sql+=" where a.dictMid='"+catalogType+"'";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    if (orSql.length()>0) sql+=" and ("+orSql+")";
                    sql+=" and b.id is null order by a.cTime desc)) as ul";
                }
            }
            sql+=" limit "+(((page<=0?1:page)-1)*pageSize)+","+pageSize; //分页
            //执行得到具体内容Id的SQL
            List<String> sortIdList=new ArrayList<String>();

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
                //获得记录
                ps=conn.prepareStatement(sql);
                rs=ps.executeQuery();

                String sma2msInSql="";
                String bcSqlSign="", maSqlSign="", smaSqlSign="";   //为找到内容设置
                String bcSqlSign1="", maSqlSign1="", smaSqlSign1="";//为查询相关信息设置
                List<Map<String, Object>> pubChannelList=new ArrayList<Map<String, Object>>();
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
                        if (pageType==0) sma2msInSql+=" or sma.sId='"+rs.getString(resIdCName)+"'";
                    }
                    Map<String, Object> oneAsset=new HashMap<String, Object>();
                    oneAsset.put("resId", rs.getString(resIdCName));
                    oneAsset.put("resTableName", rs.getString(typeCName));
                    assetList.add(oneAsset);
                    if (catalogType.equals("-1")) {
                        Map<String, Object> onePub=new HashMap<String, Object>();
                        onePub.put("assetType", rs.getString(resIdCName));
                        onePub.put("assetId", rs.getString(resIdCName));
                        onePub.put("pubTime", rs.getString("pubTime"));
                        onePub.put("flowFlag",rs.getString("flowFlag"));
                        pubChannelList.add(onePub);
                    }
                }
                //得到发布列表
                if (!catalogType.equals("-1")) pubChannelList=channelService.getPubChannelList(assetList);

                rs.close(); rs=null;
                ps.close(); ps=null;
                if (sortIdList!=null&&!sortIdList.isEmpty()) {
                    //以下为提取需要的
                    List<Map<String, Object>> ret4=new ArrayList<Map<String, Object>>();//只有当pageType=0时，此列表才有用
                    boolean samExtractHas=false;
                    if (sma2msInSql.length()>0) {//提取单体节目
                        String firstMaSql="select b.sId, ma.* from wt_MediaAsset as ma, ("+
                                            "select max(a.mId) as mId, a.sId from wt_SeqMA_Ref as a, vWt_FirstMaInSequ as sma "+
                                            "where CONCAT('SID:', a.sId, '|C:', 10000+a.columnNum,'|D:', a.cTime)=CONCAT('SID:', sma.sId, '|', sma.firstMa) and ("+sma2msInSql.substring(4)+") group by a.sId "+
                                          ") as b where ma.id=b.mId";
                        //获得提取出的单体节目
                        ps1=conn.prepareStatement(firstMaSql);
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
                            if (maSqlSign1.indexOf(""+rs.getString("id"))>1) {//单体中含有，要删除掉
                                int pos=maSqlSign.indexOf(""+rs.getString("id"));
                                maSqlSign=maSqlSign.substring(0, pos-8)+maSqlSign.substring(pos+(""+rs.getString("id")).length()+1);
                            } else {
                                maSqlSign1+=",'"+rs.getString("id")+"'";
                            }
                        }
                        rs.close(); rs=null;
                        ps1.close(); ps1=null;
                        samExtractHas=!ret4.isEmpty();
                    }

                    //List<Map<String, Object>> playUriList=null; //播放地址
                    //以下为控制指标参数
                    Map<String, Object> paraM=new HashMap<String, Object>();
                    if (!StringUtils.isNullOrEmptyOrSpace(bcSqlSign)) {
                        bcSqlSign=bcSqlSign.substring(4);
                        paraM.put("bcIds", bcSqlSign1.substring(1));
                    }
                    if (!StringUtils.isNullOrEmptyOrSpace(maSqlSign)) {
                        maSqlSign=maSqlSign.substring(4);
                        paraM.put("maIds", maSqlSign1.substring(1));
                        //playUriList=groupDao.queryForListAutoTranform("getPlayListByIds", paraM);
                    }
                    if (!StringUtils.isNullOrEmptyOrSpace(smaSqlSign)) {//专辑处理
                        smaSqlSign=smaSqlSign.substring(4);
                        paraM.put("smaIds", smaSqlSign1.substring(1));
                    }

                    //重构人员及分类列表：目前不处理人员
                    List<Map<String, Object>> cataList=groupDao.queryForListAutoTranform("refCataById", paraM);

                    List<Map<String, Object>> _ret=new ArrayList<Map<String, Object>>();
                    for (int j=0; j<sortIdList.size(); j++) _ret.add(null);

                    if (!StringUtils.isNullOrEmptyOrSpace(bcSqlSign)) {//电台处理
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

                            Map<String, Object> oneMedia=ContentUtils.convert2Bc(oneData, null, cataList, pubChannelList, fList);
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

                            Map<String, Object> oneMedia=ContentUtils.convert2Ma(oneData, null, cataList, pubChannelList, fList);
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

                            Map<String, Object> oneMedia=ContentUtils.convert2Sma(oneData, null, cataList, pubChannelList, fList);
                            int i=0;
                            for (; i<sortIdList.size(); i++) {
                                if (sortIdList.get(i).equals("wt_SeqMediaAsset::"+oneMedia.get("ContentId"))) break;
                            }
                            boolean hasAdd=false;
                            if (pageType==0&&samExtractHas) {
                                for (Map<String, Object> _o: ret4) {
                                    if ((""+oneMedia.get("ContentId")).equals(""+_o.get("sId"))) {
                                        Map<String, Object> newOne=ContentUtils.convert2Ma(_o, null, cataList, pubChannelList, fList);
                                        newOne.put("SeqInfo", oneMedia);
                                        _ret.set(i, newOne);
                                        hasAdd=true;
                                        break;
                                    }
                                }
                            }
                            if (!hasAdd) _ret.set(i, oneMedia);
                        }
                        rs.close(); rs=null;
                        ps.close(); ps=null;
                    }
                    for (int i=_ret.size()-1; i>=0; i--) {
                        if (_ret.get(i)==null) _ret.remove(i);
                    }

                    ret.put("ResultType", resultType);
                    ret.put("AllCount", count);
                    ret.put("Page", page);
                    ret.put("PageSize",_ret.size());
                    ret.put("List",_ret);
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
            PreparedStatement psCount=null;
            PreparedStatement psAsset=null;
            PreparedStatement psBc=null;
            PreparedStatement psMa=null;
            PreparedStatement psSma=null;
            ResultSet rs=null;
            String sql="", sqlCount="";
            String sqlBc="select a.*, b.bcSource, b.flowURI from wt_Broadcast a left join wt_BCLiveFlow b on a.id=b.bcId and b.isMain=1 where SQL";
            String sqlMa="select * from wt_MediaAsset where SQL";
            String sqlSma="select a.*, case when b.count is null then 0 else b.count end as count from wt_SeqMediaAsset a left join (select sid, count(*) count from wt_SeqMA_Ref group by sid) b on a.id=b.sid where SQL";
            if (catalogType.equals("-1")) {
                sqlCount="select count(*) from wt_ChannelAsset a where a.isValidate=1 and a.flowFlag=2 and (SQL) "+(StringUtils.isNullOrEmptyOrSpace(mediaType)?"":(" and ("+mediaFilterSql+")"));
            } else {
                sqlCount="select count(*) from wt_ResDict_Ref a where a.dictMid='"+catalogType+"' and (SQL) "+(StringUtils.isNullOrEmptyOrSpace(mediaType)?"":(" and ("+mediaFilterSql+")"));
            }
            if (StringUtils.isNullOrEmptyOrSpace(filterSql_inwhere)) {
                if (catalogType.equals("-1")) {
                    sql="select * from wt_ChannelAsset a where a.isValidate=1 and a.flowFlag=2 and (SQL) "+(StringUtils.isNullOrEmptyOrSpace(mediaType)?"":(" and ("+mediaFilterSql+")"))+" order by sort desc, pubTime desc ";
                } else {
                    sql="select * from wt_ResDict_Ref a where a.dictMid='"+catalogType+"' and (SQL) "+(StringUtils.isNullOrEmptyOrSpace(mediaType)?"":(" and ("+mediaFilterSql+")"))+" order by cTime desc";
                }
            } else {
                if (catalogType.equals("-1")&&f_catalogType.equals("-1")) {//按发布栏目,用发布栏目过滤
                    sql="select * from ((select a.assetType,a.assetId, max(a.pubTime) pubTime, max(a.sort) sort, a.flowFlag from wt_ChannelAsset a, wt_ChannelAsset b "
                       +"where a.isValidate=1 and a.flowFlag=2 and b.isValidate=1 and b.flowFlag=2 and a.assetType=b.assetType and a.assetId=b.assetId";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    sql+=" and (SQL) and "+filterSql_inwhere;
                    sql+=" group by a.assetType,a.assetId,a.flowFlag order by a.sort desc, a.pubTime desc) union (";
                    sql+="select a.assetType,a.assetId, max(a.pubTime) pubTime, max(a.sort) sort, a.flowFlag from wt_ChannelAsset a ";
                    sql+="left join wt_ChannelAsset b on a.assetType=b.assetType and a.assetId=b.assetId and b.isValidate=1 and b.flowFlag=2";
                    sql+=" and ("+filterSql_inwhere+")";
                    sql+=" where a.isValidate=1 and a.flowFlag=2";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    sql+=" and (SQL) and b.id is null group by a.assetType,a.assetId,a.flowFlag order by a.sort desc, a.pubTime desc)) as ul";
                } else if (catalogType.equals("-1")&&!f_catalogType.equals("-1")) {//按发布栏目,用字典分类过滤
                    sql="select * from ((select a.assetType,a.assetId, max(a.pubTime) pubTime, max(a.sort) sort, a.flowFlag from wt_ChannelAsset a, wt_ResDict_Ref b "
                       +"where a.isValidate=1 and a.flowFlag=2 and (a.assetType=b.resTableName and a.assetId=b.resId)";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    sql+=" and (SQL) and "+filterSql_inwhere;
                    sql+=" group by a.assetType,a.assetId,a.flowFlag order by a.sort desc, a.pubTime desc) union (";
                    sql+="select a.assetType,a.assetId, max(a.pubTime) pubTime, max(a.sort) sort, a.flowFlag from wt_ChannelAsset a ";
                    sql+="left join wt_ResDict_Ref b on (a.assetType=b.resTableName and a.assetId=b.resId)";
                    sql+=" and "+filterSql_inwhere;
                    sql+=" where a.isValidate=1 and a.flowFlag=2";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    sql+=" and (SQL)  and b.id is null group by a.assetType,a.assetId,a.flowFlag order by a.sort desc, a.pubTime desc)) as ul";
                } else if (!catalogType.equals("-1")&&f_catalogType.equals("-1")) {//按字典分类,用发布栏目过滤
                    sql="select * from ((select distinct a.resTableName,a.resId from wt_ResDict_Ref a, wt_ChannelAsset b "
                      +"where a.dictMid='"+catalogType+"' and b.isValidate=1 and b.flowFlag=2 and (b.assetType=a.resTableName and b.assetId=a.resId)";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    sql+=" and (SQL)  and "+filterSql_inwhere;
                    sql+=" order by a.cTime desc) union (";
                    sql+="select distinct a.resTableName,a.resId from wt_ResDict_Ref a ";
                    sql+="left join wt_ChannelAsset b on b.assetType=a.resTableName and b.assetId=a.resId and b.isValidate=1 and b.flowFlag=2";
                    sql+=" and "+filterSql_inwhere;
                    sql+=" where a.dictMid='"+catalogType+"'";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    sql+=" and (SQL)  and b.id is null order by a.cTime desc)) as ul";
                } else {//按字典分类,用字典分类过滤
                    sql="select * from ((select distinct a.resTableName,a.resId from wt_ResDict_Ref a, wt_ResDict_Ref b where a.dictMid='"+catalogType+"'";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    sql+=" and (SQL)  and "+filterSql_inwhere;
                    sql+=" and a.resTableName=b.resTableName and a.resId=b.resId";
                    sql+=" order by a.cTime desc) union (";
                    sql+="select distinct a.resTableName,a.resId from wt_ResDict_Ref a ";
                    sql+="left join wt_ResDict_Ref b on a.resTableName=b.resTableName and a.resId=b.resId";
                    sql+=" and "+filterSql_inwhere;
                    sql+=" where a.dictMid='"+catalogType+"'";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    sql+=" and (SQL) and b.id is null order by a.cTime desc)) as ul";
                }
            }
            if (StringUtils.isNullOrEmptyOrSpace(filterStr)) sql+=" limit "+perSize;
            try {
                conn=dataSource.getConnection();
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
                        String tempStr="a."+idCName+"='"+_stn.getId()+"'";
                        List<TreeNode<? extends TreeNodeBean>> subAllTn=TreeUtils.getDeepList(_stn);
                        if (subAllTn!=null&&!subAllTn.isEmpty()) {
                            for (TreeNode<? extends TreeNodeBean> tn: subAllTn) {
                                tempStr+=" or a."+idCName+"='"+tn.getId()+"'";
                            }
                        }
                        long count=0l;
                        psCount=conn.prepareStatement(sqlCount.replaceAll("SQL", tempStr));
                        rs=psCount.executeQuery();
                        while (rs!=null&&rs.next()) {
                            count=rs.getLong(1);
                        }
                        rs.close();rs=null;
                        if (count==0) continue;
                        psAsset=conn.prepareStatement(sql.replaceAll("SQL", tempStr));
                        rs=psAsset.executeQuery();
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
                            Map<String, Object> oneAsset=new HashMap<String, Object>();
                            oneAsset.put("resId", rs.getString(resIdCName));
                            oneAsset.put("resTableName", rs.getString(typeCName));
                            assetList.add(oneAsset);
                        }
                        rs.close();rs=null;

                        //得到发布列表
                        List<Map<String, Object>> pubChannelList=channelService.getPubChannelList(assetList);
                        //播放地址
                        //List<Map<String, Object>> playUriList=null;

                        if (sortIdList!=null&&!sortIdList.isEmpty()) {
                            Map<String, Object> paraM=new HashMap<String, Object>();
                            if (!StringUtils.isNullOrEmptyOrSpace(bcSqlSign)) {
                                bcSqlSign=bcSqlSign.substring(4);
                                paraM.put("bcIds", bcSqlSign1.substring(1));
                            }
                            if (!StringUtils.isNullOrEmptyOrSpace(maSqlSign)) {
                                maSqlSign=maSqlSign.substring(4);
                                paraM.put("maIds", maSqlSign1.substring(1));
                                //playUriList=groupDao.queryForListAutoTranform("getPlayListByIds", paraM);
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

                                    Map<String, Object> oneMedia=ContentUtils.convert2Bc(oneData, null, cataList, pubChannelList, fList);
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

                                    Map<String, Object> oneMedia=ContentUtils.convert2Ma(oneData, null, cataList, pubChannelList, fList);
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

                                    Map<String, Object> oneMedia=ContentUtils.convert2Sma(oneData, null, cataList, pubChannelList, fList);
                                    int i=0;
                                    for (; i<sortIdList.size(); i++) {
                                        if (sortIdList.get(i).equals("wt_SeqMediaAsset::"+oneMedia.get("ContentId"))) break;
                                    }
                                    _ret.set(i, oneMedia);
                                }
                            }

                            List<Map<String, Object>> _ret2=null;
                            if (!StringUtils.isNullOrEmptyOrSpace(filterStr)&&_ret.size()>0) {
                                int listLimit=perSize;
                                int[] retlIndex=new int[listLimit];
                                int i=0, j=0, insertIndex=0;
                                String identifyStr="";
                                Map<String, Object> oneMedia, oneCatalog;

                                for (; i<listLimit; i++) retlIndex[i]=-1;

                                for (i=0; i<_ret.size(); i++) {//第一次循环，把符合过滤条件的先找出来插入
                                    oneMedia=_ret.get(i);
                                    List<Map<String,Object>> catalogs=(List<Map<String, Object>>)oneMedia.get("ContentCatalogs");
                                    if (catalogs!=null&&catalogs.size()>0) {
                                        for (j=0; j<catalogs.size(); j++) {
                                            oneCatalog=catalogs.get(j);
                                            if (oneCatalog!=null&&!oneCatalog.isEmpty()) {
                                                identifyStr=oneCatalog.get("CataMId")+"::"+oneCatalog.get("CataDId");
                                                if (filterStr.indexOf(identifyStr)>-1) {
                                                    retlIndex[insertIndex++]=i;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (insertIndex>listLimit) break;
                                }
                                if (insertIndex<listLimit) {//第二次循环，插入剩下的元素
                                    boolean canInsert=false;
                                    for (i=0; i<_ret.size(); i++) {
                                        j=0;
                                        canInsert=true;
                                        while (retlIndex[j]!=-1) {
                                            if (retlIndex[j]==i) {
                                                canInsert=false;
                                                break;
                                            }
                                        }
                                        if (canInsert) {
                                            retlIndex[insertIndex++]=i;
                                        }
                                        if (insertIndex>listLimit) break;
                                    }
                                }
                                //取页
                                _ret2=new ArrayList<Map<String, Object>>();
                                for (i=0; i<(listLimit<_ret.size()?listLimit:_ret.size()); i++) {
                                    if (retlIndex[i]!=-1) {
                                        _ret2.add(_ret.get(retlIndex[i]));
                                    }
                                }
                            }

                            Map<String, Object> oneCatalog=new HashMap<String, Object>();
                            oneCatalog.put("CatalogType", catalogType);
                            oneCatalog.put("CatalogId", _stn.getId());
                            oneCatalog.put("CatalogName", _stn.getNodeName());
                            oneCatalog.put("AllCount",count);
                            oneCatalog.put("List", _ret2==null?_ret:_ret2);
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
                if (psAsset!=null) try {psAsset.close();psAsset=null;} catch(Exception e) {psAsset=null;} finally {psAsset=null;};
                if (psCount!=null) try {psCount.close();psCount=null;} catch(Exception e) {psCount=null;} finally {psCount=null;};
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
}
//测试的消息{"IMEI":"12356","UserId":"107fc906ae0f","ResultType":"0","SearchStr":"罗,电影,安徽"}=
//http://localhost:808/wt/searchByVoice.do
//http://localhost:808/wt/content/getSeqMaInfo.do