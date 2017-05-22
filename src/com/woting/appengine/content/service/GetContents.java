package com.woting.appengine.content.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.ext.redis.GetBizData;
import com.spiritdata.framework.ext.spring.redis.RedisOperService;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.StringUtils;
import com.spiritdata.framework.util.TreeUtils;
import com.woting.WtAppEngineConstants;
import com.woting.cm.cachedb.cachedb.persis.po.CacheDBPo;
import com.woting.cm.cachedb.cachedb.service.CacheDBService;
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
    private ContentService contentService;
    private CacheDBService cacheDBService;

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
    public void setDataSource(DataSource dataSource, DataSource cacheDataSource, ContentService contentService, CacheDBService cacheDBService) {
        this.dataSource=dataSource;
        this.cacheDataSource=cacheDataSource;
        this.contentService=contentService;
        this.cacheDBService=cacheDBService;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public Object getBizData() {
        if (dataSource==null) return null;

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

        //〇、根据类型的不同设置表和字段的参数
        idCName="channelId"; typeCName="assetType"; resIdCName="assetId"; tableName="wt_ChannelAsset";
        if (!catalogType.equals("-1")) {
            idCName="dictDid";
            typeCName="resTableName";
            resIdCName="resId";
            tableName="wt_ResDict_Ref";
        }

        //一、根据参数获得范围Sql
        List<String> sqlList=getRangeSql(root);
        if (sqlList==null||sqlList.isEmpty()) return null;

        //二、获得实际内容
        Map<String, Object> ret=new HashMap<String, Object>();
        Connection conn=null;
        PreparedStatement ps=null;//获得所需的记录的id
        ResultSet rs=null;
        try {
            List<String> contentIds=new ArrayList<String>();
            List<String> contentCataIds=new ArrayList<String>();
            String contentSql=sqlList.get(0);

            conn=dataSource.getConnection();
            List<String> sortCataList=new ArrayList<String>();//层级结构的排序列表，当且仅当resultType!=1是有作用
            long count=100;
            if (resultType==3) {
                //获得条数
//                if (!countSql.isEmpty()) {
//                    ps=conn.prepareStatement(countSql);
//                    rs=ps.executeQuery();
//                    if (rs!=null&&rs.next()) count=rs.getLong(1);
//                }
//                try {
//                    rs.close();
//                    ps.close();
//                } finally {
//                    rs=null;
//                    ps=null;
//                }
                if (count==0) return null;
                //获得内容
                ps=conn.prepareStatement(contentSql);
                rs=ps.executeQuery();
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
            } else {
                int i=0;
                for (;i<sqlList.size(); i++) {
                    contentSql=sqlList.get(i);
                    
                    ps=conn.prepareStatement(contentSql);
                    rs=ps.executeQuery();
                    while (rs!=null&&rs.next()) {
                        MediaType MT=MediaType.buildByTabName(rs.getString(typeCName));
                        if (MT!=MediaType.ERR) {
                            contentIds.add(MT.getTypeName()+"_"+rs.getString(resIdCName));
                            contentCataIds.add(rs.getString("ThisCataId"));
                        }
                    }
                    //获得分类
                    contentSql=contentSql.substring(0, contentSql.indexOf("ThisCataId"));
                    contentSql=contentSql.substring(23, contentSql.length()-2);
                    sortCataList.add(contentSql);
                    if (contentCataIds.size()>=pageSize) break;
                }
                try {
                    rs.close();
                    ps.close();
                } finally {
                    rs=null;
                    ps=null;
                }
                if (i==sqlList.size()) beginCatalogId="ENDEND";
                else {
                    String cataSql=sqlList.get(i+1);
                    beginCatalogId=cataSql.substring(0, cataSql.indexOf("ThisCataId"));
                    beginCatalogId=beginCatalogId.substring(23, beginCatalogId.length()-2);
                }
            }
            if (contentIds==null||contentIds.isEmpty()) return null;//若没有任何内容，返回空
            Map<String, Map<String, Object>> fromCache=getMediaContentListFromCacheDB(contentIds, true);
            if (fromCache==null||fromCache.isEmpty()) return null;//若没有任何内容，返回空

            //组装返回值
            ret.put("ResultType", resultType);
            ret.put("PageType", pageType);
            ret.put("AllCount", count);
            ret.put("Page", page);
            List<Map<String, Object>> retList=new ArrayList<Map<String, Object>>();
            ret.put("List", retList);
            Map<String, Object> o=null;
            if (resultType==3) {//按列表处理
                for (int i=0; i<contentIds.size(); i++) {
                    o=fromCache.get(contentIds.get(i));
                    if (o!=null) retList.add(o);
                }
                ret.put("PageSize",retList.size());
            } else if (resultType==1) {//按下级分类处理
                if (sortCataList==null||sortCataList.isEmpty()) return null;

                TreeNode<? extends TreeNodeBean> node=null;
                Map<String, Object> oneCatalog=null;
                for (int i=0; i<sortCataList.size(); i++) {
                    String cataId=sortCataList.get(i);
                    node=root.findNode(cataId);
                    if (node!=null) {
                        oneCatalog=new HashMap<String, Object>();
                        oneCatalog.put("CatalogType", catalogType);
                        oneCatalog.put("CatalogId", node.getId());
                        oneCatalog.put("CatalogName", node.getNodeName());
                        oneCatalog.put("List", new ArrayList<Map<String, Object>>());
                        retList.add(oneCatalog);
                    }
                }
                int k=0;
                oneCatalog=retList.get(k);
                for (int i=0; i<contentIds.size()&&k<=retList.size(); i++) {
                    o=fromCache.get(contentIds.get(i));
                    String oneCataId=contentCataIds.get(i);

                    while(!oneCataId.equals(oneCatalog.get("CatalogId"))&&k<retList.size()) {
                        oneCatalog=retList.get(k++);
                    }
                    if (k<=retList.size()) {
                        ((List<Map<String, Object>>)oneCatalog.get("List")).add(o);
                    }
                }
                
                ret.put("BeginCatalogId", beginCatalogId);
                ret.put("PageSize", fromCache.size()-1);
            } else {//按类型处理，目前用不到
//                if (sortCataList==null||sortCataList.isEmpty()) return null;
//
//                TreeNode<? extends TreeNodeBean> node=null;
//                Map<String, Object> oneCatalog=null;
//                for (int i=0; i<sortCataList.size(); i++) {
//                    String cataId=sortCataList.get(i);
//                    node=root.findNode(cataId);
//                    if (node!=null) {
//                        oneCatalog=new HashMap<String, Object>();
//                        oneCatalog.put("CatalogType", catalogType);
//                        oneCatalog.put("CatalogId", node.getId());
//                        oneCatalog.put("CatalogName", node.getNodeName());
//                        oneCatalog.put("List", new ArrayList<Map<String, Object>>());
//                        retList.add(oneCatalog);
//                    }
//                }
//                int k=0;
//                for (int i=0; i<contentIds.size()&&k<retList.size(); i++) {
//                    o=fromCache.get(contentIds.get(i));
//                    String oneCataId=(String)o.remove("ThisCataId");
//
//                    oneCatalog=retList.get(k);
//                    while(!oneCataId.equals(oneCatalog.get("CatalogId"))) {
//                        k++;
//                        oneCatalog=retList.get(k);
//                    }
//                    if (k<retList.size()) {
//                        ((List<Map<String, Object>>)oneCatalog.get("List")).add(o);
//                    }
//                }
//                ret.put("BeginCatalogId", beginCatalogId);
//                ret.put("PageSize", fromCache.size()-1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
            if (ps!=null) try {ps.close();ps=null;} catch(Exception e) {ps=null;} finally {ps=null;};
            if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
        }
        return ret.isEmpty()?null:ret;
    }

    //根据参数获得范围Sql
    private List<String> getRangeSql(TreeNode<? extends TreeNodeBean> root) {
        List<String> ret=new ArrayList<String>();
        //1-得到媒体类型过滤串
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

        //获得过滤的内容
        String f_catalogType="", f_catalogId="", filterSql_innerJoin="";
        if (filterData!=null) {//若有过滤，类别过滤
            f_catalogType=filterData.get("CatalogType")==null?"-1":(filterData.get("CatalogType")+"");
            f_catalogId=filterData.get("CatalogId")==null?null:(filterData.get("CatalogId")+"");

            TreeNode<? extends TreeNodeBean> _root=null;
            //根据分类获得根
            if (f_catalogType.equals("-1")) _root=_cc.channelTree;
            else {
                DictModel dm=_cd.getDictModelById(f_catalogType);
                if (dm!=null&&dm.dictTree!=null) _root=dm.dictTree;
            }
            if (_root!=null&&!StringUtils.isNullOrEmptyOrSpace(f_catalogId)) _root=_root.findNode(f_catalogId);

            if (_root!=null) {
                String _idCName="channelId", _typeCName="assetType", _resIdCName="assetId", _tableName="wt_ChannelAsset";
                if (!f_catalogType.equals("-1")) {
                    _idCName="dictDid";
                    _typeCName="resTableName";
                    _resIdCName="resId";
                    _tableName="wt_ResDict_Ref";
                }
                filterSql_innerJoin=" inner join "+_tableName+" c on a."+typeCName+"=c."+_typeCName+" and a."+resIdCName+"=c."+_resIdCName+" and "+(mediaFilterSql.equals("")?"":("("+mediaFilterSql+") and "));
                if (f_catalogType.equals("-1")) {
                    filterSql_innerJoin+="c.isValidate=1 and c.flowFlag=2";
                } else {
                    filterSql_innerJoin+="c.dictMid='"+f_catalogType+"'";
                }
                String _orSql="";
                List<TreeNode<? extends TreeNodeBean>> allTn=TreeUtils.getDeepList(_root);
                if (allTn!=null&&!allTn.isEmpty()) {
                    for (TreeNode<? extends TreeNodeBean> tn: allTn) {
                        _orSql+=" or c."+_idCName+"='"+tn.getId()+"'";
                    }
                }
                if (_orSql.length()>4) _orSql=_orSql.substring(4);
                if (_orSql.length()>0) filterSql_innerJoin+=" and (c."+_idCName+"='"+_root.getId()+"' or ("+_orSql+"))";
                else filterSql_innerJoin+=" and (c."+_idCName+"='"+_root.getId()+")";
            }
        }
        String orSql="", orderBySql = "", getContentSql="", getCountSql="", wtChannelIsPub="";
        if (resultType==3) { //按列表返回
            if (!root.isRoot()) {
                orSql+=" or a."+idCName+"='"+root.getId()+"'";
                orderBySql += ",'"+root.getId()+"'";
                if (recursionTree==1) {
                    List<TreeNode<? extends TreeNodeBean>> allTn=TreeUtils.getDeepList(root);
                    if (allTn!=null&&!allTn.isEmpty()) {
                        for (TreeNode<? extends TreeNodeBean> tn: allTn) {
                            orSql+=" or a."+idCName+"='"+tn.getId()+"'";
                            orderBySql+=",'"+tn.getId()+"'";
                        }
                    }
                }
            }

            if (!catalogType.equals("-1")&&!f_catalogType.equals("-1")) {//主分类是字典，过滤不是栏目
                wtChannelIsPub=" inner join wt_ChannelAsset b on a.resTableName=b.assetType and a.resId=b.assetId and b.isValidate=1 and b.flowFlag=2"+(mediaFilterSql.equals("")?"":(" and ("+mediaFilterSql+")"));
            }
            getContentSql="select distinct a."+typeCName+", a."+resIdCName+" from "+tableName+" a";
            if (!StringUtils.isNullOrEmptyOrSpace(wtChannelIsPub)) getContentSql+=" "+wtChannelIsPub;
            if (!StringUtils.isNullOrEmptyOrSpace(filterSql_innerJoin)) getContentSql+=" "+filterSql_innerJoin;
            getContentSql+=" where "+(catalogType.equals("-1")?" a.isValidate=1 and a.flowFlag=2":" (a.dictMid='"+catalogType+"')");
            if (mediaFilterSql.length()>0) getContentSql+=" and ("+mediaFilterSql+")";
            if (orSql.length()>0) getContentSql+=" and ("+orSql.substring(4)+")";
            getCountSql="select count(*) from ("+getContentSql+") as b";
            if (catalogType.equals("-1")) { //栏目
                if (!root.isLeaf()) getContentSql+=" order by a.pubTime desc";
                else getContentSql+=" order by a.topSort desc, a.pubTime desc";
            } else { //分类
                getContentSql+=" order by field(a.dictDid"+orderBySql+")";
                String flag=(StringUtils.isNullOrEmptyOrSpace(wtChannelIsPub)?"c":"b");
                if (!root.isLeaf()) getContentSql+=","+flag+".pubTime desc";
                else getContentSql+=","+flag+".topSort desc, "+flag+".pubTime desc";
            }
            //加入电台的特殊处理
            if (mediaType!=null&&mediaType.equals("RADIO")) {
                //联合中央台
                String nationalSql="select distinct a.assetType, a.assetId from wt_ChannelAsset a";
                if (!StringUtils.isNullOrEmptyOrSpace(catalogId)) {
                    if (catalogType.equals("-1")) nationalSql+=" inner join wt_ChannelAsset b on a.assetType=b.assetType and a.assetId=b.assetId and b.channelId='"+catalogId+"' ";
                    else nationalSql+=" inner join wt_ResDict_Ref b on a.assetType=b.resTableName and a.assetId=b.resId and b.dictDid='"+catalogId+"' and b.dictMid='"+catalogType+"' ";
                }
                nationalSql+="where a.channelId='dtfl2001_1' and a.assetType='wt_Broadcast' and a.flowFlag=2 and a.isValidate=1";
                getContentSql="select * from ("+getContentSql+") as getSql union "+nationalSql;
                //找到所属地区
                String areaId="";
                if (f_catalogType.equals("2")) areaId=f_catalogId;
                if (catalogType!=null&&catalogType.equals("2")&&StringUtils.isNullOrEmpty(areaId)) areaId=catalogId;
                //找到所有的对应的其他电台，按地区排序
                List<String> neighborIds=new ArrayList<String>();
                try {
                    if (!StringUtils.isNullOrEmptyOrSpace(areaId)) {
                        TreeNode<? extends TreeNodeBean> xzqhRoot=null;
                        DictModel dm=_cd.getDictModelById("2");
                        if (dm!=null&&dm.dictTree!=null) xzqhRoot=dm.dictTree;
                        if (xzqhRoot!=null) {
                            ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
                            if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                                JedisConnectionFactory conn=(JedisConnectionFactory)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("connectionFactory182");
                                RedisOperService ros=new RedisOperService(conn, 5);
                                String neighbors=ros.get(areaId);
                                if (!StringUtils.isNullOrEmptyOrSpace(neighbors)) {
                                    if (xzqhRoot!=null) {
                                        String[] _neighbors=neighbors.split(",");
                                        for (int i=0;i<_neighbors.length;i++) {
                                            if (i>4) break;
                                            neighborIds.add(_neighbors[i]);
                                            TreeNode<? extends TreeNodeBean> areaNode=xzqhRoot.findNode(_neighbors[i]);
                                            if (areaNode!=null) {
                                                List<TreeNode<? extends TreeNodeBean>> inTreeNodes=TreeUtils.getDeepList(areaNode);
                                                if (inTreeNodes!=null&&!inTreeNodes.isEmpty()) {
                                                    for (TreeNode<? extends TreeNodeBean> tn: inTreeNodes) {
                                                        String tmpId=tn.getId();
                                                        if (tmpId.startsWith("11")||tmpId.startsWith("12")||tmpId.startsWith("31")||tmpId.startsWith("50")) {
                                                            neighborIds.add(tn.getId());
                                                        } else {
                                                            if (!tmpId.endsWith("00")) neighborIds.add(tn.getId());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch(Exception e) {
                }
                String neighborSql=null;
                if (!neighborIds.isEmpty()) {
                    neighborSql="select distinct a.resTableName, a.resId from wt_ResDict_Ref a ";
                    if (!StringUtils.isNullOrEmptyOrSpace(catalogId)) {
                        if (catalogType.equals("-1")) neighborSql+=" inner join wt_ChannelAsset b on a.resTableName=b.assetType and a.resId=b.assetId and b.channelId='"+catalogId+"' and isValidate=1 and flowFlag=2";
                        else {
                            neighborSql+=" inner join wt_ResDict_Ref  b on a.resTableName=b.resTableName and a.resId=b.resId and b.dictDid='"+catalogId+"' and b.dictMid='"+catalogType+"' ";
                            neighborSql+=" inner join wt_ChannelAsset c on a.resTableName=c.assetType and a.resId=c.assetId and c.isValidate=1 and c.flowFlag=2";
                        }
                    }
                    neighborSql+=" where a.resTableName='wt_Broadcast' and a.dictMid='2'";
                    if (catalogType.equals("-1")) {
                        if (!StringUtils.isNullOrEmptyOrSpace(catalogId)) neighborSql+=" order by field(b.channelId, '"+catalogId+"'), field(a.dictDid";
                        else neighborSql+=" order by field(a.dictDid";
                    } else {
                        neighborSql+=" order by field(a.dictDid";
                    }
                    for (int i=neighborIds.size()-1; i>=0; i--) {
                        neighborSql+=",'"+neighborIds.get(i)+"'";
                    }
                    neighborSql+=") desc";
                }
                if (!StringUtils.isNullOrEmptyOrSpace(neighborSql)) {
                    getContentSql+=" union select * from ("+neighborSql+") as neighbor";
                }
                getContentSql="select * from ("+getContentSql+") as allSql ";
            }
            getContentSql+=" limit "+(((page<=0?1:page)-1)*pageSize)+","+pageSize; //分页
            ret.add(getContentSql);
            ret.add(getCountSql);
        } else if (resultType==2) { //按内容类型返回，先不做
//            if (!root.isRoot()) {
//                orSql+=" or a."+idCName+"='"+root.getId()+"'";
//                orderBySql += ",'"+root.getId()+"'";
//                if (recursionTree==1) {
//                    List<TreeNode<? extends TreeNodeBean>> allTn=TreeUtils.getDeepList(root);
//                    if (allTn!=null&&!allTn.isEmpty()) {
//                        for (TreeNode<? extends TreeNodeBean> tn: allTn) {
//                            orSql+=" or a."+idCName+"='"+tn.getId()+"'";
//                            orderBySql+=",'"+tn.getId()+"'";
//                        }
//                    }
//                }
//            }
//            if (orSql.length()>0) {
//                orSql=catalogType.equals("-1")?orSql.substring(4):" a.dictMid='"+catalogType+"' and ("+orSql.substring(4)+")";
//            }
//
//            if (!catalogType.equals("-1")) getContentSql=", wt_ChannelAsset b";
//            getContentSql="select a."+typeCName+", a."+resIdCName+" from "+tableName+" a"+getContentSql+" where";
//            getContentSql+=(catalogType.equals("-1")?" a.isValidate=1 and a.flowFlag=2":" (a.dictMid='"+catalogType+"') and (a."+typeCName+"=b.assetType and a."+resIdCName+"=b.assetId and b.isValidate=1 and b.flowFlag=2)");
//            getContentSql+="#mediaType#";
//            if (mediaFilterSql.length()>0) getContentSql+=" and ("+mediaFilterSql+")";
//            if (orSql.length()>0) getContentSql+=" and ("+orSql+")";
//            getCountSql="select count(*) from ("+getContentSql.replaceAll("#mediaType#", "")+") as b";
//            if (catalogType.equals("-1")) {//栏目
//                if (!root.isLeaf()) getContentSql+=" order by a.pubTime desc";
//                else getContentSql+=" order by a.topSort desc, a.pubTime desc";
//            } else {//分类
//                getContentSql+=" order by field(a.dictDid,"+orderBySql+")";
//                if (!root.isLeaf()) getContentSql+=",b.pubTime desc";
//                else getContentSql+=",b.topSort desc, b.pubTime desc";
//            }
//            String audioSql, radioSql, seqSql;
//            audioSql="select 'AUDIO' ThisCataId, audio.* from ("+getContentSql.replaceAll("#mediaType#", " and (a."+typeCName+"='wt_MediaAsset')")+" limit "+perSize+") as audio";
//            radioSql="select 'AUDIO' ThisCataId, radio.* from ("+getContentSql.replaceAll("#mediaType#", " and (a."+typeCName+"='wt_Broadcast')")+" limit "+perSize+") as radio";
//            seqSql="select 'AUDIO' ThisCataId, sequ.* from ("+getContentSql.replaceAll("#mediaType#", " and (a."+typeCName+"='wt_SeqMediaAsset')")+" limit "+perSize+") as sequ";
//            getContentSql=audioSql+" union all "+radioSql+" union all "+seqSql;
        } else if (resultType==1) { //按分类下级返回
            if (beginCatalogId==null||!beginCatalogId.equals("ENDEND")) {
                List<TreeNode<? extends TreeNodeBean>> subCata=new ArrayList<TreeNode<? extends TreeNodeBean>>();
                for (TreeNode<? extends TreeNodeBean> _stn: root.getChildren()) subCata.add(_stn);
                if (catalogType.equals("2")&&catalogId!=null&&(catalogId.equals("110000")||catalogId.equals("120000")||catalogId.equals("310000")||catalogId.equals("500000"))) {//对行政区划做特别的处理
                    List<TreeNode<? extends TreeNodeBean>> subCata1=new ArrayList<TreeNode<? extends TreeNodeBean>>();
                    for (int i=0; i<subCata.size(); i++) {
                        TreeNode<? extends TreeNodeBean> _stn=subCata.get(i);
                        if (_stn.isLeaf()) subCata1.add(_stn);
                        else {
                            for (TreeNode<? extends TreeNodeBean> _stn1: _stn.getChildren()) subCata1.add(_stn1);
                        }
                    }
                    subCata=subCata1;
                }
                if (!catalogType.equals("-1")&&!f_catalogType.equals("-1")) {//主分类是字典，过滤不是栏目
                    wtChannelIsPub=" inner join wt_ChannelAsset b on a.resTableName=b.assetType and a.resId=b.assetId and b.isValidate=1 and b.flowFlag=2"+(mediaFilterSql.equals("")?"":(" and ("+mediaFilterSql+")"));
                }
                getContentSql="select distinct a."+typeCName+", a."+resIdCName+" from "+tableName+" a";
                if (!StringUtils.isNullOrEmptyOrSpace(wtChannelIsPub)) getContentSql+=" "+wtChannelIsPub;
                if (!StringUtils.isNullOrEmptyOrSpace(filterSql_innerJoin)) getContentSql+=" "+filterSql_innerJoin;
                getContentSql+=" where "+(catalogType.equals("-1")?" a.isValidate=1 and a.flowFlag=2":" (a.dictMid='"+catalogType+"')");
                if (mediaFilterSql.length()>0) getContentSql+=" and ("+mediaFilterSql+")";
                getContentSql+="#subOr#";

                String oneCataSql="";
                boolean canBegin=StringUtils.isNullOrEmpty(beginCatalogId);

                //为电台找到所属地区
                List<String> neighborIds=new ArrayList<String>();
                if (mediaType!=null&&mediaType.equals("RADIO")) {
                    String areaId="";
                    if (f_catalogType.equals("2")) areaId=f_catalogId;
                    if (catalogType!=null&&catalogType.equals("2")&&StringUtils.isNullOrEmpty(areaId)) areaId=catalogId;
                    //找到所有的对应的其他电台，按地区排序
                    try {
                        if (!StringUtils.isNullOrEmptyOrSpace(areaId)) {
                            TreeNode<? extends TreeNodeBean> xzqhRoot=null;
                            DictModel dm=_cd.getDictModelById("2");
                            if (dm!=null&&dm.dictTree!=null) xzqhRoot=dm.dictTree;
                            if (xzqhRoot!=null) {
                                ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
                                if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                                    JedisConnectionFactory conn=(JedisConnectionFactory)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("connectionFactory182");
                                    RedisOperService ros=new RedisOperService(conn, 5);
                                    String neighbors=ros.get(areaId);
                                    if (!StringUtils.isNullOrEmptyOrSpace(neighbors)) {
                                        if (xzqhRoot!=null) {
                                            String[] _neighbors=neighbors.split(",");
                                            for (int k=0;k<_neighbors.length;k++) {
                                                if (k>4) break;
                                                neighborIds.add(_neighbors[k]);
                                                TreeNode<? extends TreeNodeBean> areaNode=xzqhRoot.findNode(_neighbors[k]);
                                                if (areaNode!=null) {
                                                    List<TreeNode<? extends TreeNodeBean>> inTreeNodes=TreeUtils.getDeepList(areaNode);
                                                    if (inTreeNodes!=null&&!inTreeNodes.isEmpty()) {
                                                        for (TreeNode<? extends TreeNodeBean> tn: inTreeNodes) {
                                                            String tmpId=tn.getId();
                                                            if (tmpId.startsWith("11")||tmpId.startsWith("12")||tmpId.startsWith("31")||tmpId.startsWith("50")) {
                                                                neighborIds.add(tn.getId());
                                                            } else {
                                                                if (!tmpId.endsWith("00")) neighborIds.add(tn.getId());
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch(Exception e) {
                    }
                }

                for (int i=0; i<subCata.size(); i++) {
                    TreeNode<? extends TreeNodeBean> _stn=subCata.get(i);
                    canBegin=canBegin||_stn.getId().equals(beginCatalogId);
                    if (!canBegin) continue;
                    orSql=" or a."+idCName+"='"+_stn.getId()+"'";
                    if (recursionTree==1&&!_stn.isLeaf()) {
                        List<TreeNode<? extends TreeNodeBean>> allTn=TreeUtils.getDeepList(_stn);
                        if (allTn!=null&&!allTn.isEmpty()) {
                            for (TreeNode<? extends TreeNodeBean> tn: allTn) orSql+=" or a."+idCName+"='"+tn.getId()+"'";
                        }
                    }
                    if (orSql.length()>0) {
                        orSql=catalogType.equals("-1")?orSql.substring(4):" a.dictMid='"+catalogType+"' and ("+orSql.substring(4)+")";
                    }
                    //先不加排序，太慢了
                    oneCataSql=getContentSql;
                    if (catalogType.equals("-1")) {//栏目
                        if (!_stn.isLeaf()) oneCataSql+=" order by a.pubTime desc";
                        else oneCataSql+=" order by a.topSort desc, a.pubTime desc";
                    } else {//分类
                        oneCataSql+=" order by ";
                        if (!StringUtils.isNullOrEmpty(catalogId)) oneCataSql+="field(a.dictDid,'"+catalogId+"'),";
                        if (!_stn.isLeaf()) oneCataSql+=" b.pubTime desc";
                        else oneCataSql+=" b.topSort desc, b.pubTime desc";
                    }
                    oneCataSql=oneCataSql.replaceAll("#subOr#", " and ("+orSql+") ")+" limit "+perSize;
                    oneCataSql="select '"+_stn.getId()+"' ThisCataId, cidt.* from ("+oneCataSql+") cidt";
                    //加入电台的特殊处理
                    if (mediaType!=null&&mediaType.equals("RADIO")) {
                        //为填充其他信息做准备
                        String extSql=orSql.replaceAll("a\\.", "b\\.");
                        //联合中央台
                        String nationalSql="select distinct a.assetType, a.assetId from wt_ChannelAsset a ";
                        if (!StringUtils.isNullOrEmptyOrSpace(catalogId)) {
                            if (catalogType.equals("-1")) nationalSql+=" inner join wt_ChannelAsset b on a.assetType=b.assetType and a.assetId=b.assetId and ("+extSql+") ";
                            else nationalSql+=" inner join wt_ResDict_Ref b on a.assetType=b.resTableName and a.assetId=b.resId and b.dictMid='"+catalogType+"' and ("+extSql+") ";
                        }
                        nationalSql+="where a.channelId='dtfl2001_1' and a.assetType='wt_Broadcast' and a.flowFlag=2 and a.isValidate=1";
                        nationalSql="select '"+_stn.getId()+"' ThisCataId, national.* from ("+nationalSql+" limit "+perSize+") national";
                        oneCataSql=oneCataSql+" union "+nationalSql;
                        String neighborSql=null;
                        if (!neighborIds.isEmpty()) {
                            neighborSql="select distinct a.resTableName, a.resId from wt_ResDict_Ref a ";
                            if (!StringUtils.isNullOrEmptyOrSpace(catalogId)) {
                                if (catalogType.equals("-1")) neighborSql+=" inner join wt_ChannelAsset b on a.resTableName=b.assetType and a.resId=b.assetId and isValidate=1 and flowFlag=2 and ("+extSql+")";
                                else {
                                    neighborSql+=" inner join wt_ResDict_Ref  b on a.resTableName=b.resTableName and a.resId=b.resId' and b.dictMid='"+catalogType+"' and ("+extSql+")";
                                    neighborSql+=" inner join wt_ChannelAsset c on a.resTableName=c.assetType and a.resId=c.assetId and c.isValidate=1 and c.flowFlag=2";
                                }
                            }
                            neighborSql+=" where a.resTableName='wt_Broadcast' and a.dictMid='2'";
                            if (catalogType.equals("-1")) {
                                if (!StringUtils.isNullOrEmptyOrSpace(catalogId)) neighborSql+=" order by field(b.channelId, '"+catalogId+"'), field(a.dictDid";
                                else neighborSql+=" order by field(a.dictDid";
                            } else {
                                neighborSql+=" order by field(a.dictDid";
                            }
                            for (int j=neighborIds.size()-1; j>=0; j--) {
                                neighborSql+=",'"+neighborIds.get(j)+"'";
                            }
                            neighborSql+=") desc";
                        }
                        if (!StringUtils.isNullOrEmptyOrSpace(neighborSql)) {
                            neighborSql="select '"+_stn.getId()+"' ThisCataId, neighbor.* from ("+neighborSql+" limit "+perSize+") neighbor";
                            oneCataSql=oneCataSql+" union "+neighborSql;
                        }
                    }
                    ret.add("select * from ("+oneCataSql+") as allSql limit "+perSize);
                }
            }
        }
        if (ret.isEmpty()) return null;
        return ret;
    }
    /**
     * 获得内容快照
     * @param catchDBIds，快照Id的列表（排好顺序的，仅包括Id）形如【AUDIO_34weqr34evad245sgaer23432，RADIO_34weqr34evad245sgaer23431, SEQU_34weqr34evad245sgaer23433】
     * @param nullIsLoad 当cacheDB未包含id信息时是否从总库中加载这条记录
     * @return 返回结果包含内容信息，栏目信息，字典信息，专辑下级节目信息，其排序要与catchDBIds相同
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Map<String, Map<String, Object>> getMediaContentListFromCacheDB(List<String> cacheDBIds, boolean nullIsLoad) {
        if (cacheDBIds==null||cacheDBIds.isEmpty()) return null;
        String orSql="", orderSql="", tmpStr=null;
        for (int i=0; i<cacheDBIds.size(); i++) {
            if (cacheDBIds.get(i)==null) continue;
            tmpStr="'"+cacheDBIds.get(i)+"_INFO'";
            orSql+=" or id="+tmpStr;
            orderSql+=","+tmpStr;
            if (cacheDBIds.get(i).startsWith("SEQU_")&&pageType==0) {
                tmpStr="'"+cacheDBIds.get(i)+"_SUBLIST'";
                orSql+=" or id="+tmpStr;
                orderSql+=","+tmpStr;
            }
        }
        orSql="select * from wt_CacheDB where "+orSql.substring(4)+" order by field(id, "+orderSql.substring(1)+")";
        Connection conn=null;
        PreparedStatement ps=null;//获得所需的记录的id
        ResultSet rs=null;
        try {
            conn=cacheDataSource.getConnection();
            ps=conn.prepareStatement(orSql);
            rs=ps.executeQuery();
            List<String> cacheContentList=new ArrayList<String>();
            while (rs!=null&&rs.next()) {
                cacheContentList.add(rs.getString("id")+"::"+rs.getString("value"));
            }
            try {
                rs.close();
                ps.close();
            } finally {
                rs=null;
                ps=null;
            }

            List<String> loadContentList=new ArrayList<String>();
            Map<String, Object> audioMap=new HashMap<String, Object>();//存储单体内容的数据
            Map<String, Object> tempM=null;
            //第一次组织返回值
            List<Map<String, Object>> tempRet=new ArrayList<Map<String, Object>>();
            for (int i=0; i<cacheDBIds.size(); i++) tempRet.add(null);
            String[] splitOneCacheInfo=null;
            int j=0, k=0;
            for (; j<cacheDBIds.size()&&k<cacheContentList.size(); j++) {
                tmpStr=cacheDBIds.get(j);
                orSql=cacheContentList.get(k);
                splitOneCacheInfo=orSql.split("::");
                if (splitOneCacheInfo.length!=2) k++;
                else if ((tmpStr+"_INFO").equals(splitOneCacheInfo[0])) {
                    k++;
                    tempM=null;
                    try {
                        tempM=(Map<String, Object>)JsonUtils.jsonToObj(splitOneCacheInfo[1], Map.class);
                    } catch(Exception e) {
                    }
                    if (tempM!=null) {
                        tempRet.remove(j);
                        tempRet.add(j, tempM);
                        if (tmpStr.startsWith("AUDIO")) audioMap.put(tmpStr, tempM);
                        if (tmpStr.startsWith("SEQU")&&pageType==0) {
                            orSql=cacheContentList.get(k);
                            splitOneCacheInfo=orSql.split("::");
                            if (splitOneCacheInfo.length!=2) k++;
                            else {
                                if ((tmpStr+"_SUBLIST").equals(splitOneCacheInfo[0])) {
                                    List<String> l=new ArrayList<String>();
                                    List<String> smaSubs=(List<String>)JsonUtils.jsonToObj(splitOneCacheInfo[1], List.class);
                                    if (smaSubs!=null&&smaSubs.size()>0) {
                                        orderSql=smaSubs.get(0);
                                        l.add(orderSql);
                                        if (audioMap.get(orderSql)==null) audioMap.put(orderSql, null);
                                    }
                                    tempM.put("SubList", l);
                                    k++;
                                }
                            }
                        }
                    }
                }
            }
            //看是否有不存在的内容
            for (int i=0; i<cacheDBIds.size(); i++) {
                if (tempRet.get(i)==null) loadContentList.add(cacheDBIds.get(i));
            }
            //获得Seq下面的单体内容
            orSql="";
            for (String audioId: audioMap.keySet()) {
                if (audioMap.get(audioId)==null) orSql+=" or id='"+audioId+"_INFO'";
            }
            if (orSql.length()>4) {
                orSql="select * from wt_CacheDB where "+orSql.substring(4);
                ps=conn.prepareStatement(orSql);
                rs=ps.executeQuery();
                while (rs!=null&&rs.next()) {
                    String mapId=rs.getString("id");
                    mapId=mapId.substring(0, mapId.length()-5);
                    audioMap.put(mapId, JsonUtils.jsonToObj(rs.getString("value"), Map.class));
                }
                try {
                    rs.close();
                    ps.close();
                } finally {
                    rs=null;
                    ps=null;
                }
            }
            //补充不存在的内容
            for (String audioId: audioMap.keySet()) {
                if (audioMap.get(audioId)==null) loadContentList.add("AUDIO_"+audioId);
            }
            //多线程获取不存在的内容
            if (nullIsLoad&&loadContentList.size()>0) {
                ExecutorService fixedThreadPool = Executors.newFixedThreadPool(loadContentList.size());
                for (int i=0; i<loadContentList.size(); i++) {
                    String loadId=loadContentList.get(i);
                    String[] params=loadId.split("_");
                    String type=params[0];
                    String id=params[1];
                    fixedThreadPool.execute(new Runnable() {
                        public void run() {
                            try {
                                Map<String, Object> contentInfo=null;
                                if (type.equals("RADIO")) contentInfo=contentService.getBcInfo4Cache(id);
                                else if (type.equals("AUDIO")) contentInfo=contentService.getMaInfo4Cache(id);
                                else if (type.equals("SEQU")) contentInfo=contentService.getSeqMaInfo4Cache(id);

                                if (contentInfo!=null) {
                                    if (type.equals("SEQU")) {
                                        List<String> subList=(List<String>)contentInfo.get("SubListStr");
                                        CacheDBPo cacheDBPo=new CacheDBPo();
                                        cacheDBPo.setId("SEQU_"+id+"_SUBLIST");
                                        cacheDBPo.setResTableName("wt_SeqMediaAsset");
                                        cacheDBPo.setResId(id);
                                        if (subList==null) cacheDBPo.setValue("[]");
                                        else cacheDBPo.setValue(JsonUtils.objToJson(subList));
                                        (new AddCacheDB(null, null)).start();

                                        contentInfo=(Map<String, Object>)contentInfo.get("MainInfo");
                                    }
                                    for (int i=0; i<cacheDBIds.size(); i++) {
                                        if (loadId.equals(cacheDBIds.get(i))) {
                                            tempRet.remove(i);
                                            tempRet.add(i, contentInfo);
                                            break;
                                        }
                                    }
                                    if (type.equals("AUDIO")) audioMap.put(loadId, contentInfo);
                                    (new AddCacheDB(contentInfo, null)).start();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                fixedThreadPool.shutdown();
                while (true) {
                    if (fixedThreadPool.isTerminated()) break;
                    try { Thread.sleep(50); } catch (Exception e) {e.printStackTrace();}
                }
            }
            //最后的组装
            Map<String, Map<String, Object>> ret=new HashMap<String, Map<String, Object>>();
            for (int i=0; i<tempRet.size(); i++) {
                Map<String, Object> one=tempRet.get(i);
                if (one!=null) {
                    if (cacheDBIds.get(i).startsWith("SEQU_")) {//组装SubList
                        List<String> subs=(List<String>)one.get("SubList");
                        if (subs!=null&&!subs.isEmpty()) {
                            List<Map<String, Object>> newSubList=new ArrayList<Map<String, Object>>();
                            for (j=0; j<subs.size(); j++) {
                                if (subs.get(j)!=null) {
                                    Map<String, Object> a=(Map<String, Object>)audioMap.get(subs.get(j));
                                    if (a!=null) {
                                        newSubList.add(a);
                                    }
                                }
                            }
                            if (!newSubList.isEmpty()) one.put("SubList", newSubList);
                            else one.remove("SubList");
                        }
                        
                    }
                    if (pageType==0&&one.get("SubList")!=null) {
                        if (!((List)one.get("SubList")).isEmpty()) {
                            Map newOne=(Map)((List)one.get("SubList")).get(0);
                            if (newOne!=null) {
                                one.remove("SubList");
                                newOne.put("SeqInfo", one);
                                one=newOne;
                            }
                        }
                    }
                    ret.put(cacheDBIds.get(i), one);
                }
            }
            return ret.isEmpty()?null:ret;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
            if (ps!=null) try {ps.close();ps=null;} catch(Exception e) {ps=null;} finally {ps=null;};
            if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
        }
        return null;
    }

    /**
     * 私有线程类，用于多线程写入缓存数据库
     * @author wanghui
     */
    private class AddCacheDB extends Thread {
        CacheDBPo cPo=null;
        Map<String, Object> contentInfo;
        public AddCacheDB(Map<String, Object> contentInfo, CacheDBPo cPo) {
            this.contentInfo=contentInfo;
            this.cPo=cPo;
        }
        @Override
        public void run() {
            if ((contentInfo==null||contentInfo.isEmpty())&&(cPo==null)) return;
            if (contentInfo!=null&&!contentInfo.isEmpty()) {
                MediaType MT=MediaType.buildByTypeName(contentInfo.get("MediaType")+"");
                CacheDBPo cacheDBPo=new CacheDBPo();
                cacheDBPo.setId(contentInfo.get("MediaType")+"_"+contentInfo.get("ContentId")+"_INFO");
                cacheDBPo.setResTableName(MT.getTabName());
                cacheDBPo.setResId(contentInfo.get("ContentId")+"");
                cacheDBPo.setValue(JsonUtils.objToJson(contentInfo));
                cacheDBService.insertCacheDBPo(cacheDBPo);
            }
            if (cPo!=null) {
                cacheDBService.insertCacheDBPo(cPo);
            }
        }
    }
}