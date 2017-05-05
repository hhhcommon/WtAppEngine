package com.woting.appengine.content.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.core.model.BaseObject;
import com.spiritdata.framework.core.model.Page;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.ext.redis.GetBizData;
import com.spiritdata.framework.ext.spring.redis.RedisOperService;
import com.spiritdata.framework.util.DateUtils;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.StringUtils;
import com.spiritdata.framework.util.TreeUtils;
import com.woting.WtAppEngineConstants;
import com.woting.appengine.content.AddCacheDBInfoThread;
import com.woting.appengine.favorite.persis.po.UserFavoritePo;
import com.woting.appengine.favorite.service.FavoriteService;
import com.woting.appengine.solr.SolrUpdateThread;
import com.woting.appengine.solr.persis.po.SolrInputPo;
import com.woting.appengine.solr.persis.po.SolrSearchResult;
import com.woting.appengine.solr.service.SolrJService;
import com.woting.appengine.solr.utils.SolrUtils;
import com.woting.cm.core.utils.ContentUtils;
import com.woting.cm.cachedb.cachedb.service.CacheDBService;
import com.woting.cm.cachedb.playcountdb.service.PlayCountDBService;
import com.woting.cm.core.broadcast.persis.po.BCProgrammePo;
import com.woting.cm.core.broadcast.service.BcProgrammeService;
import com.woting.cm.core.channel.mem._CacheChannel;
import com.woting.cm.core.channel.service.ChannelService;
import com.woting.cm.core.common.model.Owner;
import com.woting.cm.core.dict.mem._CacheDictionary;
import com.woting.cm.core.dict.model.DictModel;
import com.woting.cm.core.dict.model.DictRefRes;
import com.woting.cm.core.dict.service.DictService;
import com.woting.cm.core.media.MediaType;
import com.woting.cm.core.subscribe.service.SubscribeService;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.searchword.service.WordService;

@Lazy(true)
@Service
public class ContentService {
    @Resource(name="connectionFactory123")
    JedisConnectionFactory redisConn;
    @Resource(name="connectionFactory182")
    JedisConnectionFactory redisConn182;
    @Resource(name="connectionFactory7_2")
    JedisConnectionFactory redisConn7_2;
    //先用Group代替！！
    @Resource(name="defaultDAO")
    private MybatisDAO<BaseObject> contentDao;
    @Resource
    private DataSource dataSource;
    @Resource
    private FavoriteService favoriteService;
    @Resource
    private ChannelService channelService;
    @Resource
    private BcProgrammeService bcProgrammeService;
    @Resource
    private SubscribeService subscribeService;
    @Resource
    private SolrJService solrJService;
    @Resource
    private WordService wordService;
    @Resource
    private DictService dictService;
    @Resource
    private CacheDBService cacheDBService;
    @Resource
    private PlayCountDBService playCountDBService;
    
    private _CacheDictionary _cd=null;
    private _CacheChannel _cc=null;

