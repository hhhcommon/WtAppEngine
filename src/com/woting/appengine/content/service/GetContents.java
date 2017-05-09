package com.woting.appengine.content.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.ext.redis.GetBizData;
import com.spiritdata.framework.util.StringUtils;
import com.spiritdata.framework.util.TreeUtils;
import com.woting.WtAppEngineConstants;
import com.woting.cm.core.channel.mem._CacheChannel;
import com.woting.cm.core.dict.mem._CacheDictionary;
import com.woting.cm.core.dict.model.DictModel;
import com.woting.cm.core.media.MediaType;

/**
 * 获得内容的类。
 * <prep>
 * 此类继承自ext.redis.GetBizData，可以获得内容也可以与Redis缓存相结合
 * </prep>
 * @author wanghui
 *
 */
public class GetContents extends GetBizData {
    private DataSource dataSource; //数据库连接
    private DataSource cacheDataSource; //数据库连接

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
    private int recursionTree=1;//默认为递归

    private _CacheDictionary _cd=null;
    private _CacheChannel _cc=null;

    private String idCName=null;
    private String typeCName=null;
    private String resIdCName=null;
    private String tableName=null;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GetContents(Map<String, Object> param) {
        super(param);
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
        recursionTree=param.get("RecursionTree")==null?recursionTree:(Integer)param.get("RecursionTree");

        _cd=(SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)==null?null:((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)).getContent());
        _cc=(SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)==null?null:((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)).getContent());
    }

    /**
     * 设置数据库连接
     * @param dataSource
     */
    public void setDataSource(DataSource dataSource, DataSource cacheDataSource) {
        this.dataSource=dataSource;
        this.cacheDataSource=cacheDataSource;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object getBizData() {
        if (dataSource==null) return null;

        //〇、根据类型的不同设置表和字段的参数
        idCName="channelId"; typeCName="assetType"; resIdCName="assetId"; tableName="wt_ChannelAsset";
        if (!catalogType.equals("-1")) {
            idCName="dictDid";
            typeCName="resTableName";
            resIdCName="resId";
            tableName="wt_ResDict_Ref";
        }

        //一、根据参数获得范围Sql
        List<String> sqlList=getRangeSql();
        if (sqlList==null||sqlList.isEmpty()) return null;
        String contentSql=sqlList.get(0);
        String countSql=sqlList.size()>0?sqlList.get(1).trim():null;

        //二、获得实际内容
        Map<String, Object> ret=new HashMap<String, Object>();
        Connection conn=null;
        PreparedStatement ps=null;//获得所需的记录的id
        ResultSet rs=null;
        try {
            conn=dataSource.getConnection();
            //获得条数
            long count=0;
            if (!countSql.isEmpty()) {
                ps=conn.prepareStatement(countSql);
                rs=ps.executeQuery();
                if (rs!=null&&rs.next()) count=rs.getLong(0);
            }
            try {
                rs.close();
                ps.close();
            } finally {
                rs=null;
                ps=null;
            }
            //获得内容
            ps=conn.prepareStatement(contentSql);
            rs=ps.executeQuery();
            List<String> contentIds=new ArrayList<String>();
            while (rs!=null&&rs.next()) {
                MediaType MT=MediaType.buildByTabName(rs.getString(typeCName));
                if (MT!=MediaType.ERR) contentIds.add(MT.getTypeName()+"_"+rs.getString(resIdCName));
            }
            try {
                rs.close();
                ps.close();
            } finally {
                rs=null;
                ps=null;
            }
            if (contentIds==null||contentIds.isEmpty()) return null;//若没有任何内容，返回空
            List<Map<String, Object>> fromCache=getMediaContentListFromCacheDB(contentIds, 1, 10, true);
            if (fromCache==null||fromCache.isEmpty()) return null;//若没有任何内容，返回空
            List<Map<String, Object>> _ret=new ArrayList<Map<String, Object>>();
            for (int i=0; i<contentIds.size(); i++) {
                String[] s=contentIds.get(i).split("_");
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
            ret.put("ResultType", resultType);
            ret.put("AllCount", count);
            ret.put("Page", page);
            ret.put("PageSize",_ret.size());
            ret.put("List",_ret);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
            if (ps!=null) try {ps.close();ps=null;} catch(Exception e) {ps=null;} finally {ps=null;};
            if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
        }
        return ret.isEmpty()?null:ret;
    }

    //根据参数获得范围Sql,并根据参数设置一些全局性的变量($)
    private List<String> getRangeSql() {
        //1-根据分类获得根
        TreeNode<? extends TreeNodeBean> root=null;
        if (catalogType.equals("-1")) root=_cc.channelTree;
        else {
            DictModel dm=_cd.getDictModelById(catalogType);
            if (dm!=null&&dm.dictTree!=null) root=dm.dictTree;
        }
        //通过查找得到相应的结点，
        if (root!=null&&catalogId!=null) root=root.findNode(catalogId);
        if (root==null) return null;

        if (root.isLeaf()||recursionTree==0) resultType=3; //$

        //2-得到媒体类型过滤串
        String mediaFilterSql="";//过滤类型的字符串
        if (!StringUtils.isNullOrEmptyOrSpace(mediaType)) {
            String[] _mt=mediaType.split(",");
            Map<String, String> tempMediaSql=new HashMap<String, String>();
            for (int i=0; i<_mt.length; i++) {
                MediaType MT=MediaType.buildByTypeName(_mt[i].trim());
                if (MT!=MediaType.ERR) tempMediaSql.put(_mt[i].trim(), "or a."+typeCName+"='"+MT.getTabName()+"'");
            }
            if (!tempMediaSql.isEmpty()) {
                for (String ms: tempMediaSql.keySet()) mediaFilterSql+=tempMediaSql.get(ms);
                mediaFilterSql=mediaFilterSql.substring(3);
            }
        }

        //4-下级内容的获得
        String orSql="";
        String orderBySql = "";
        if (!root.isRoot()) {
            orSql+=" or a."+idCName+"='"+root.getId()+"'";
            orderBySql += ",'"+root.getId()+"'";
            if (recursionTree==1) {
                List<TreeNode<? extends TreeNodeBean>> allTn=TreeUtils.getDeepList(root);
                if (allTn!=null&&!allTn.isEmpty()) {
                    for (TreeNode<? extends TreeNodeBean> tn: allTn) {
                        orSql+=" or a."+idCName+"='"+tn.getId()+"'";
                        orderBySql += ",'"+tn.getId()+"'";
                    }
                }
            }
        }
        String getContentSql="", getCountSql="";
        if (resultType==3) {//按列表返回
            getContentSql="select distinct a."+typeCName+", a."+resIdCName+" from "+tableName+" a where ";
            if (catalogType.equals("-1")) getContentSql+=" a.isValidate=1 and a.flowFlag=2";
            else  getContentSql+=" (a.dictMid='"+catalogType+"')";
            if (mediaFilterSql.length()>0) getContentSql+=" and ("+mediaFilterSql+")";
            if (orSql.length()>0) getContentSql+=" and ("+orSql.substring(4)+")";
            getCountSql="select count(*) from ("+getContentSql+") as b";
            if (catalogType.equals("-1")) {//栏目
                if (!root.isLeaf()) getContentSql+=" order by a.pubTime desc";
                else getContentSql+=" order by a.topSort desc, a.pubTime desc";
            } else {//分类
                getContentSql+=" order by field(a.dictDid,"+orderBySql+") ,cTime desc";
            }
        } else if (resultType==2) {//按内容类型返回，先不做
            
        } else if (resultType==1) {//按分类下级返回
            
        }
        if (getContentSql.isEmpty()) return null;
        getContentSql+=" limit "+(((page<=0?1:page)-1)*pageSize)+","+pageSize; //分页
        List<String> ret=new ArrayList<String>();
        ret.add(getContentSql);
        ret.add(getCountSql);
        return ret;
    }
    /**
     * 获得内容快照
     * @param catchDBIds，快照Id的列表（排好顺序的，仅包括Id）形如【AUDIO_34weqr34evad245sgaer23432，READIO_34weqr34evad245sgaer23431, SEQU_34weqr34evad245sgaer23433】
     * @param page 只有当是专辑时，这个字段才有意义，获取所属节目的列表
     * @param pageSize 只有当是专辑时，这个字段才有意义，获取所属节目的列表
     * @param nullIsLoad 当cacheDB未包含id信息时是否从总库中加载这条记录
     * @return 返回结果包含内容信息，栏目信息，字典信息，专辑下级节目信息，其排序要与catchDBIds相同
     */
    private List<Map<String, Object>> getMediaContentListFromCacheDB(List<String> cacheDBIds, int page, int pageSize, boolean nullIsLoad) {
        if (cacheDBIds==null||cacheDBIds.isEmpty()) return null;
        String orSql="", orderSql="", tmpStr=null;
        for (int i=0; i<cacheDBIds.size(); i++) {
            tmpStr="'"+cacheDBIds.get(i)+"_INFO'";
            orSql+=" or id="+tmpStr;
            orderSql+=","+tmpStr;
            if (cacheDBIds.get(i).startsWith("SEQU_")) {
                tmpStr="'"+cacheDBIds.get(i)+"_SUBLIST'";
                orSql+=" or id="+tmpStr;
                orderSql+=","+tmpStr;
            }
        }
        Connection conn=null;
        PreparedStatement ps=null;//获得所需的记录的id
        ResultSet rs=null;
        try {
            conn=cacheDataSource.getConnection();
            ps=conn.prepareStatement("select * from wt_CahceDB where "+orSql.substring(4)+" order by fields(id, "+orderSql.substring(1)+")");
            rs=ps.executeQuery();
            //获得列表
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
            if (ps!=null) try {ps.close();ps=null;} catch(Exception e) {ps=null;} finally {ps=null;};
            if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
        }
        return null;
    }

}