    @SuppressWarnings("unchecked")
    @PostConstruct
    public void initParam() {
        contentDao.setNamespace("WT_CONTENT");
        _cd=(SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)==null?null:((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)).getContent());
        _cc=(SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)==null?null:((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)).getContent());
    }

    /**
     * 得到某分类下的轮播图
     * @param catalogType 分类类型(目前只支持栏目-1)
     * @param catalogId 分类Id
     * @param size 得到轮播图的尺寸
     * @return 轮播图
     */
    public Map<String, Object> getLoopImgs(String catalogType, String catalogId, int size) {
        Map<String, Object> map=new HashMap<String, Object>();

        if (catalogType==null||!"-1".equals(catalogType.trim())) {
            map.put("ReturnType", "1003");
            map.put("Message", "不是有效参数");
            return map;
        }

        //2.1-根据分类获得根
        TreeNode<? extends TreeNodeBean> root=null;
        if (catalogType.equals("-1")) {
            root=_cc.channelTree;
        }
        //2.2-获得相应的结点，通过查找
        if (root!=null&&catalogId!=null) root=root.findNode(catalogId);
        if (root==null) {
            map.put("ReturnType", "1004");
            map.put("Message", "没有对应栏目");
            return map;
        }
        List<Map<String, Object>> imgList=channelService.getLoopImgs(catalogId);
        if (imgList!=null&&imgList.size()>0) {
            map.put("ReturnType", "1001");
            map.put("CatalogType", catalogType);
            map.put("CatalogId", catalogId);
            map.put("CatalogName", root.getNodeName());
            List<Map<String, Object>> retImgList=new ArrayList<Map<String, Object>>();
            int i=0;
            for (Map<String, Object> m: imgList) {
                if (++i>size&&size!=-1) break;
                Map<String, Object> oneImg=new HashMap<String, Object>();
                String temp=(String)m.get("assetType");
                MediaType mt=MediaType.buildByTabName(temp);
                if (mt!=MediaType.ERR) {
                    oneImg.put("MediaType", mt.getTypeName());
                    oneImg.put("ContentId", m.get("assetId"));
                    oneImg.put("LoopImg", m.get("loopImg"));
                }
                retImgList.add(oneImg);
            }
            map.put("LoopImgs", retImgList);
        } else {
            map.put("ReturnType", "1011");
        }
        return map;//
    }
    /**
     * 查找内容，此内容无排序，按照创建时间的先后顺序排序，最新的在最前面
     * @param searchStr 查找串
     * @param resultType 返回类型,0把所有结果按照一个列表返回；1按照“单体节目、系列节目、电台”的顺序分类给出
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return 创建用户成功返回1，否则返回0
     */
    public Map<String, Object> searchAll(String searchStr, int resultType, int pageType, MobileUDKey mUdk) {
        //得到喜欢列表
        List<UserFavoritePo> _fList=favoriteService.getPureFavoriteList(mUdk);
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
            List<String> idl=typeMap.get(resTableName);
            if (idl==null) {
                idl=new ArrayList<String>();
                typeMap.put(resTableName, idl);
            }
            boolean find=false;
            for (String id: idl) {
                find=id.equals(one.get("resId"));
                if (find) break;
            }
            if (!find) idl.add(one.get("resId")+"");
        }
        //0.2-查找节目-查人员
        List<Map<String, Object>> personList=contentDao.queryForListAutoTranform("searchPerson", _s);
        for (int i=0; i<personList.size(); i++) {
            Map<String, Object> one=personList.get(i);
            String resTableName=one.get("resTableName")+"";
            List<String> idl=typeMap.get(resTableName);
            if (idl==null) {
                idl=new ArrayList<String>();
                typeMap.put(resTableName, idl);
            }
            boolean find=false;
            for (String id: idl) {
                find=id.equals(one.get("resId"));
                if (find) break;
            }
            if (!find) idl.add(one.get("resId")+"");
        }

        List<Map<String, Object>> tempList=null;
        List<Map<String, Object>> assetList=new ArrayList<Map<String, Object>>();
        //1-查找电台
        paraM.put("searchArray", _s);
        String tempStr=getIds(typeMap, "wt_Broadcast", "c.id");
        if (tempStr!=null) paraM.put("orIds", tempStr);
        tempList=contentDao.queryForListAutoTranform("searchBc", paraM);
        String bcPlayOrIds="";
        for (int i=0; i<tempList.size(); i++) {
            add(ret1, tempList.get(i));
            bcPlayOrIds+=" or bcId='"+tempList.get(i).get("id")+"'";
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
        tempStr=getIds(typeMap, "wt_MediaAsset", "c.id");
        sql="select a.* from wt_MediaAsset a where ";
        likeSql="";
        for (int k=0; k<_s.length; k++) {
            if (k!=0) likeSql+=" or ";
            likeSql+="(CONCAT(IFNULL(a.maTitle,''),'#S#',IFNULL(a.maPublisher,''),'#S#',IFNULL(a.subjectWords,''),'#S#',IFNULL(a.keyWords,''),'#S#',IFNULL(a.descn,'')) like '%"+_s[k]+"%')";
        }
        sql+=(likeSql.length()>0?"("+likeSql+")":"")+" limit 0, 20";
        if (tempStr!=null) sql+=" union select c.* from wt_MediaAsset c where ("+tempStr+") limit 0, 10";
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
        tempStr=getIds(typeMap, "wt_SeqMediaAsset", "c.id");
        sql="select a.*, case when b.count is null then 0 else b.count end as count from wt_SeqMediaAsset a left join (select sid, count(*) count from wt_SeqMA_Ref group by sid) b on a.id=b.sid where ";
        likeSql="";
        for (int k=0; k<_s.length; k++) {
            if (k!=0) likeSql+=" or ";
            likeSql+="(CONCAT(IFNULL(a.smaTitle,''),'#S#',IFNULL(a.smaPublisher,''),'#S#',IFNULL(a.subjectWords,''),'#S#',IFNULL(a.keyWords,''),'#S#',IFNULL(a.descn,'')) like '%"+_s[k]+"%')";
        }
        sql+=(likeSql.length()>0?"("+likeSql+")":"")+" and b.count>0 limit 0,10";
        if (tempStr!=null) sql+=" union select c.*, case when d.count is null then 0 else d.count end as count from wt_SeqMediaAsset c left join (select sid, count(*) count from wt_SeqMA_Ref group by sid) d on c.id=d.sid "
                +"where ("+tempStr+") limit 0, 10";
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
        if (reBuildMap.get("wt_Broadcast")!=null&&reBuildMap.get("wt_Broadcast").size()>0) paraM.put("bcIds", getIds(reBuildMap, "wt_Broadcast", "a.resId"));
        if (reBuildMap.get("wt_MediaAsset")!=null&&reBuildMap.get("wt_MediaAsset").size()>0) paraM.put("maIds", getIds(reBuildMap, "wt_MediaAsset", "a.resId"));
        if (reBuildMap.get("wt_SeqMediaAsset")!=null&&reBuildMap.get("wt_SeqMediaAsset").size()>0) paraM.put("smaIds", getIds(reBuildMap, "wt_SeqMediaAsset", "a.resId"));

        //重构人员
        personList=contentDao.queryForListAutoTranform("refPersonById", paraM);
        //重构分类
        cataList=contentDao.queryForListAutoTranform("refCataById", paraM);
        //得到发布列表
        List<Map<String, Object>> pubChannelList=channelService.getPubChannelList(assetList);
        //获得播放地址列表
        //List<Map<String, Object>> playUriList=contentDao.queryForListAutoTranform("getPlayListByIds", paraM);
        //得到点击量
        List<Map<String, Object>> playCountList=contentDao.queryForListAutoTranform("refPlayCountById", paraM);
        //当前播放列表
        List<Map<String, Object>> playingList=null;
        if (ret1!=null&&ret1.size()>0) {
            Calendar cal = Calendar.getInstance();
            Date date = new Date();
            cal.setTime(date);
            int week = cal.get(Calendar.DAY_OF_WEEK);
            DateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timestr = sdf.format(date);
            paraM.clear();
            paraM.put("bcIds", bcPlayOrIds.substring(4));
            paraM.put("weekDay", week);
            paraM.put("sort", 0);
            paraM.put("timeStr", timestr);
            playingList=contentDao.queryForListAutoTranform("playingBc", paraM);
        }

        //组装结果信息
        Map<String, Object> ret=new HashMap<String, Object>();
        ret.put("ResultType", resultType);
        Map<String, Object> oneMedia;
        int i=0;
        if (resultType==0) {//按一个列表获得
            List<Map<String, Object>> allList=new ArrayList<Map<String, Object>>();
            if (ret1!=null||ret1.size()>0) {
                for (; i<ret1.size(); i++) {
                    oneMedia=ContentUtils.convert2Bc(ret1.get(i), personList, cataList, pubChannelList, fList, playCountList, playingList);
                    add(allList, oneMedia);
                }
            }
            if (ret2!=null||ret2.size()>0) {
                for (i=0; i<ret2.size(); i++) {
                    oneMedia=ContentUtils.convert2Ma(ret2.get(i), personList, cataList, pubChannelList, fList, playCountList);
                    if (pageType==0&&ret2.get(i).get("sId")!=null) {
                        for (int j=0; j<ret3.size(); j++) {
                            if (ret3.get(j).get("id").equals(ret2.get(i).get("sId"))) {
                                Map<String, Object> _oneMedia=ContentUtils.convert2Sma(ret3.get(j), personList, cataList, pubChannelList, fList, playCountList);
                                oneMedia.put("SeqInfo", _oneMedia);
                            }
                        }
                    }
                    add(allList, oneMedia);
                }
            }
            if (pageType==0&&ret4!=null) {
                for (i=0; i<ret4.size(); i++) {
                    oneMedia=ContentUtils.convert2Ma(ret4.get(i), personList, cataList, pubChannelList, fList, playCountList);
                    for (int j=0; j<ret3.size(); j++) {
                        if (ret3.get(j).get("id").equals(ret4.get(i).get("sId"))) {
                            Map<String, Object> _oneMedia=ContentUtils.convert2Sma(ret3.get(j), personList, cataList, pubChannelList, fList, playCountList);
                            oneMedia.put("SeqInfo", _oneMedia);
                        }
                    }
                    add(allList, oneMedia);
                }
            } else {
                if (ret3!=null||ret3.size()>0) {//系列节目
                    for (i=0; i<ret3.size(); i++) {
                        oneMedia=ContentUtils.convert2Sma(ret3.get(i), personList, cataList, pubChannelList, fList, playCountList);
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
                    oneMedia=ContentUtils.convert2Bc(ret1.get(i), personList, cataList , pubChannelList, fList, playCountList, playingList);
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
                    oneMedia=ContentUtils.convert2Ma(ret2.get(i), personList, cataList , pubChannelList, fList, playCountList);
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
                                        oneMedia=ContentUtils.convert2Ma(ret2.get(i) ,personList, cataList, pubChannelList, fList, playCountList);
                                        Map<String, Object> _oneMedia=ContentUtils.convert2Sma(ret3.get(j) ,personList, cataList, pubChannelList, fList, playCountList);
                                        oneMedia.put("SeqInfo", _oneMedia);
                                        resultList3.add(oneMedia);
                                    }
                                }
                            }
                        }
                    }
                    if (ret4!=null||ret4.size()>0) {
                        for (i=0; i<ret4.size(); i++) {
                            oneMedia=ContentUtils.convert2Ma(ret4.get(i) ,personList, cataList, pubChannelList, fList, playCountList);
                            for (int j=0; j<ret3.size(); j++) {
                                if (ret3.get(j).get("id").equals(ret4.get(i).get("sId"))) {
                                    Map<String, Object> _oneMedia=ContentUtils.convert2Sma(ret3.get(i) ,personList, cataList, pubChannelList, fList, playCountList);
                                    oneMedia.put("SeqInfo", _oneMedia);
                                }
                            }
                            resultList3.add(oneMedia);
                        }
                    }
                } else {
                    for (i=0; i<ret3.size(); i++) {
                        oneMedia=ContentUtils.convert2Sma(ret3.get(i) ,personList, cataList, pubChannelList, fList, playCountList);
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
    public Map<String, Object> getMainPage(String userId, int pageType, int pageSize, int page, MobileUDKey mUdk) {
        return getContents("-1", null, 3, null, 3, pageSize, page, null, pageType, mUdk, null, 1);
    }

    /**
     * 获得专辑信息
     * @param contentId 专辑内容Id
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSeqMaInfo(String contentId, int pageSize, int page, int sortType, MobileUDKey mUdk) {
        List<Map<String, Object>> cataList=null;//分类
        List<Map<String, Object>> personList=null;//人员
        Map<String, Object> paraM=new HashMap<String, Object>();

        //0-得到喜欢列表
        List<Map<String, Object>> fList=null;
        if (mUdk!=null) {
            Map<String, Object> param=new HashMap<String, Object>();
            param.put("mUdk", mUdk);
            String key="Favorite="+(mUdk.isUser()?("UserId=["+mUdk.getUserId()+"]=LIST"):("DeviceId=["+mUdk.getDeviceId()+"]=LIST"));
            RedisOperService roService=new RedisOperService(redisConn7_2, 11);
            Map<String, Object> bizData=null;
            try {
                bizData=roService.getAndSet(key, new GetFavoriteList(param), 30*60*1000, true);
            } finally {
                if (roService!=null) roService.close();
                roService=null;
            }
            List<Map<String, Object>> _fList=null;
            if (bizData!=null) {
                if (bizData.get("FromBiz")!=null) _fList=(List<Map<String, Object>>)(bizData.get("FromBiz"));
                else
                if (bizData.get("FromRedis")!=null) _fList=(List<Map<String, Object>>)JsonUtils.jsonToObj((String)bizData.get("FromRedis"), List.class);
            }
            if (_fList!=null&&!_fList.isEmpty()) {
                fList=new ArrayList<Map<String, Object>>();
                for (Map<String, Object> ufPo: _fList) {
                    fList.add(ufPo);
                }
            }
        }
        //1、得主内容
        Map<String, Object> tempMap=contentDao.queryForObjectAutoTranform("getSmById", contentId);
        if (tempMap==null||tempMap.size()==0) return null;
        paraM.put("resTableName", "wt_SeqMediaAsset");
        paraM.put("ids", " a.resId='"+contentId+"'");
        cataList=contentDao.queryForListAutoTranform("getCataListByTypeAndIds", paraM);
        personList=contentDao.queryForListAutoTranform("getPersonListByTypeAndIds", paraM);
        //2、得到明细内容
        paraM.put("sId", contentId);
        if (sortType==1) paraM.put("orderByClause", "order by b.columnNum desc, a.cTime desc , a.maTitle desc");
        else paraM.put("orderByClause", "order by b.columnNum asc, a.cTime asc, a.maTitle asc");
        Page<Map<String, Object>> pageList=contentDao.pageQueryAutoTranform(null, "getSmSubMedias", paraM, page, pageSize);
        List<Map<String, Object>> tempList=null;
        if (pageList!=null&&pageList.getDataCount()>0) tempList=(List<Map<String, Object>>)pageList.getResult();
        //3、得到发布情况
        List<Map<String, Object>> assetList=new ArrayList<Map<String, Object>>();
        if (tempList!=null) {
            for (Map<String, Object> one: tempList) {
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
        //4、得到点击量
        paraM.put("smaIds", "a.resTableName='wt_SeqMediaAsset' and a.resId='"+contentId+"'");
        List<Map<String, Object>> playCountList=contentDao.queryForListAutoTranform("refPlayCountById", paraM);
        //判断专辑是否订阅
        if (subscribeService.isOrNoSubscribe(tempMap.get("id")+"", mUdk)) tempMap.put("subscribe", "1");
        else tempMap.put("subscribe", "0");
        //5、组装内容——住内容
        Map<String, Object> retInfo=ContentUtils.convert2Sma(tempMap, personList, cataList, pubChannelList, fList, playCountList);

        if (tempList!=null&&tempList.size()>0) {
            String ids="";
            for (Map<String, Object> one: tempList) {
                if (one.get("id")!=null) ids+=" or a.resId='"+one.get("id")+"'"; 
            }
            ids=ids.substring(4);
            paraM.clear();
            paraM.put("resTableName", "wt_MediaAsset");
            paraM.put("ids", ids);
            cataList=contentDao.queryForListAutoTranform("getCataListByTypeAndIds", paraM);
            personList=contentDao.queryForListAutoTranform("getPersonListByTypeAndIds", paraM);
            paraM.put("maIds", "a.resTableName='wt_MediaAsset' and ("+ids+")");
            playCountList=contentDao.queryForListAutoTranform("refPlayCountById", paraM);

            List<Map<String, Object>> subList=new ArrayList<Map<String, Object>>();
            //计算页数
            int begin=0, end=tempList.size();
//            if (pageSize>0&&page>0) {
//                begin=pageSize*(page-1);
//                end=begin+pageSize;
//                if (end>tempList.size()) end=tempList.size();
//            }
            for (int i=begin; i<end; i++) {
                subList.add(ContentUtils.convert2Ma(tempList.get(i), personList, cataList, pubChannelList, fList, playCountList));
            }
            retInfo.put("SubList", subList);
            retInfo.put("PageSize", subList.size());
        }
        retInfo.put("Page", page);
        retInfo.put("ContentSubCount", pageList.getDataCount());
        return retInfo;
    }
    /**
     * 获得单体信息
     * @param contentId 专辑内容Id
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMaInfo(String contentId, MobileUDKey mUdk) {
        List<Map<String, Object>> cataList=null;//分类
        List<Map<String, Object>> personList=null;//人员
        List<Map<String, Object>> playCountList=null;//播放次数
        Map<String, Object> paraM=new HashMap<String, Object>();

        //0-得到喜欢列表
        List<Map<String, Object>> fList=null;
        if (mUdk!=null) {
            Map<String, Object> param=new HashMap<String, Object>();
            param.put("mUdk", mUdk);
            String key="Favorite="+(mUdk.isUser()?("UserId=["+mUdk.getUserId()+"]=LIST"):("DeviceId=["+mUdk.getDeviceId()+"]=LIST"));
            RedisOperService roService=new RedisOperService(redisConn7_2, 11);
            Map<String, Object> bizData=null;
            try {
                bizData=roService.getAndSet(key, new GetFavoriteList(param), 30*60*1000, true);
            } finally {
                if (roService!=null) roService.close();
                roService=null;
            }
            List<Map<String, Object>> _fList=null;
            if (bizData!=null) {
                if (bizData.get("FromBiz")!=null) _fList=(List<Map<String, Object>>)(bizData.get("FromBiz"));
                else
                if (bizData.get("FromRedis")!=null) _fList=(List<Map<String, Object>>)JsonUtils.jsonToObj((String)bizData.get("FromRedis"), List.class);
            }
            if (_fList!=null&&!_fList.isEmpty()) {
                fList=new ArrayList<Map<String, Object>>();
                for (Map<String, Object> ufPo: _fList) {
                    fList.add(ufPo);
                }
            }
        }
        //1、得主内容
        Map<String, Object> tempMap=contentDao.queryForObjectAutoTranform("getMediaById", contentId);
        if (tempMap==null||tempMap.size()==0) return null;
        paraM.put("resTableName", "wt_MediaAsset");
        paraM.put("ids", "a.resId='"+contentId+"'");
        cataList=contentDao.queryForListAutoTranform("getCataListByTypeAndIds", paraM);
        personList=contentDao.queryForListAutoTranform("getPersonListByTypeAndIds", paraM);
        paraM.clear();
        paraM.put("maIds", "a.resTableName='wt_MediaAsset' and a.resId='"+contentId+"'");
        playCountList=contentDao.queryForListAutoTranform("refPlayCountById", paraM);
//        List<Map<String, Object>> playUriList=null;
//        paraM.put("maIds", "'"+contentId+"'");
//        playUriList=contentDao.queryForListAutoTranform("getPlayListByIds", paraM);
        //2、得到发布情况
        List<Map<String, Object>> assetList=new ArrayList<Map<String, Object>>();
        Map<String, Object> oneAsset=new HashMap<String, Object>();
        oneAsset.put("resId", contentId);
        oneAsset.put("resTableName", "wt_MediaAsset");
        assetList.add(oneAsset);
        List<Map<String, Object>> pubChannelList=channelService.getPubChannelList(assetList);

        //3、组装内容
        Map<String, Object> retInfo=ContentUtils.convert2Ma(tempMap, personList, cataList, pubChannelList, fList, playCountList);

        return retInfo;
    }
    /**
     * 获得电台信息
     * @param contentId 专辑内容Id
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getBcInfo(String contentId, MobileUDKey mUdk) {
        List<Map<String, Object>> cataList=null;//分类
        List<Map<String, Object>> personList=null;//人员
        List<Map<String, Object>> playCountList=null;//播放次数
        Map<String, Object> paraM=new HashMap<String, Object>();

        //0、得到喜欢列表
        List<Map<String, Object>> fList=null;
        if (mUdk!=null) {
            Map<String, Object> param=new HashMap<String, Object>();
            param.put("mUdk", mUdk);
            String key="Favorite="+(mUdk.isUser()?("UserId=["+mUdk.getUserId()+"]=LIST"):("DeviceId=["+mUdk.getDeviceId()+"]=LIST"));
            RedisOperService roService=new RedisOperService(redisConn7_2, 11);
            Map<String, Object> bizData=null;
            try {
                bizData=roService.getAndSet(key, new GetFavoriteList(param), 30*60*1000, true);
            } finally {
                if (roService!=null) roService.close();
                roService=null;
            }
            List<Map<String, Object>> _fList=null;
            if (bizData!=null) {
                if (bizData.get("FromBiz")!=null) _fList=(List<Map<String, Object>>)(bizData.get("FromBiz"));
                else
                if (bizData.get("FromRedis")!=null) _fList=(List<Map<String, Object>>)JsonUtils.jsonToObj((String)bizData.get("FromRedis"), List.class);
            }
            if (_fList!=null&&!_fList.isEmpty()) {
                fList=new ArrayList<Map<String, Object>>();
                for (Map<String, Object> ufPo: _fList) {
                    fList.add(ufPo);
                }
            }
        }
        //1、得主内容
        Map<String, Object> tempMap=contentDao.queryForObjectAutoTranform("getBcById", contentId);
        if (tempMap==null||tempMap.size()==0) return null;
        paraM.put("resTableName", "wt_Broadcast");
        paraM.put("ids", "a.resId='"+contentId+"'");
        cataList=contentDao.queryForListAutoTranform("getCataListByTypeAndIds", paraM);
        personList=contentDao.queryForListAutoTranform("getPersonListByTypeAndIds", paraM);
        paraM.clear();
        paraM.put("maIds", "a.resTableName='wt_Broadcast' and a.resId='"+contentId+"'");
        playCountList=contentDao.queryForListAutoTranform("refPlayCountById", paraM);
//        List<Map<String, Object>> playUriList=null;
//        paraM.put("maIds", "'"+contentId+"'");
//        playUriList=contentDao.queryForListAutoTranform("getPlayListByIds", paraM);
        //2、得到发布情况
        List<Map<String, Object>> assetList=new ArrayList<Map<String, Object>>();
        Map<String, Object> oneAsset=new HashMap<String, Object>();
        oneAsset.put("resId", contentId);
        oneAsset.put("resTableName", "wt_Broadcast");
        assetList.add(oneAsset);
        List<Map<String, Object>> pubChannelList=channelService.getPubChannelList(assetList);
        //3、得到当前节目列表
        Calendar cal = Calendar.getInstance();
        Date date = new Date();
        cal.setTime(date);
        int week = cal.get(Calendar.DAY_OF_WEEK);
        DateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String timestr = sdf.format(date);
        paraM.clear();
        paraM.put("bcIds", "bcId='"+contentId+"'");
        paraM.put("weekDay", week);
        paraM.put("sort", 0);
        paraM.put("timeStr", timestr);
        List<Map<String, Object>> playingList=contentDao.queryForListAutoTranform("playingBc", paraM);
        
        //4、组装内容
        Map<String, Object> retInfo=ContentUtils.convert2Bc(tempMap, personList, cataList, pubChannelList, fList, playCountList, playingList);

        return retInfo;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map<String, Object> getContents(String catalogType, String catalogId, int resultType, String mediaType, int perSize, int pageSize, int page, String beginCatalogId,
                                            int pageType, MobileUDKey mUdk, Map<String, Object> filterData, int recursionTree) {
        Map<String, Object> param=new HashMap<String, Object>();
        RedisOperService roService=new RedisOperService(redisConn7_2, 11);
        //1-得到喜欢列表
        String key=null;
        List<Map<String, Object>> fList=null;
        if (mUdk!=null) {
            param.put("mUdk", mUdk);
            key="Favorite="+(mUdk.isUser()?("UserId=["+mUdk.getUserId()+"]=LIST"):("DeviceId=["+mUdk.getDeviceId()+"]=LIST"));
            Map<String, Object> bizData=null;
            try {
                bizData=roService.getAndSet(key, new GetFavoriteList(param), 30*60*1000, true);
            } finally {
                if (roService!=null) roService.close();
                roService=null;
            }
            List<Map<String, Object>> _fList=null;
            if (bizData!=null) {
                if (bizData.get("FromBiz")!=null) _fList=(List<Map<String, Object>>)(bizData.get("FromBiz"));
                else
                if (bizData.get("FromRedis")!=null) _fList=(List<Map<String, Object>>)JsonUtils.jsonToObj((String)bizData.get("FromRedis"), List.class);
            }
            if (_fList!=null&&!_fList.isEmpty()) {
                fList=new ArrayList<Map<String, Object>>();
                for (Map<String, Object> ufPo: _fList) {
                    fList.add(ufPo);
                }
            }
        }

        //2-根据参数得到内容
        key="Contents=CatalogType_CatalogId_ResultType_PageType_MediaType_PageSize_Page_PerSize_BeginCatalogId=FilterData=["+catalogType+"_"+catalogId+"_"+resultType+"_"+pageType+"_"+mediaType+"_"+pageSize+"_"+page+"_"+"_"+perSize+"_"+beginCatalogId+"_"+JsonUtils.objToJson(filterData)+"]";
        param.clear();
        param.put("CatalogType", catalogType);
        param.put("CatalogId", catalogId);
        param.put("ResultType", resultType);
        param.put("PageType", pageType);
        param.put("MediaType", mediaType);
        param.put("PageSize", pageSize);
        param.put("Page", page);
        param.put("PerSize", perSize);
        param.put("BeginCatalogId", beginCatalogId);
        param.put("FilterData", filterData);
        param.put("RecursionTree", recursionTree);

        roService=new RedisOperService(redisConn7_2, 11);

        Map<String, Object> bizData=null;
        try {
            bizData=roService.getAndSet(key, new GetContents(param), 30*60*1000, true);
        } finally {
            if (roService!=null) roService.close();
            roService=null;
        }
        param=null;
        if (bizData!=null) {
            if (bizData.get("FromBiz")!=null) param=(Map)(bizData.get("FromBiz"));
            else
            if (bizData.get("FromRedis")!=null) param=(Map)JsonUtils.jsonToObj((String)bizData.get("FromRedis"), Map.class);
        }
        if (param==null) return null;

        //加入喜欢
        if (fList==null||fList.size()==0) return param;
        resultType=0;
        try {resultType=Integer.parseInt(""+param.get("ResultType"));} catch(Exception e) {};
        if (resultType==1) {
            List<Map> lm=(List<Map>)param.get("List");
            for (Map m: lm) {
                List<Map> lm2=(List<Map>)m.get("List");
                for (Map m2: lm2) {
                    boolean find=false;
                    String tableName=MediaType.buildByTypeName(""+m2.get("MediaType")).getTabName();
                    for (Map fm: fList) {
                        if (m2.get("ContentId").equals(fm.get("resId"))&&fm.get("resTableName").equals(tableName)) {
                            find=true;
                            break;
                        }
                    }
                    if (find) m2.put("ContentFavorite", "1");
                }
            }
        } else if (resultType==3) {//列表
            List<Map> lm=(List<Map>)param.get("List");
            for (Map m: lm) {
                boolean find=false;
                String tableName=MediaType.buildByTypeName(""+m.get("MediaType")).getTabName();
                for (Map fm: fList) {
                    if (m.get("ContentId").equals(fm.get("resId"))&&fm.get("resTableName").equals(tableName)) {
                        find=true;
                        break;
                    }
                }
                if (find) m.put("ContentFavorite", "1");
            }
        }
        return param;
    }

    //私有方法
    private String getIds(Map<String, List<String>> m, String typeStr, String schemaColumnName) {
        List<String> l=m.get(typeStr);
        if (l==null||l.size()==0) return null;
        String ret="";
        for (String sKey: l) ret+=" or "+schemaColumnName+"='"+sKey+"'";
        return "("+ret.substring(4)+")";
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

	public List<Map<String, Object>> BcProgrammes(String bcId, String requestTimesstr) {
		String [] times = requestTimesstr.split(",");
		if (times!=null && times.length>0) {
			List<Map<String, Object>> l = new ArrayList<>();
			for (String ts : times) {
				long time = Long.valueOf(ts);
				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date(time));
				int week = cal.get(Calendar.DAY_OF_WEEK);
				List<BCProgrammePo> bcps = bcProgrammeService.getBCProgrammeListByTime(bcId, week, time, 0, " validTime desc", 1);
				if (bcps!=null && bcps.size()>0) {
					long validTime = bcps.get(0).getValidTime().getTime();
					bcps = bcProgrammeService.getBCProgrammeListByTime(bcId, week, 0, validTime, " beginTime", 0);
				}
				if (bcps!=null && bcps.size()>0) {
					List<Map<String, Object>> bcpsm = new ArrayList<>();
					for (BCProgrammePo bcProgrammePo : bcps) {
						Map<String, Object> m = new HashMap<>();
					    m.put("Title", bcProgrammePo.getTitle());
					    m.put("BeginTime", bcProgrammePo.getBeginTime());
					    m.put("EndTime", bcProgrammePo.getEndTime());
					    bcpsm.add(m);
					}
					Map<String, Object> mm = new HashMap<>();
					mm.put("Day", time);
					mm.put("List", bcpsm);
					l.add(mm);
				}
			}
			if (l!=null && l.size()>0) {
				return l;
			}
		}
		return null;
	}

	public Map<String, Object> getBCIsPlayingProgramme(String BcIds, long time) {
		Calendar cal = Calendar.getInstance();
		Date date = new Date(time);
		cal.setTime(date);
		int week = cal.get(Calendar.DAY_OF_WEEK);
		DateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String timestr = sdf.format(date);
		String[] ids = BcIds.split(",");
		if (BcIds!=null && BcIds.length()>0) {
			Map<String, Object> m = new HashMap<>();
			for (String id : ids) {
				String pr = bcProgrammeService.getBcIsPlaying(id, week, timestr, time);
				if (pr!=null) {
					m.put(id, pr);
				}
			}
			return m;
		}
		return null;
	}

	/**
	 * 仅得到专辑下属节目的列表
	 * @param contentId
	 * @param page
	 * @param pageSize
	 * @param sortType
	 * @param mUdk
	 * @return
	 */
	public Map<String, Object> getSmSubMedias(String contentId, int page, int pageSize, int sortType, MobileUDKey mUdk) {
		List<Map<String, Object>> cataList=null;//分类
        List<Map<String, Object>> personList=null;//人员
        List<Map<String, Object>> playCountList=null;
		Map<String, Object> paraM=new HashMap<String, Object>();
		//0、得到喜欢列表
        List<UserFavoritePo> _fList=favoriteService.getPureFavoriteList(mUdk);
        List<Map<String, Object>> fList=null;
        if (_fList!=null&&!_fList.isEmpty()) {
            fList=new ArrayList<Map<String, Object>>();
            for (UserFavoritePo ufPo: _fList) {
                fList.add(ufPo.toHashMapAsBean());
            }
        }
		//1、得主内容
        Map<String, Object> tempMap=contentDao.queryForObjectAutoTranform("getSmById", contentId);
        if (tempMap==null||tempMap.size()==0) return null;
        Map<String, Object> retInfo = new HashMap<>();
        retInfo.put("ContentId", contentId);
        paraM.put("sId", contentId);
        if (sortType==1) paraM.put("orderByClause", "order by b.columnNum desc, a.cTime desc , a.maTitle desc");
		else paraM.put("orderByClause", "order by b.columnNum asc, a.cTime asc, a.maTitle asc");
        paraM.put("limitByClause", (page-1)*pageSize+","+pageSize);
        //2、得到明细内容
        Page<Map<String, Object>> pageList=contentDao.pageQueryAutoTranform(null, "getSmSubMedias", paraM, page, pageSize);
        List<Map<String, Object>> tempList=null;
        if (pageList!=null&&pageList.getDataCount()>0) tempList=(List<Map<String, Object>>)pageList.getResult();
        //3、得到发布情况
        List<Map<String, Object>> assetList=new ArrayList<Map<String, Object>>();
        if (tempList!=null&&tempList.size()>0) {
            for (Map<String, Object> one: tempList) {
                Map<String, Object> oneAsset=new HashMap<String, Object>();
                oneAsset.put("resId", one.get("id"));
                oneAsset.put("resTableName", "wt_MediaAsset");
                assetList.add(oneAsset);
            }
        }
        List<Map<String, Object>> pubChannelList=channelService.getPubChannelList(assetList);
        if (tempList!=null&&tempList.size()>0) {
            String ids="";
            for (Map<String, Object> one: tempList) {
                if (one.get("id")!=null) ids+=" or a.resId='"+one.get("id")+"'"; 
            }
            ids=ids.substring(4);
            paraM.clear();
            paraM.put("resTableName", "wt_MediaAsset");
            paraM.put("ids", ids);
            cataList=contentDao.queryForListAutoTranform("getCataListByTypeAndIds", paraM);
            personList=contentDao.queryForListAutoTranform("getPersonListByTypeAndIds", paraM);
            paraM.put("maIds", "a.resTableName='wt_MediaAsset' and ("+ids+")");
            playCountList=contentDao.queryForListAutoTranform("refPlayCountById", paraM);

            List<Map<String, Object>> subList=new ArrayList<Map<String, Object>>();
            for (int i=0; i<tempList.size(); i++) {
                subList.add(ContentUtils.convert2Ma(tempList.get(i), personList, cataList, pubChannelList, fList, playCountList));
            }
            retInfo.put("SubList", subList);
            retInfo.put("PageSize", subList.size());
            retInfo.put("Page", page);
            retInfo.put("ContentSubCount", tempList.size());
        }
        return retInfo;
	}

	public Map<String, Object> searchBySolr(String searchStr, String mediaType, int pageType, int resultType, int page, int pageSize, int rootPage, String rootInfo, MobileUDKey mUdk) {
		Map<String, Object> retMap = new HashMap<>();
		switch (rootPage) {
			case 0:retMap = makeSearch(searchStr, mediaType, pageType, resultType, page, pageSize, mUdk);break;
			case 1:
			case 3:
			case 4:retMap = makeSearchBySearch(searchStr, mediaType, pageType, resultType, page, pageSize, rootInfo, mUdk);break;
			case 2:retMap = makeSearchByChannel(searchStr, mediaType, pageType, resultType, page, pageSize, rootInfo, mUdk);break;
			case 5:retMap = makeSearch(searchStr, mediaType, pageType, resultType, page, pageSize, mUdk);break;
			case 6:retMap = makeSearch(searchStr, mediaType, pageType, resultType, page, pageSize, mUdk);break;
			case 7:retMap = makeSearchByPerson(searchStr, mediaType, pageType, resultType, page, pageSize, rootInfo, mUdk);break;
			case 8:retMap = makeSearch(searchStr, mediaType, pageType, resultType, page, pageSize, mUdk);break;
			default:break;
		}
		if (retMap==null || retMap.size()<1) {
			retMap = makeSearch(searchStr, mediaType, pageType, resultType, page, pageSize, mUdk);
			return retMap;
		}
		return retMap;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> makeSearchByPerson(String searchStr, String mediaType, int pageType, int resultType,
			int page, int pageSize, String rootInfo, MobileUDKey mUdk) {
		try {
			String personId = null;
			if (rootInfo!=null) {
				String[] kv = rootInfo.split("_");
				if (kv[0].equals("PERSON")) {
					personId = kv[1];
				}
			}
			searchStr = "item:title:"+searchStr;
			if (personId!=null && personId.length()>0) {
				searchStr += " item_person:"+personId;
			}
			List<SolrInputPo> solrips = new ArrayList<>();
			List<SortClause> solrsorts = SolrUtils.makeSolrSort("score desc");
			SolrSearchResult sResult = solrJService.solrSearch(3, searchStr, solrsorts, null, "*,score", page, pageSize, "item_type:SEQU");
			if (sResult!=null && sResult.getSolrInputPos().size()>0) {
				solrips.addAll(sResult.getSolrInputPos());
		    }
			
			List<Map<String, Object>> retLs = new ArrayList<>();
			if (solrips!=null && solrips.size()>0) {
				Map<String, Object> mf = new HashMap<>();
				mf.put("mUdk", mUdk);
				List<Map<String, Object>> favret=(List<Map<String, Object>>)new GetFavoriteList(mf).getBizData();
				ExecutorService fixedThreadPool = Executors.newFixedThreadPool(solrips.size());
				for (int i=0;i<solrips.size();i++) {
					int f = i;
					retLs.add(null);
					List<Map<String, Object>> ret = favret;
					fixedThreadPool.execute(new Runnable() {
						public void run() {
							String contentid = solrips.get(f).getItem_id();
							String info = null;
							if (pageType==0) {
								if (solrips.get(f).getItem_type().equals("SEQU")) {
									String malist = cacheDBService.getCacheDBInfo("SEQU_"+contentid+"_SUBLIST");
									if (malist!=null) {
										List<String> mas = (List<String>) JsonUtils.jsonToObj(malist, List.class);
										contentid = mas.get(0).replace("AUDIO_", "");
										solrips.get(f).setItem_type("AUDIO");
									}
								}
							}
							info = cacheDBService.getCacheDBInfo(solrips.get(f).getItem_type()+"_"+contentid+"_INFO");
							if (info!=null && info.length()>0) {
								Map<String, Object> infomap = retLs.get(f);
								infomap = (Map<String, Object>) JsonUtils.jsonToObj(info, Map.class);
								long playcount = 0;
								playcount = playCountDBService.getPlayCountNum(solrips.get(f).getItem_type()+"_"+contentid+"_PLAYCOUNT");
								infomap.put("PlayCount", playcount);
								infomap.put("ContentFavorite", 0);
								try {
									if (solrips.get(f).getItem_type().equals("AUDIO")) {
										Map<String, Object> smainfom = (Map<String, Object>) infomap.get("SeqInfo");
										String smaid = smainfom.get("ContentId").toString();
										String smainfo = cacheDBService.getCacheDBInfo("SEQU_"+smaid+"_INFO");
										smainfom = (Map<String, Object>) JsonUtils.jsonToObj(smainfo, Map.class);
										infomap.put("SeqInfo", smainfom);
									}
								} catch (Exception e) {}
								if (ret!=null && ret.size()>0) {
									for (Map<String, Object> userfav : ret) {
										if (userfav!=null && userfav.get("resId").equals(contentid)) {
											if ((mediaType.equals("AUDIO") && userfav.get("resTableName").equals("wt_MediaAsset")) 
											|| (mediaType.equals("SEQU") && userfav.get("resTableName").equals("wt_SeqMediaAsset")) 
											|| (mediaType.equals("RADIO") && userfav.get("resTableName").equals("wt_Broadcast"))) 
												infomap.put("ContentFavorite", 1);
										}
								    }
								}
								retLs.set(f, infomap);
							}
						}
					});
				}
				fixedThreadPool.shutdown();
				while (true) {
					Thread.sleep(10);
					if (fixedThreadPool.isTerminated()) {
						break;
					}
				}
				if (retLs!=null && retLs.size()>0) {
					Iterator<Map<String, Object>> it = retLs.iterator();
					while (it.hasNext()) {
						Map<String, Object> m = it.next();
						if (m==null) {
							it.remove();
						}
					}
					Map<String, Object> ret = new HashMap<>();
					ret.put("ResultType", "1001");
					ret.put("List", retLs);
		            ret.put("AllCount", sResult.getRecordCount());
		            return ret;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, Object> makeSearchByChannel(String searchStr, String mediaType, int pageType, int resultType, int page, int pageSize, String rootInfo, MobileUDKey mUdk) {
		try {
			Map<String, Object> params = new HashMap<>();
			String channelId = null;
			if (rootInfo!=null) {
				String[] kv = rootInfo.split("_");
				if (kv[0].equals("CHANNEL")) {
					channelId = kv[1];
				}
			}
			String channelName = null;
			if (channelId!=null) {
				TreeNode chtree=_cc.channelTree.getChild(channelId);
				channelName = chtree.getNodeName();
			}
			if (channelName==null) return null;
			
			Map<String, Object> ownerSolrInfo = null;
			if (mUdk!=null && mUdk.getUserId()!=null) ownerSolrInfo = getOwnerRecommendSolrInfo(mUdk);
			String queryStr = "";
			queryStr = "item_title:"+searchStr+"^1.1";
			String channelfqstr = null;
			String idfqstr = null;
			
			if (ownerSolrInfo!=null && ownerSolrInfo.size()>0) {
				if (ownerSolrInfo.containsKey("item_channel")) channelfqstr += ownerSolrInfo.get("item_channel").toString();
				if (ownerSolrInfo.containsKey("item_title")) queryStr += " item_title:"+ownerSolrInfo.get("item_title").toString()+"^1";
			}
			 channelfqstr = "item_channel:"+channelName;
			//TODO
			List<SolrInputPo> solrips = new ArrayList<>();
			List<SortClause> solrsorts = SolrUtils.makeSolrSort("score desc");
			SolrSearchResult sResult = solrJService.solrSearch(2, queryStr, solrsorts, params, "*,score", page, pageSize, idfqstr, channelfqstr, "item_type:AUDIO");
			if (sResult!=null && sResult.getSolrInputPos().size()>0) {
				solrips.addAll(sResult.getSolrInputPos());
		    }
			
			List<Map<String, Object>> retLs = new ArrayList<>();
			if (solrips!=null && solrips.size()>0) {
				Map<String, Object> mf = new HashMap<>();
				mf.put("mUdk", mUdk);
                List<Map<String, Object>> favret=(List<Map<String, Object>>)new GetFavoriteList(mf).getBizData();
				ExecutorService fixedThreadPool = Executors.newFixedThreadPool(solrips.size());
				for (int i=0;i<solrips.size();i++) {
					int f = i;
					retLs.add(null);
					List<Map<String, Object>> ret = favret;
					fixedThreadPool.execute(new Runnable() {
						public void run() {
							String contentid = solrips.get(f).getItem_id();
							String info = null;
							if (pageType==0) {
								if (solrips.get(f).getItem_type().equals("SEQU")) {
									String malist = cacheDBService.getCacheDBInfo("SEQU_"+contentid+"_SUBLIST");
									if (malist!=null) {
										List<String> mas = (List<String>) JsonUtils.jsonToObj(malist, List.class);
										contentid = mas.get(0).replace("AUDIO_", "");
										solrips.get(f).setItem_type("AUDIO");
									}
								}
							}
							info = cacheDBService.getCacheDBInfo(solrips.get(f).getItem_type()+"_"+contentid+"_INFO");
							if (info!=null && info.length()>0) {
								Map<String, Object> infomap = retLs.get(f);
								infomap = (Map<String, Object>) JsonUtils.jsonToObj(info, Map.class);
								long playcount = 0;
								playcount = playCountDBService.getPlayCountNum(solrips.get(f).getItem_type()+"_"+contentid+"_PLAYCOUNT");
								infomap.put("PlayCount", playcount);
								infomap.put("ContentFavorite", 0);
								try {
									if (solrips.get(f).getItem_type().equals("AUDIO")) {
										Map<String, Object> smainfom = (Map<String, Object>) infomap.get("SeqInfo");
										String smaid = smainfom.get("ContentId").toString();
										String smainfo = cacheDBService.getCacheDBInfo("SEQU_"+smaid+"_INFO");
										smainfom = (Map<String, Object>) JsonUtils.jsonToObj(smainfo, Map.class);
										infomap.put("SeqInfo", smainfom);
									}
								} catch (Exception e) {}
								if (ret!=null && ret.size()>0) {
									for (Map<String, Object> userfav : ret) {
										if (userfav!=null && userfav.get("resId").equals(contentid)) {
											if ((mediaType.equals("AUDIO") && userfav.get("resTableName").equals("wt_MediaAsset")) 
											|| (mediaType.equals("SEQU") && userfav.get("resTableName").equals("wt_SeqMediaAsset")) 
											|| (mediaType.equals("RADIO") && userfav.get("resTableName").equals("wt_Broadcast"))) 
												infomap.put("ContentFavorite", 1);
										}
								    }
								}
								retLs.set(f, infomap);
							}
						}
					});
				}
				fixedThreadPool.shutdown();
				while (true) {
					Thread.sleep(10);
					if (fixedThreadPool.isTerminated()) {
						break;
					}
				}
				if (retLs!=null && retLs.size()>0) {
					Iterator<Map<String, Object>> it = retLs.iterator();
					while (it.hasNext()) {
						Map<String, Object> m = it.next();
						if (m==null) {
							it.remove();
						}
					}
					Map<String, Object> ret = new HashMap<>();
					ret.put("ResultType", "1001");
					ret.put("List", retLs);
		            ret.put("AllCount", sResult.getRecordCount());
		            return ret;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
    private Map<String, Object> makeSearch(String searchStr, String mediaType, int pageType, int resultType, int page, int pageSize, MobileUDKey mUdk) {
		try {
			List<SortClause> solrsorts = SolrUtils.makeSolrSort("score desc");
			SolrSearchResult sResult = null;
			List<SolrInputPo> solrips = new ArrayList<>();
			if (resultType==2) {
				String[] types = {"SEQU","AUDIO"};
				for (String type : types) {
					sResult = solrJService.solrSearch(1, searchStr, solrsorts, null, "*,score", 1, 5, "item_type:"+type);
					if (sResult!=null && sResult.getSolrInputPos().size()>0) {
						solrips.addAll(sResult.getSolrInputPos());
					}
				}
			} else {
				if (resultType==0) {
					if (mediaType!=null) {
						sResult = solrJService.solrSearch(1, searchStr, solrsorts, null, "*,score", page, pageSize, "item_type:"+mediaType);
						if (sResult!=null && sResult.getSolrInputPos().size()>0) {
							solrips.addAll(sResult.getSolrInputPos());
					    }
					} else {
						sResult = solrJService.solrSearch(1, searchStr, solrsorts, null, "*,score", page, pageSize);
						if (sResult!=null && sResult.getSolrInputPos().size()>0) {
							solrips.addAll(sResult.getSolrInputPos());
					    }
					}
				}
			}
			List<Map<String, Object>> retLs = new ArrayList<>();
			if (solrips!=null && solrips.size()>0) {
				Map<String, Object> mf = new HashMap<>();
				mf.put("mUdk", mUdk);
                List<Map<String, Object>> favret=(List<Map<String, Object>>)new GetFavoriteList(mf).getBizData();
				ExecutorService fixedThreadPool = Executors.newFixedThreadPool(solrips.size());
				for (int i=0;i<solrips.size();i++) {
					int f = i;
					retLs.add(null);
					List<Map<String, Object>> ret = favret;
					fixedThreadPool.execute(new Runnable() {
						public void run() {
							String contentid = solrips.get(f).getItem_id();
							String info = null;
							if (pageType==0) {
								if (solrips.get(f).getItem_type().equals("SEQU")) {
									String malist = cacheDBService.getCacheDBInfo("SEQU_"+contentid+"_SUBLIST");//FileUtils.readContentInfo("Content=MediaType_CID=[SEQU_"+contentid+"]=SUBLIST");
									if (malist!=null && malist.length()>0) {
										List<String> mas = (List<String>) JsonUtils.jsonToObj(malist, List.class);
										contentid = mas.get(0).replace("AUDIO_", "");
										solrips.get(f).setItem_type("AUDIO");
									}
								}
							}
							info = cacheDBService.getCacheDBInfo(solrips.get(f).getItem_type()+"_"+contentid+"_INFO");//FileUtils.readContentInfo("Content=MediaType_CID=["+solrips.get(f).getItem_type()+"_"+contentid+"]=INFO");
							if (info!=null && info.length()>32) {
								Map<String, Object> infomap = null;
								try {
									infomap = (Map<String, Object>) JsonUtils.jsonToObj(info, Map.class);
								} catch (Exception e) {
									e.printStackTrace();
								}
								
								long playcount = 0;
								playcount = playCountDBService.getPlayCountNum(solrips.get(f).getItem_type()+"_"+contentid+"_PLAYCOUNT");//FileUtils.readContentInfo("Content=MediaType_CID=["+solrips.get(f).getItem_type()+"_"+contentid+"]=PLAYCOUNT");
								infomap.put("PlayCount", playcount);
								infomap.put("ContentFavorite", 0);
								try {
									if (solrips.get(f).getItem_type().equals("AUDIO")) {
										Map<String, Object> smainfom = (Map<String, Object>) infomap.get("SeqInfo");
										String smaid = smainfom.get("ContentId").toString();
										String smainfo = cacheDBService.getCacheDBInfo("SEQU_"+smaid+"_INFO");//FileUtils.readContentInfo("Content=MediaType_CID=[SEQU_"+smaid+"]=INFO");
										smainfom = (Map<String, Object>) JsonUtils.jsonToObj(smainfo, Map.class);
										infomap.put("SeqInfo", smainfom);
									}
								} catch (Exception e) {}
								if (ret!=null && ret.size()>0) {
									for (Map<String, Object> userfav : ret) {
										if (userfav!=null && userfav.get("resId").equals(contentid)) {
											if ((mediaType.equals("AUDIO") && userfav.get("resTableName").equals("wt_MediaAsset")) 
											|| (mediaType.equals("SEQU") && userfav.get("resTableName").equals("wt_SeqMediaAsset")) 
											|| (mediaType.equals("RADIO") && userfav.get("resTableName").equals("wt_Broadcast"))) 
												infomap.put("ContentFavorite", 1);
										}
								    }
								}
								retLs.set(f, infomap);
							} else {
								addCacheDBInfo(contentid, solrips.get(f).getItem_type());
								info = cacheDBService.getCacheDBInfo(solrips.get(f).getItem_type()+"_"+contentid+"_INFO");
								if (info!=null && info.length()>32) {
									Map<String, Object> infomap = null;
									try {
										infomap = (Map<String, Object>) JsonUtils.jsonToObj(info, Map.class);
									} catch (Exception e) {
										e.printStackTrace();
									}
									
									long playcount = 0;
									playcount = playCountDBService.getPlayCountNum(solrips.get(f).getItem_type()+"_"+contentid+"_PLAYCOUNT");//FileUtils.readContentInfo("Content=MediaType_CID=["+solrips.get(f).getItem_type()+"_"+contentid+"]=PLAYCOUNT");
									infomap.put("PlayCount", playcount);
									infomap.put("ContentFavorite", 0);
									try {
										if (solrips.get(f).getItem_type().equals("AUDIO")) {
											Map<String, Object> smainfom = (Map<String, Object>) infomap.get("SeqInfo");
											String smaid = smainfom.get("ContentId").toString();
											String smainfo = cacheDBService.getCacheDBInfo("SEQU_"+smaid+"_INFO");//FileUtils.readContentInfo("Content=MediaType_CID=[SEQU_"+smaid+"]=INFO");
											smainfom = (Map<String, Object>) JsonUtils.jsonToObj(smainfo, Map.class);
											infomap.put("SeqInfo", smainfom);
										}
									} catch (Exception e) {}
									if (ret!=null && ret.size()>0) {
										for (Map<String, Object> userfav : ret) {
											if (userfav!=null && userfav.get("resId").equals(contentid)) {
												if ((mediaType.equals("AUDIO") && userfav.get("resTableName").equals("wt_MediaAsset")) 
												|| (mediaType.equals("SEQU") && userfav.get("resTableName").equals("wt_SeqMediaAsset")) 
												|| (mediaType.equals("RADIO") && userfav.get("resTableName").equals("wt_Broadcast"))) 
													infomap.put("ContentFavorite", 1);
											}
									    }
									}
									retLs.set(f, infomap);
								}
							}
						}
					});
				}
				fixedThreadPool.shutdown();
				while (true) {
					Thread.sleep(10);
					if (fixedThreadPool.isTerminated()) {
						break;
					}
				}
				if (retLs!=null && retLs.size()>0) {
					Iterator<Map<String, Object>> it = retLs.iterator();
					while (it.hasNext()) {
						Map<String, Object> m = it.next();
						if (m==null) {
							it.remove();
						}
					}
					Map<String, Object> ret = new HashMap<>();
					ret.put("ResultType", "1001");
					ret.put("List", retLs);
		            ret.put("AllCount", sResult.getRecordCount());
		            return ret;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> makeSearchBySearch(String searchStr, String mediaType, int pageType, int resultType, int page, int pageSize, String rootInfo, MobileUDKey mUdk) {
		try {
			Map<String, Object> params = new HashMap<>();
			String audioId = null;
			if (rootInfo!=null) {
				String[] kv = rootInfo.split("_");
				if (kv[0].equals("AUDIO")) {
					audioId = kv[1];
				}
			}
			Map<String, Object> contentSolrInfo = null;
			Map<String, Object> ownerSolrInfo = null;
			if (audioId!=null && audioId.length()>0) contentSolrInfo = getAUDIORecommendSolrInfo(audioId);
			if (mUdk!=null && mUdk.getUserId()!=null) ownerSolrInfo = getOwnerRecommendSolrInfo(mUdk);
			String queryStr = "";
			queryStr = "item_title:"+searchStr+"^1.1";
			String channelfqstr = null;
			String idfqstr = null;
			if (contentSolrInfo!=null && contentSolrInfo.size()>0) { //提取通过内容获得的推荐信息
				if (contentSolrInfo.containsKey("item_title")) queryStr += " item_title:"+contentSolrInfo.get("item_title").toString()+"^1";
				if (contentSolrInfo.containsKey("fqm")) {
					Map<String, Object> fqm = (Map<String, Object>) contentSolrInfo.get("fqm");
					if (fqm!=null && fqm.size()>0) {
						if (fqm.containsKey("id")) idfqstr = fqm.get("id").toString();
						if (fqm.containsKey("item_channel")) channelfqstr = fqm.get("item_channel").toString();
					}
				}
			}
			if (ownerSolrInfo!=null && ownerSolrInfo.size()>0) {
				if (ownerSolrInfo.containsKey("item_channel")) channelfqstr += ownerSolrInfo.get("item_channel").toString();
				if (ownerSolrInfo.containsKey("item_title")) queryStr += " item_title:"+ownerSolrInfo.get("item_title").toString()+"^1";
			}
			//TODO
			List<SolrInputPo> solrips = new ArrayList<>();
			List<SortClause> solrsorts = SolrUtils.makeSolrSort("score desc");
			SolrSearchResult sResult = solrJService.solrSearch(2, queryStr, solrsorts, params, "*,score", page, pageSize, idfqstr, channelfqstr, "item_type:AUDIO","-item_title:"+searchStr);
			if (sResult!=null && sResult.getSolrInputPos().size()>0) {
				solrips.addAll(sResult.getSolrInputPos());
		    }
			
			List<Map<String, Object>> retLs = new ArrayList<>();
			if (solrips!=null && solrips.size()>0) {
				Map<String, Object> mf = new HashMap<>();
				mf.put("mUdk", mUdk);
                List<Map<String, Object>> favret=(List<Map<String, Object>>)new GetFavoriteList(mf).getBizData();
				ExecutorService fixedThreadPool = Executors.newFixedThreadPool(solrips.size());
				for (int i=0;i<solrips.size();i++) {
					int f = i;
					retLs.add(null);
					List<Map<String, Object>> ret = favret;
					fixedThreadPool.execute(new Runnable() {
						public void run() {
							String contentid = solrips.get(f).getItem_id();
							String info = null;
							if (pageType==0) {
								if (solrips.get(f).getItem_type().equals("SEQU")) {
									String malist =  cacheDBService.getCacheDBInfo("SEQU_"+contentid+"_SUBLIST");;//FileUtils.readContentInfo("Content=MediaType_CID=[SEQU_"+contentid+"]=SUBLIST");
									if (malist!=null) {
										List<String> mas = (List<String>) JsonUtils.jsonToObj(malist, List.class);
										contentid = mas.get(0).replace("AUDIO_", "");
										solrips.get(f).setItem_type("AUDIO");
									}
								}
							}
							info = cacheDBService.getCacheDBInfo(solrips.get(f).getItem_type()+"_"+contentid+"_INFO");;//FileUtils.readContentInfo("Content=MediaType_CID=["+solrips.get(f).getItem_type()+"_"+contentid+"]=INFO");
							if (info!=null && info.length()>0) {
								Map<String, Object> infomap = retLs.get(f);
								infomap = (Map<String, Object>) JsonUtils.jsonToObj(info, Map.class);
								long playcount = 0;
								playcount = playCountDBService.getPlayCountNum(solrips.get(f).getItem_type()+"_"+contentid+"_PLAYCOUNT");//FileUtils.readContentInfo("Content::MediaType_CID::["+solrips.get(f).getItem_type()+"_"+contentid+"]::PLAYCOUNT");
								infomap.put("PlayCount", playcount);
								infomap.put("ContentFavorite", 0);
								try {
									if (solrips.get(f).getItem_type().equals("AUDIO")) {
										Map<String, Object> smainfom = (Map<String, Object>) infomap.get("SeqInfo");
										String smaid = smainfom.get("ContentId").toString();
										String smainfo = cacheDBService.getCacheDBInfo("SEQU_"+smaid+"_INFO");//FileUtils.readContentInfo("Content=MediaType_CID=[SEQU_"+smaid+"]=INFO");
										smainfom = (Map<String, Object>) JsonUtils.jsonToObj(smainfo, Map.class);
										infomap.put("SeqInfo", smainfom);
									}
								} catch (Exception e) {}
								if (ret!=null && ret.size()>0) {
									for (Map<String, Object> userfav : ret) {
										if (userfav!=null && userfav.get("resId").equals(contentid)) {
											if ((mediaType.equals("AUDIO") && userfav.get("resTableName").equals("wt_MediaAsset")) 
											|| (mediaType.equals("SEQU") && userfav.get("resTableName").equals("wt_SeqMediaAsset")) 
											|| (mediaType.equals("RADIO") && userfav.get("resTableName").equals("wt_Broadcast"))) 
												infomap.put("ContentFavorite", 1);
										}
								    }
								}
								retLs.set(f, infomap);
							}
						}
					});
				}
				fixedThreadPool.shutdown();
				while (true) {
					Thread.sleep(10);
					if (fixedThreadPool.isTerminated()) {
						break;
					}
				}
				if (retLs!=null && retLs.size()>0) {
					Iterator<Map<String, Object>> it = retLs.iterator();
					while (it.hasNext()) {
						Map<String, Object> m = it.next();
						if (m==null) {
							it.remove();
						}
					}
					Map<String, Object> ret = new HashMap<>();
					ret.put("ResultType", "1001");
					ret.put("List", retLs);
		            ret.put("AllCount", sResult.getRecordCount());
		            return ret;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	//===============================以下为Redis GetAndSet类==========================
	/**
	 * 获得某用户喜欢列表
	 */
    class GetFavoriteList extends GetBizData {
        public GetFavoriteList(Map<String, Object> param) {
            super(param);
        }
        @Override
        public Object getBizData() {
            return favoriteService.getPureFavoriteList((MobileUDKey)param.get("mUdk"));
        }
    }

    class GetContents extends GetBizData {
        private String catalogType;
        private String catalogId;
        private int resultType;//=3仅列表返回；1=按下级分类；2=按媒体类型
        private int pageType;
        private String mediaType;
        private int pageSize;
        private int page;
        //以下两个参数仅仅在resultType=1/2时这个参数才有意义
        private int perSize;
        private String beginCatalogId;
        private Map<String, Object> filterData;
        private int recursionTree=1;

        public GetContents(Map<String, Object> param) {
            super(param);
            parseParam();
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        private void parseParam() {
            catalogType=param.get("CatalogType")==null?"-1":(String)param.get("CatalogType");
            catalogId=param.get("CatalogId")==null?null:(String)param.get("CatalogId");

            resultType=param.get("ResultType")==null?3:(Integer)param.get("ResultType");
            pageType=param.get("PageType")==null?0:(Integer)param.get("PageType");
            mediaType=param.get("MediaType")==null?null:(String)param.get("MediaType");

            pageSize=param.get("PageSize")==null?0:(Integer)param.get("PageSize");
            page=param.get("Page")==null?0:(Integer)param.get("Page");

            perSize=param.get("PerSize")==null?0:(Integer)param.get("PerSize");
            beginCatalogId=param.get("BeginCatalogId")==null?null:(String)param.get("BeginCatalogId");
            filterData=param.get("FilterData")==null?null:(Map)param.get("FilterData");
            recursionTree=param.get("RecursionTree")==null?0:(Integer)param.get("RecursionTree");
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public Object getBizData() {
            Map<String, Object> ret=new HashMap<String, Object>();
            //1-根据参数获得范围
            //1.1-根据分类获得根
            TreeNode<? extends TreeNodeBean> root=null;
            if (catalogType.equals("-1")) root=_cc.channelTree;
            else {
                DictModel dm=_cd.getDictModelById(catalogType);
                if (dm!=null&&dm.dictTree!=null) root=dm.dictTree;
            }
            //1.2-获得相应的结点，通过查找
            if (root!=null&&catalogId!=null) root=root.findNode(catalogId);
            if (root==null) return null;
            if (root.isLeaf()) resultType=3;
            //1.3-得到分类id的语句
            String idCName="channelId", typeCName="assetType", resIdCName="assetId";
            if (!catalogType.equals("-1")) {
                idCName="dictDid";
                typeCName="resTableName";
                resIdCName="resId";
            }
            //4-获得过滤内容
            String filterStr="";
            List<TreeNode<? extends TreeNodeBean>> allTn=null;
            String filterSql_inwhere="";
            String f_catalogType="", f_catalogId="";
            String f_orderBySql="";
            if (filterData!=null) {//若有过滤，类别过滤
                f_catalogType=filterData.get("CatalogType")==null?"-1":(filterData.get("CatalogType")+"");
                f_catalogId=filterData.get("CatalogId")==null?null:(filterData.get("CatalogId")+"");

                TreeNode<? extends TreeNodeBean> _root=null;
                if (!StringUtils.isNullOrEmptyOrSpace(f_catalogType)) {
                    //根据分类获得根
                    if (f_catalogType.equals("-1")) _root=_cc.channelTree;
                    else {
                        DictModel dm=_cd.getDictModelById(f_catalogType);
                        if (dm!=null&&dm.dictTree!=null) _root=dm.dictTree;
                    }
                    if (_root!=null&&!StringUtils.isNullOrEmptyOrSpace(f_catalogId)) _root=_root.findNode(f_catalogId);
                }
                if (_root!=null) {
                    String _idCName=f_catalogType.equals("-1")?"channelId":"dictDid";
                    if (!f_catalogType.equals("-1")) {
                        if (!f_catalogType.equals("2")) {
                            filterSql_inwhere="b.dictMid='"+f_catalogType+"' and (";
                        } else {
                            filterSql_inwhere="(b.dictMid='"+f_catalogType+"' or b.dictMid='9') and (";
                        }
                    } 
                    filterSql_inwhere+="b."+_idCName+"='"+_root.getId()+"'";
                    f_orderBySql+="'"+_root.getId()+"'";
                    allTn=TreeUtils.getDeepList(_root);
                    if (allTn!=null&&!allTn.isEmpty()) {
                        for (TreeNode<? extends TreeNodeBean> tn: allTn) {
                            filterSql_inwhere+=" or b."+_idCName+"='"+tn.getId()+"'";
                            f_orderBySql+=",'"+tn.getId()+"'";
                        }
                    }
                    if (mediaType!=null && mediaType.equals("RADIO")) {
                        ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
                        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                            JedisConnectionFactory conn=(JedisConnectionFactory)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("connectionFactory182");
                            RedisOperService ros=new RedisOperService(conn, 5);
                            if (ros.exist("LINSHENG_ID_"+f_catalogId)) {
                                TreeNode<? extends TreeNodeBean> roots=null;
                                DictModel dm=_cd.getDictModelById(f_catalogType);
                                if (dm!=null&&dm.dictTree!=null) roots=dm.dictTree;
                                String lscataids = ros.get("LINSHENG_ID_"+f_catalogId);
                                String[] lsids = lscataids.split(",");
                                for (String str : lsids) {
                                    if (str.equals("dtfl2001_1")) {
                                        dm = _cd.getDictModelById("9");
                                        if (dm!=null&&dm.dictTree!=null) roots=dm.dictTree;
                                        roots = roots.findNode(str);
                                        filterSql_inwhere+=" or b."+idCName+"='"+roots.getId()+"'";
                                        f_orderBySql+=",'"+roots.getId()+"'";
                                        allTn=TreeUtils.getDeepList(roots);
                                        if (allTn!=null&&!allTn.isEmpty()) {
                                            for (TreeNode<? extends TreeNodeBean> tn: allTn) {
                                                filterSql_inwhere+=" or b."+idCName+"='"+tn.getId()+"'";
                                                f_orderBySql+=",'"+tn.getId()+"'";
                                            }
                                        }
                                    } else {
                                        dm=_cd.getDictModelById(f_catalogType);
                                        if (dm!=null&&dm.dictTree!=null) roots=dm.dictTree;
                                        roots = roots.findNode(str);
                                        TreeUtils.cutLevel(roots, 1);
                                        filterSql_inwhere+=" or b."+idCName+"='"+roots.getId()+"'";
                                        f_orderBySql+=",'"+roots.getId()+"'";
                                        allTn=TreeUtils.getDeepList(roots);
                                        if (allTn!=null&&!allTn.isEmpty()) {
                                            for (TreeNode<? extends TreeNodeBean> tn: allTn) {
                                                filterSql_inwhere+=" or b."+idCName+"='"+tn.getId()+"'";
                                                f_orderBySql+=",'"+tn.getId()+"'";
                                            }
                                        }
                                    }
                                }
                            }
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
                //得到分类id的语句
                String orSql="";
                String orderBySql = "";
                if (!root.isRoot()) {
                    orSql+=" or a."+idCName+"='"+root.getId()+"'";
                    orderBySql += "'"+root.getId()+"'";
                    if (recursionTree==1) {
                        allTn=TreeUtils.getDeepList(root);
                        if (allTn!=null&&!allTn.isEmpty()) {
                            for (TreeNode<? extends TreeNodeBean> tn: allTn) {
                                orSql+=" or a."+idCName+"='"+tn.getId()+"'";
                                orderBySql += ",'"+tn.getId()+"'";
                            }
                        }
                    }
                }
                if ((mediaType!=null && mediaType.equals("RADIO")) || catalogType.equals("2")) {
                    ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
                    if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                        JedisConnectionFactory conn=(JedisConnectionFactory)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("connectionFactory182");
                        RedisOperService ros=new RedisOperService(conn, 5);
                        if (ros.exist("LINSHENG_ID_"+catalogId)) {
                            TreeNode<? extends TreeNodeBean> roots=null;
                            DictModel dm=_cd.getDictModelById(catalogType);
                            if (dm!=null&&dm.dictTree!=null) roots=dm.dictTree;
                            String lscataids = ros.get("LINSHENG_ID_"+catalogId);
                            String[] lsids = lscataids.split(",");
                            for (String str : lsids) {
                                if (str.equals("dtfl2001_1")) {
                                    dm = _cd.getDictModelById("9");
                                    if (dm!=null&&dm.dictTree!=null) roots=dm.dictTree;
                                    roots = roots.findNode(str);
                                    orSql+=" or a."+idCName+"='"+roots.getId()+"'";
                                    orderBySql += ",'"+roots.getId()+"'";
                                    allTn=TreeUtils.getDeepList(roots);
                                    if (allTn!=null&&!allTn.isEmpty()) {
                                        for (TreeNode<? extends TreeNodeBean> tn: allTn) {
                                            orSql+=" or a."+idCName+"='"+tn.getId()+"'";
                                            orderBySql += ",'"+tn.getId()+"'";
                                        }
                                    }
                                } else {
                                    dm=_cd.getDictModelById(catalogType);
                                    if (dm!=null&&dm.dictTree!=null) roots=dm.dictTree;
                                    roots = roots.findNode(str);
                                    TreeUtils.cutLevel(roots, 1);
                                    orSql+=" or a."+idCName+"='"+roots.getId()+"'";
                                    orderBySql += ",'"+roots.getId()+"'";
                                    allTn=TreeUtils.getDeepList(roots);
                                    if (allTn!=null&&!allTn.isEmpty()) {
                                        for (TreeNode<? extends TreeNodeBean> tn: allTn) {
                                            orSql+=" or a."+idCName+"='"+tn.getId()+"'";
                                            orderBySql += ",'"+tn.getId()+"'";
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (orSql.length()>0) orSql=orSql.substring(4);

                //得到获得数据条数的Sql
//                String sqlCount=null;
//                if (catalogType.equals("-1")) {//按发布栏目
//                    sqlCount="select count(distinct a.assetType,a.assetId) from wt_ChannelAsset a where a.isValidate=1 and a.flowFlag=2";
//                } else {//按分类
//                    sqlCount="select count(distinct a.resTableName,a.resId) from wt_ResDict_Ref a where ( a.dictMid='"+catalogType+"'";
//                    if (catalogType.equals("1") || catalogType.equals("2")) {
//                        sqlCount+=" or a.dictMid='9' )";
//                    } else {
//                        sqlCount+=")";
//                    }
//                }
//                if (orSql.length()>0) sqlCount+=" and ("+orSql+")";
//                if (mediaFilterSql.length()>0) sqlCount+=" and ("+mediaFilterSql+")";

                //得到获得具体数据的Sql
                String sql=null;
                if (StringUtils.isNullOrEmptyOrSpace(filterSql_inwhere)) {
                    if (catalogType.equals("-1")) {//按发布栏目
                        sql="select distinct a.assetType,a.assetId from wt_ChannelAsset a where a.isValidate=1 and a.flowFlag=2";
                    } else {//按分类
                        sql="select distinct a.resTableName,a.resId from wt_ResDict_Ref a where ( a.dictMid='"+catalogType+"'";
                        if (catalogType.equals("1") || catalogType.equals("2")) {
                            sql+=" or a.dictMid='9' )";
                        } else {
                            sql+=")";
                        }
                    }
                    if (orSql.length()>0) sql+=" and ("+orSql+")";
                    if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                    if (catalogType.equals("-1")) {//栏目
                        if (!root.isLeaf()) sql+=" order by a.pubTime desc";
                        else sql+=" order by a.topSort desc, a.pubTime desc";
                    }
                    else sql+="  order by field(a.dictDid,"+orderBySql+") ,cTime desc";//分类
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
                        sql="select * from ((select distinct a.resTableName,a.resId,b.dictDid from wt_ResDict_Ref a, wt_ResDict_Ref b where a.dictMid='"+catalogType+"'";
                        if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                        if (orSql.length()>0) sql+=" and ("+orSql+")";
                        sql+=" and "+filterSql_inwhere;
                        sql+=" and a.resTableName=b.resTableName and a.resId=b.resId";
                        sql+=" order by a.cTime desc)) as ul";
                        sql+=" order by field(ul.dictDid,"+f_orderBySql+")";
                    }
                }
                sql+=" limit "+(((page<=0?1:page)-1)*pageSize)+","+pageSize; //分页
                //执行得到具体内容Id的SQL
                List<String> sortIdList=new ArrayList<String>();

                Connection conn=null;
                PreparedStatement ps=null;//获得所需的记录的id
                ResultSet rs=null;
                try {
                    conn=dataSource.getConnection();
                    //获得总条数
                    long count=0l;
                    count=1000;
                    //获得记录
                    ps=conn.prepareStatement(sql);
                    rs=ps.executeQuery();

                    String bcSqlSign="";   //为找到内容设置
                    String bcSqlSign1="";//为查询相关信息设置
                    orderBySql = "";
                    List<String> cacheIdList=new ArrayList<String>();
                    while (rs!=null&&rs.next()) {
                        sortIdList.add(rs.getString(typeCName)+"="+rs.getString(resIdCName));

                        MediaType MT=MediaType.buildByTabName(rs.getString(typeCName));
                        if (MT==MediaType.RADIO) {
                            bcSqlSign+=" or a.id='"+rs.getString(resIdCName)+"'";
                            bcSqlSign1+=" or a.resId='"+rs.getString(resIdCName)+"'";
                            orderBySql+=",'"+rs.getString(resIdCName)+"'";
                        } else {
                            cacheIdList.add(MT.getTypeName()+"_"+rs.getString(resIdCName)+"_INFO");
                        }
                    }
                    rs.close(); rs=null;
                    ps.close(); ps=null;

                    if (sortIdList!=null&&!sortIdList.isEmpty()) {
                        List<Map<String, Object>> _ret=new ArrayList<Map<String, Object>>();
                        for (int j=0; j<sortIdList.size(); j++) _ret.add(null);

                        if (!cacheIdList.isEmpty()) {
                            List<Map<String, Object>> fromCache=getCacheDBList(cacheIdList, 1, 10, true);
                            if (fromCache!=null&&!fromCache.isEmpty()) {
                                for (int i=0; i<sortIdList.size(); i++) {
                                    String[] s=sortIdList.get(i).split("=");
                                    MediaType MT=MediaType.buildByTabName(s[0]);
                                    String typeStr=MT.getTypeName();
                                    for (Map<String, Object> o:fromCache) {
                                        if (o.get("MediaType")!=null&&o.get("MediaType").equals(typeStr)&&o.get("ContentId")!=null&&o.get("ContentId").equals(s[1])) {
                                            if (pageType==0&&MT==MediaType.SEQU) {//需要提取内容
                                                if (o.get("SubList")==null) continue;
                                                if (((List)o.get("SubList")).isEmpty()) continue;
                                                Map newOne=(Map)((List)o.get("SubList")).get(0);
                                                if (newOne!=null) {
                                                    o.remove("SubList");
                                                    newOne.put("SeqInfo", o);
                                                    _ret.set(i, newOne);
                                                }
                                            } else {
                                                _ret.set(i, o);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (!StringUtils.isNullOrEmptyOrSpace(bcSqlSign)) {
                            Map<String, Object> paraM=new HashMap<String, Object>();
                            List<Map<String, Object>> cataList=null;
                            bcSqlSign=bcSqlSign.substring(4);
                            paraM.put("bcIds", "a.resTableName='wt_Broadcast' and ("+bcSqlSign1.substring(4)+")");
                            cataList=contentDao.queryForListAutoTranform("refCataById", paraM);
                            if (!StringUtils.isNullOrEmptyOrSpace(bcSqlSign)) {//电台处理
                                String bcSql = "select a.*, b.bcSource, b.flowURI from wt_Broadcast a left join wt_BCLiveFlow b on a.id=b.bcId and b.isMain=1 where "+bcSqlSign+" order by field(a.id,"+orderBySql.substring(1)+")";
                                ps=conn.prepareStatement(bcSql);
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

                                    Map<String, Object> oneMedia=ContentUtils.convert2Bc(oneData, null, cataList, null, null, null, null);
                                    Map<String, Object> pm = getBCIsPlayingProgramme(oneData.get("id")+"", System.currentTimeMillis());
                                    if (pm!=null && pm.size()>0) {
                                        oneMedia.put("IsPlaying", pm.get(oneData.get("id")+""));
                                    } else {
                                        oneMedia.put("IsPlaying", null);
                                    }
                                    int i=0;
                                    for (; i<sortIdList.size(); i++) {
                                        if (sortIdList.get(i).equals("wt_Broadcast="+oneMedia.get("ContentId"))) break;
                                    }
                                    _ret.set(i, oneMedia);
                                }
                                rs.close(); rs=null;
                                ps.close(); ps=null;
                            }
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
                        sql="select * from ((select distinct a.resTableName,a.resId,b.dictDid from wt_ResDict_Ref a, wt_ResDict_Ref b where a.dictMid='"+catalogType+"'";
                        if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
                        sql+=" and (SQL)  and "+filterSql_inwhere;
                        sql+=" and a.resTableName=b.resTableName and a.resId=b.resId";
                        sql+=" )) as ul";
                        sql+=" order by field(ul.dictDid,"+f_orderBySql+")";
//                        sql+=" order by a.cTime desc limit 0,"+perSize+") union (";
//                        sql+="select distinct a.resTableName,a.resId from wt_ResDict_Ref a ";
//                        sql+="left join wt_ResDict_Ref b on a.resTableName=b.resTableName and a.resId=b.resId";
//                        sql+=" and "+filterSql_inwhere;
//                        sql+=" where a.dictMid='"+catalogType+"'";
//                        if (mediaFilterSql.length()>0) sql+=" and ("+mediaFilterSql+")";
//                        sql+=" and (SQL) and b.id is null order by a.cTime desc)) as ul"; 
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
                            String bcPlayOrId="";
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
                                sortIdList.add(rs.getString(typeCName)+"="+rs.getString(resIdCName));
                                if (rs.getString(typeCName).equals("wt_Broadcast")) {
                                    bcSqlSign+=" or a.id='"+rs.getString(resIdCName)+"'";
                                    bcSqlSign1+=" or a.resId='"+rs.getString(resIdCName)+"'";
                                    bcPlayOrId+=" or bcId='"+rs.getString(resIdCName)+"'";
                                } else if (rs.getString(typeCName).equals("wt_MediaAsset")) {
                                    maSqlSign+=" or id='"+rs.getString(resIdCName)+"'";
                                    maSqlSign1+=" or a.resId='"+rs.getString(resIdCName)+"'";
                                } else if (rs.getString(typeCName).equals("wt_SeqMediaAsset")) {
                                    smaSqlSign+=" or id='"+rs.getString(resIdCName)+"'";
                                    smaSqlSign1+=" or a.resId='"+rs.getString(resIdCName)+"'";
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
                                //重构人员及分类列表
                                Map<String, Object> paraM=new HashMap<String, Object>();
                                if (!StringUtils.isNullOrEmptyOrSpace(bcSqlSign)) {
                                    bcSqlSign=bcSqlSign.substring(4);
                                    paraM.put("bcIds", "a.resTableName='wt_Broadcast' and ("+bcSqlSign1.substring(4)+")");
                                }
                                if (!StringUtils.isNullOrEmptyOrSpace(maSqlSign)) {
                                    maSqlSign=maSqlSign.substring(4);
                                    paraM.put("maIds", "a.resTableName='wt_MediaAsset' and ("+maSqlSign1.substring(4)+")");
                                    //playUriList=contentDao.queryForListAutoTranform("getPlayListByIds", paraM);
                                }
                                if (!StringUtils.isNullOrEmptyOrSpace(smaSqlSign)) {//专辑处理
                                    smaSqlSign=smaSqlSign.substring(4);
                                    paraM.put("smaIds", "a.resTableName='wt_SeqMediaAsset' and ("+smaSqlSign1.substring(4)+")");
                                }
                                List<Map<String, Object>> cataList=null;
                                List<Map<String, Object>> personList=null;
                                List<Map<String, Object>> playCountList=null; //播放次数
                                List<Map<String, Object>> playingList=null; //电台播放节目
                                if (!paraM.isEmpty()) {
                                    cataList=contentDao.queryForListAutoTranform("refCataById", paraM);
                                    playCountList=contentDao.queryForListAutoTranform("refPlayCountById", paraM);; //播放次数
                                    personList=contentDao.queryForListAutoTranform("refPersonById", paraM); //人员
                                    if (!StringUtils.isNullOrEmptyOrSpace(bcSqlSign)) {
                                        Calendar cal = Calendar.getInstance();
                                        Date date = new Date();
                                        cal.setTime(date);
                                        int week = cal.get(Calendar.DAY_OF_WEEK);
                                        DateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                                        String timestr = sdf.format(date);
                                        paraM.clear();
                                        paraM.put("bcIds", bcPlayOrId.substring(4));
                                        paraM.put("weekDay", week);
                                        paraM.put("sort", 0);
                                        paraM.put("timeStr", timestr);
                                        playingList=contentDao.queryForListAutoTranform("playingBc", paraM); //电台播放节目
                                    }
                                }

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
                                        oneData.put("CTime", rs.getTimestamp("cTime"));
                                        Map<String, Object> oneMedia=ContentUtils.convert2Bc(oneData, personList, cataList, pubChannelList, null, playCountList, playingList);
                                        Map<String, Object> pm = getBCIsPlayingProgramme(oneData.get("id")+"", System.currentTimeMillis());
                                        if (pm!=null && pm.size()>0) {
                                            oneMedia.put("IsPlaying", pm.get(oneData.get("id")+""));
                                        } else {
                                            oneMedia.put("IsPlaying", null);
                                        }
                                        int i=0;
                                        for (; i<sortIdList.size(); i++) {
                                            if (sortIdList.get(i).equals("wt_Broadcast="+oneMedia.get("ContentId"))) break;
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

                                        Map<String, Object> oneMedia=ContentUtils.convert2Ma(oneData, personList, cataList, pubChannelList, null, playCountList);
                                        int i=0;
                                        for (; i<sortIdList.size(); i++) {
                                            if (sortIdList.get(i).equals("wt_MediaAsset="+oneMedia.get("ContentId"))) break;
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

                                        Map<String, Object> oneMedia=ContentUtils.convert2Sma(oneData, personList, cataList, pubChannelList, null, playCountList);
                                        int i=0;
                                        for (; i<sortIdList.size(); i++) {
                                            if (sortIdList.get(i).equals("wt_SeqMediaAsset="+oneMedia.get("ContentId"))) break;
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
                                                    identifyStr=oneCatalog.get("CataMId")+"="+oneCatalog.get("CataDId");
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
        /**
         * 得到栏目下的所有内容，无用户信息
         * @param catalogType 分类Id
         * @param isRoot 是否为根
         * @param treeNode 结点
         * @param mediaType 类型过滤
         * @param page 第几页
         * @param pageSize 每页条数
         * @param pageType 是否提取其中的内容
         * @param resultType 列表还是分类
         * @return 
         */
//        private Map<String, Object> getOneCatalContents(String catalogType, TreeNode<? extends TreeNodeBean> treeNode, String mediaType, int page, int pageSize, int pageType, int resultType) {
//            int type=catalogType.equals("-1")?0:1;//0是栏目1是分类
//
//            return null;
//        }
    }
    
    /**
     * 通过用户本身进行推荐信息提取
     * @param mUdk
     * @return
     */
    private Map<String, Object> getOwnerRecommendSolrInfo(MobileUDKey mUdk) {
    	try {
			String item_title = "";
			Owner o = new Owner(mUdk.getPCDType(), mUdk.getUserId());
			Map<String, Object> wordmap = wordService.getHotWordsByOwner(o, 5);
			if (wordmap!=null && wordmap.size()>0) {
				Set<String> sets = wordmap.keySet();
				for (String wordStr : sets) {
					item_title += wordStr.replace(" ", "");
				}
			}
			String item_channel = "";
			Map<String, Object> param = new HashMap<>();
			param.put("resTableName", "plat_User");
			param.put("resId", mUdk.getUserId());
			param.put("dictMid", "-1");
			List<DictRefRes> dRefRes = dictService.getDictRefs(param);
			if (dRefRes!=null && dRefRes.size()>0) {
				for (DictRefRes dictRefRes : dRefRes) {
					item_channel += " item_channel:"+dictRefRes.getDd().getNodeName();
				}
//				item_channel = item_channel.substring(1);
			}
			Map<String, Object> retM = new HashMap<>();
			if (item_title!=null && item_title.length()>0) retM.put("item_title", item_title);
			if (item_channel!=null && item_channel.length()>0) retM.put("item_channel", item_channel);
			if (retM.size()>0) return retM;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
    }
    
    /**
     * 通过内容本身进行推荐信息提取
     * @param id
     * @return
     * 					fqm    id
     *  					   item_channel
     *                  item_title
     */
    @SuppressWarnings({ "unchecked", "unused" })
	private Map<String, Object> getAUDIORecommendSolrInfo(String id) {
    	try {
    		String mainfo = cacheDBService.getCacheDBInfo("AUDIO_"+id+"_INFO");
    		if (mainfo!=null && mainfo.length()>0) {
    			Map<String, Object> retM = new HashMap<>();
    			Map<String, Object> fqm = new HashMap<>();
    			String fq = "";
    			fq = "-id:AUDIO_"+id;
    			fqm.put("id", fq);
				Map<String, Object> mamap = (Map<String, Object>) JsonUtils.jsonToObj(mainfo, Map.class);
				Map<String, Object> smamap = (Map<String, Object>) mamap.get("SeqInfo");
				String smaId = smamap.get("ContentId").toString(); // 得到专辑的id
				List<Map<String, Object>> chamap = (List<Map<String, Object>>) mamap.get("ContentPubChannels");
				
				String item_channel = "";  // 得到栏目信息
				if (chamap!=null && chamap.size()>0) {
					for (Map<String, Object> chmap : chamap) {
						if (chmap!=null) {
							item_channel += " item_channel:"+chmap.get("ChannelName").toString();
						}
					}
//					item_channel = item_channel.substring(1);
					fqm.put("item_channel",item_channel);
					
				}
				retM.put("fqm", fqm);
				String item_title = "";
				String kws = null;
				try {kws = mamap.get("ContentKeyWord").toString();} catch (Exception e) {}
				if (kws!=null && kws.length()>0) {
					item_title = kws;
					retM.put("item_title", item_title);
				}
				if (retM!=null && retM.size()>0) {
					return retM;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
    }
    
    /**
     * 新增CacheDB表数据
     * @param type
     * @param id
     */
    public void addCacheDBInfo(String id, String type) {
    	if (type.equals("SEQU")) new AddCacheDBInfoThread(id).addCacheDB();
		else if (type.equals("AUDIO")) {
			try {
				SolrSearchResult sResult = solrJService.solrSearch(1, null, null, null, null, 1, 1, "item_type:AUDIO", "item_id:"+id);
				if (sResult!=null && sResult.getSolrInputPos().size()>0) {
					SolrInputPo solrInputPo = sResult.getSolrInputPos().get(0);
					String pid = solrInputPo.getItem_pid();
					sResult = solrJService.solrSearch(1, null, null, null, null, 1, 1, "item_type:SEQU", "item_id:"+pid);
					if (sResult!=null && sResult.getSolrInputPos().size()>0) {
						solrInputPo = sResult.getSolrInputPos().get(0);
						new AddCacheDBInfoThread(solrInputPo.getItem_id()).addCacheDB();
					} else {
						new SolrUpdateThread(pid).updateSolr();
						new AddCacheDBInfoThread(pid).addCacheDB();
					}
				}
			} catch (Exception e) {e.printStackTrace();}
		}
    }
    
    /**
     * 获得内容快照
     * @param cacheDBIds 例如 AUDIO_00022bbcc4b349f582587c2ef579ae5f_INFO
     * @param page 当cacheDBId为专辑时，才有效;
     * @param pageSize 当cacheDBId为专辑时，才有效;
     * @param isOrNoToLoad 当cacheDB未包含id信息时是否查询
     * @return
     *         返回结果包含内容信息，栏目信息，字典信息，播放次数，专辑下级节目信息
     */
    public List<Map<String, Object>> getCacheDBList(List<String> cacheDBIds, int page, int pageSize, boolean isOrNoToLoad) {
    	if (cacheDBIds!=null && cacheDBIds.size()>0) {
    		List<Map<String, Object>> retList = new ArrayList<>();
//    		for (int i = 0; i < cacheDBIds.size(); i++) retList.add(null);
    		ExecutorService fixedThreadPool = Executors.newFixedThreadPool(cacheDBIds.size());
    		for (int i = 0; i < cacheDBIds.size(); i++) {
    			int f = i;
    			retList.add(null);
				fixedThreadPool.execute(new Runnable() {
					public void run() {
						try {
							String cacheDBId = cacheDBIds.get(f);
							if (cacheDBId!=null && cacheDBId.length()>0) {
								String[] params = cacheDBId.split("_");
								String type = params[0];
								String id = params[1];
								Map<String, Object> retM = getCacheDBInfo(id, type, page, pageSize);
								if (retM==null||retM.size()==0) {
			                        System.out.println("============================NOIN CacheDB=="+System.currentTimeMillis());
									if (isOrNoToLoad) {
										if (type.equals("SEQU")) retM = getSeqMaInfo(id, pageSize, page, 1, null);
										else if (type.equals("AUDIO")) retM = getMaInfo(id, null);
										if (retM!=null) {
											retList.set(f, retM);
										}
									}
									addCacheDBInfo(id, type); //往CacheDB表里添加数据
								} else {
									retList.set(f, retM);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
    		
    		fixedThreadPool.shutdown();
			while (true) {
				try {
					Thread.sleep(20);
				} catch (Exception e) {e.printStackTrace();}
				if (fixedThreadPool.isTerminated()) {
					break;
				}
			}
			if (retList!=null && retList.size()>0) {
				Iterator<Map<String, Object>> it = retList.iterator();
				while (it.hasNext()) {
					Map<String, Object> m = it.next();
					if (m==null) {
						it.remove();
					}
				}
				return retList;
			}
		}
		return null;
    }
    
    /**
     * 
     * @param id 内容Id，目前只限专辑和节目,
     * @param type 内容类型，SEQU，AUDIO
     * @param page 
     *            当type='SEQU'时有效，专辑下级节目页码，默认为0
     * @param pageSize
     *            当type='SEQU'时有效，专辑下级节目每页页数，默认为0
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCacheDBInfo(String id, String type, int page, int pageSize) {
    	if (id!=null && type!=null) {
			if (type.equals("wt_SeqMediaAsset")) type = "SEQU";
			if (type.equals("wt_MediaAsset")) type = "AUDIO";
			String cacheDBInfostr = cacheDBService.getCacheDBInfo(type+"_"+id+"_INFO");
			if (cacheDBInfostr!=null && cacheDBInfostr.length()>0) {
				Map<String, Object> cacheDBMap = (Map<String, Object>) JsonUtils.jsonToObj(cacheDBInfostr, Map.class);
				long playcount = playCountDBService.getPlayCountNum(type+"_"+id+"_PLAYCOUNT");
				cacheDBMap.put("PlayCount", playcount);
				if (type.equals("SEQU") && page>0 && pageSize>0) {
					String smaSubList = cacheDBService.getCacheDBInfo(type+"_"+id+"_SUBLIST");
					if (smaSubList!=null && smaSubList.length()>0) {
						List<String> smaSubs = (List<String>) JsonUtils.jsonToObj(smaSubList, List.class);
						if (smaSubs!=null && smaSubs.size()>0) {
							int begNum = (page-1)*pageSize;
							int endNum = page*pageSize;
							if (smaSubs.size()>=begNum) {
								List<String> maIds = smaSubs.subList(begNum, smaSubs.size()>endNum?endNum:smaSubs.size());
								if (maIds!=null && maIds.size()>0) {
									List<Map<String, Object>> audios = cacheDBService.getCacheDBAudios(maIds);
									if (audios!=null && audios.size()>0) {
										cacheDBMap.put("SubList", audios);
									}
								}
							}
							if (!cacheDBMap.containsKey("SubList")) {
								cacheDBMap.put("SubList", null);
							}
						}
					}
				} else {
					if (type.equals("AUDIO")) {
						try {
							Map<String, Object> smamap = (Map<String, Object>) cacheDBMap.get("SeqInfo");
						    String smaId = smamap.get("ContentId").toString();
						    String smastr = cacheDBService.getCacheDBInfo("SEQU_"+smaId+"_INFO");
						    smamap = (Map<String, Object>) JsonUtils.jsonToObj(smastr, Map.class);
						    cacheDBMap.put("SeqInfo", smamap);
						} catch (Exception e) {}
					}
				}
				return cacheDBMap;
			}
		}
		return null;
    }
    
//    public String getAUDIOContentInfo(String id) {
//        return FileUtils.readContentInfo("Content=MediaType_CID=[AUDIO_"+id+"]=INFO");
//    }
//    
//    public String getSEQUContentInfo(String id) {
//        return FileUtils.readContentInfo("Content=MediaType_CID=[SEQU_"+id+"]=INFO");
//    }
//    
//    public String getSEQUSublist(String id) {
//    	return FileUtils.readContentInfo("Content=MediaType_CID=[SEQU_"+id+"]=SUBLIST");
//    }
}