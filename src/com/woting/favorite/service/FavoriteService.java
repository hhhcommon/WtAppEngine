package com.woting.favorite.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.core.model.Page;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.utils.ContentUtils;
import com.woting.cm.core.channel.service.ChannelService;
import com.woting.favorite.persis.po.UserFavoritePo;
import com.woting.passport.UGA.persis.pojo.GroupPo;
import com.woting.passport.mobile.MobileUDKey;

@Lazy(true)
@Service
public class FavoriteService {
    @Resource
    private ChannelService channelService;
    @Resource(name="defaultDAO")
    private MybatisDAO<UserFavoritePo> userFavoriteDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<GroupPo> groupDao;
    @Resource
    private DataSource dataSource;

    @PostConstruct
    public void initParam() {
        groupDao.setNamespace("WT_GROUP");
        userFavoriteDao.setNamespace("DA_USERFAVORITE");
    }

    /**
     * 喜欢或取消喜欢某个内容
     * @param mediaType 内容类型
     * @param contentId 内容Id
     * @param flag 操作类型:=1喜欢；=0取消喜欢，默认=1
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return 若成功返回1；若是喜欢：0=所指定的节目不存在，2=已经喜欢了此内容；若是取消喜欢：-1=还未喜欢此内容；
     *          -100——内容类型不符合要求
     */
    public int favorite(String mediaType, String contentId, int flag, MobileUDKey mUdk) {
        String CType=mediaType.toUpperCase();
        if (!CType.equals("RADIO")&&!CType.equals("AUDIO")&&!CType.equals("SEQU")&&!CType.equals("TEXT")) return -100;

        String assetType=ContentUtils.getResTableName(mediaType);
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("resTableName", assetType);
        param.put("resId", contentId);

        if (flag==1) {
            if (!channelService.isPub(assetType, contentId)) return 0;
            if (mUdk.isUser()) {
                param.put("ownerType", "201");
                param.put("ownerId", mUdk.getUserId());
            } else {
                param.put("ownerType", "202");
                param.put("ownerId", mUdk.getDeviceId());
            }
            if (userFavoriteDao.getCount(param)>0) return 2;
            param.put("id", SequenceUUID.getUUIDSubSegment(4));
            userFavoriteDao.insert(param);//加入喜欢队列
        } else {
            param.put("mobileId", mUdk.getDeviceId());
            if (mUdk.isUser()) param.put("userId", mUdk.getUserId());
            if (userFavoriteDao.getCount("getCount4Favorite", param)==0) return -1;
            //设备删除
            param.put("ownerType", "202");
            param.put("ownerId", mUdk.getDeviceId());
            userFavoriteDao.delete("deleteByEntity",param);
            //用户删除
            if (mUdk.isUser()) {
                param.put("ownerType", "201");
                param.put("ownerId", mUdk.getUserId());
                userFavoriteDao.delete("deleteByEntity",param);
            }
        }
        return 1;
    }

    /**
     * 得到某人或设备的喜欢内容按列表
     * @param resultType 返回类型：2：按媒体性质分类：电台/单体/专辑；3：列表
     * @param pageType 页面类型：默认值1；=0：在播放页显示，把专辑的第一个节目提取出来；=1：在内容页显示，专辑和节目独立处理
     * @param mediaType 过滤条件，若没有，则全部获取，注意，这里的mediaType可以是一个，也可以是多个,用逗号隔开
     * @param pageSize 没页个数
     * @param page 页数
     * @param perSize 每分类条目数
     * @param beginCatalogId 开始分类Id
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return 该页内容
     */
    //注意这里的内容分类目前只有Radio,Audio,Sequ,Text四种，而且就是按这个顺序排列的
    @SuppressWarnings({ "null", "unchecked" })
    public Map<String, Object> getFavoriteList(int resultType, int pageType, String mediaType, int pageSize, int page, int perSize, String beginCatalogId, MobileUDKey mUdk) {
        Map<String, Object> param=new HashMap<String, Object>();//返回数据，以及查询参数

        param.put("mobileId", mUdk.getDeviceId());
        if (mUdk.isUser()) param.put("userId", mUdk.getUserId());

        Page<UserFavoritePo> resultPage=null;
        List<Map<String, Object>> fList=new ArrayList<Map<String, Object>>();

        //一、根据不同的条件获得喜欢内容的标引列表
        if (resultType==2) {//得到喜欢的分类列表
            param.put("sortByClause", "CTime desc");

            boolean isBeginCatalog=StringUtils.isNullOrEmptyOrSpace(beginCatalogId)||beginCatalogId.equals("RADIO");
            boolean canContinue=true;
            if (isBeginCatalog&&canContinue&&(StringUtils.isNullOrEmptyOrSpace(mediaType)||mediaType.indexOf("RADIO")!=-1)) {//得到Radio
                canContinue=fillTypeList("RADIO", fList, perSize, pageSize, param);
            }
            isBeginCatalog=StringUtils.isNullOrEmptyOrSpace(beginCatalogId)||beginCatalogId.equals("AUDIO");
            if (isBeginCatalog&&canContinue&&(StringUtils.isNullOrEmptyOrSpace(mediaType)||mediaType.indexOf("AUDIO")!=-1)) {//得到Audio
                canContinue=fillTypeList("AUDIO", fList, perSize, pageSize, param);
            }
            isBeginCatalog=StringUtils.isNullOrEmptyOrSpace(beginCatalogId)||beginCatalogId.equals("SEQU");
            if (isBeginCatalog&&canContinue&&(StringUtils.isNullOrEmptyOrSpace(mediaType)||mediaType.indexOf("SEQU")!=-1)) {//得到Sequ
                canContinue=fillTypeList("SEQU", fList, perSize, pageSize, param);
            }
            /* if (isBeginCatalog&&canContinue&&(StringUtils.isNullOrEmptyOrSpace(mediaType)||mediaType.indexOf("TEXT")!=-1)) {//得到Text，目前没有
                canContinue=fillTypeList("TEXT", fList, perSize, pageSize, param);
            }*/
        } else {//得到喜欢的平铺列表
            //处理过滤条件
            if (!StringUtils.isNullOrEmptyOrSpace(mediaType)) {
                String typeSql="";
                String[] types=mediaType.split(",");
                for (int i=0; i<types.length; i++) {
                    if (types[i].equals("RADIO")||types[i].equals("AUDIO")||!types[i].equals("SEQU")||!types[i].equals("TEXT")) {
                        typeSql+="or resTableName='"+ContentUtils.getResTableName(types[i])+"'";
                    }
                }
                if (typeSql.length()>0) {
                    typeSql=typeSql.substring(3);
                    param.put("mediaFilterSql", typeSql);
                }
            }
            resultPage=userFavoriteDao.pageQueryAutoTranform(null, "getFavoriteAssets", param, page, pageSize);
            if (!(resultPage==null||resultPage.getDataCount()==0||resultPage.getResult()==null||resultPage.getResult().isEmpty())) {
                for (UserFavoritePo ufPo: resultPage.getResult()) {
                    fList.add(ufPo.toHashMapAsBean());
                }
            }
        }
        if (fList==null||fList.isEmpty()) return null;

        //二、得到关联数据
        String bcIds="", maIds="", smaIds="";
        for (Map<String, Object> oneF: fList) {
            if (oneF.get("resTableName")!=null&&oneF.get("resId")!=null) {
                if ((oneF.get("resTableName")+"").equals("wt_Broadcast")) bcIds+=",'"+oneF.get("resId")+"'";
                else
                if ((oneF.get("resTableName")+"").equals("wt_MediaAsset")) maIds+=",'"+oneF.get("resId")+"'";
                else
                if ((oneF.get("resTableName")+"").equals("wt_SeqMediaAsset")) smaIds+=",'"+oneF.get("resId")+"'";
            }
        }
        if (bcIds.length()>0) bcIds=bcIds.substring(1);
        if (maIds.length()>0) maIds=maIds.substring(1);
        if (smaIds.length()>0) smaIds=smaIds.substring(1);

        Map<String, Object> reParam=new HashMap<String, Object>();
        List<Map<String, Object>> personList=null;//人员
        List<Map<String, Object>> cataList=null;//分类
        if ((bcIds+maIds+smaIds).length()>0) {
            if (bcIds.length()>0) reParam.put("bcIds", bcIds);
            if (maIds.length()>0) reParam.put("maIds", maIds);
            if (smaIds.length()>0) reParam.put("smaIds", smaIds);
            //重构人员
            personList=groupDao.queryForListAutoTranform("refPersonById", reParam);
            //重构分类
            cataList=groupDao.queryForListAutoTranform("refCataById", reParam);
        }
        List<Map<String, Object>> assetList=new ArrayList<Map<String, Object>>();
        for (Map<String, Object> oneF: fList) {
            Map<String, Object> oneAsset=new HashMap<String, Object>();
            oneAsset.put("resTableName", oneF.get("resTableName"));
            oneAsset.put("resId", oneF.get("resId"));
            assetList.add(oneAsset);
        }
        List<Map<String, Object>> pubChannelList=channelService.getPubChannelList(assetList);

        //三、组装大列表
        Map<String, Object>[] favoriteArray=new Map[fList.size()];
        Map<String, Object> oneContent;

        List<Map<String, Object>> tempList;
        if (bcIds.length()>0) {
            reParam.clear();
            reParam.put("inIds", bcIds);
            tempList=groupDao.queryForListAutoTranform("getBcList", reParam);
            if (tempList!=null&&!tempList.isEmpty()) {
                for (Map<String, Object> oneCntt: tempList) {
                    oneContent=ContentUtils.convert2Bc(oneCntt, personList, cataList, pubChannelList, fList);
                    add2FavoretList(favoriteArray, oneContent, fList);
                }
            }
        }
        if (maIds.length()>0) {
            reParam.clear();
            reParam.put("inIds", maIds);
            tempList=groupDao.queryForListAutoTranform("getMaList", reParam);
            if (tempList!=null&&!tempList.isEmpty()) {
                for (Map<String, Object> oneCntt: tempList) {
                    oneContent=ContentUtils.convert2Ma(oneCntt, personList, cataList, pubChannelList, fList);
                    add2FavoretList(favoriteArray, oneContent, fList);
                }
            }
        }
        if (smaIds.length()>0) {
            reParam.clear();
            reParam.put("inIds", smaIds);
            tempList=groupDao.queryForListAutoTranform("getSeqMaList", reParam);
            if (tempList!=null&&!tempList.isEmpty()) {
                if (pageType==0) {//提取可听内容，//TODO 注意：这里有一个问题：提取单体后，可能和已有的单体重复，这个目前无法处理（特别是在分页的情况下），除非采用分布式处理；现在不处理，重复就重复吧
                    Map<String, Object> seqMaMap=new HashMap<String, Object>();
                    String _orSql="";
                    //处理专辑
                    for (Map<String, Object> oneCntt: tempList) {
                        oneContent=ContentUtils.convert2Sma(oneCntt, personList, cataList, pubChannelList, fList);
                        seqMaMap.put(oneContent.get("ContentId")+"", oneContent);
                        _orSql+=" or sma.sId='"+oneContent.get("ContentId")+"'";
                    }
                    //提取单体
                    Map<String, Object> maMap=new HashMap<String, Object>();
                    maIds="";
                    assetList.clear();
                    _orSql="select b.sId, ma.* from wt_MediaAsset as ma, ("+
                            "select max(a.mId) as mId, a.sId from wt_SeqMA_Ref as a, vWt_FirstMaInSequ as sma "+
                            "where CONCAT('SID:', a.sId, '|C:', 10000+a.columnNum,'|D:', a.cTime)=CONCAT('SID:', sma.sId, '|', sma.firstMa) and ("+_orSql.substring(4)+") group by a.sId "+
                          ") as b where ma.id=b.mId";
                    Connection conn=null;
                    PreparedStatement ps=null;
                    ResultSet rs=null;
                    try {
                        conn=dataSource.getConnection();
                        ps=conn.prepareStatement(_orSql);
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
                            oneData.put("sId", rs.getString("sId"));
                            maMap.put(rs.getString("id"), oneData);

                            maIds+=",'"+rs.getString("id")+"'";
                            Map<String, Object> oneAsset=new HashMap<String, Object>();
                            oneAsset.put("resTableName", "wt_MediaAsset");
                            oneAsset.put("resId", rs.getString("id"));
                            assetList.add(oneAsset);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
                        if (ps!=null) try {ps.close();ps=null;} catch(Exception e) {ps=null;} finally {ps=null;};
                        if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
                    }
                    List<Map<String, Object>> _fList=null;
                    if (!maMap.isEmpty()) {
                        maIds=maIds.substring(1);
                        reParam.clear();
                        reParam.put("maIds", maIds);
                        //重构人员
                        personList=groupDao.queryForListAutoTranform("refPersonById", reParam);
                        //重构分类
                        cataList=groupDao.queryForListAutoTranform("refCataById", reParam);
                        //重构发布
                        pubChannelList=channelService.getPubChannelList(assetList);
                        //重构喜欢
                        List<UserFavoritePo> __fList=getPureFavoriteList(mUdk);
                        if (__fList!=null&&__fList.size()>0) {
                            for (UserFavoritePo ufPo: __fList) _fList.add(ufPo.toHashMapAsBean());
                        }
                    }
                    Map<String, Object> maData, smaData;
                    for (Map<String, Object> oneSeqMa: tempList) {
                        maData=(Map<String, Object>)maMap.get(oneSeqMa.get("id")+"");
                        smaData=ContentUtils.convert2Ma(maData, personList, cataList, pubChannelList, _fList);
                        if (maData!=null) {
                            maData.put("SeqInfo", smaData);
                            add2FavoretList4Extract(favoriteArray, maData, fList);
                        } else {
                            add2FavoretList(favoriteArray, (Map<String, Object>)seqMaMap.get(oneSeqMa.get("id")+""), fList);
                        }
                    }
                } else {
                    for (Map<String, Object> oneCntt: tempList) {
                        oneContent=ContentUtils.convert2Sma(oneCntt, personList, cataList, pubChannelList, fList);
                        add2FavoretList(favoriteArray, oneContent, fList);
                    }
                }
            }
        }
        List<Map<String, Object>> favoriteList=new ArrayList<Map<String, Object>>();
        for (int i=0; i<favoriteArray.length; i++) {
            if (favoriteArray[i]!=null) favoriteList.add(favoriteArray[i]);
        }

        //四、组装内容
        param.clear();
        if (resultType==2) {//组装喜欢的分类列表
            List<Map<String, Object>> retMediaTypeList=new ArrayList<Map<String, Object>>();

            Map<String, Object> oneMediaType=null;
            List<Map<String, Object>> oneMediaTypeList=null;
            int i=0;
            for (Map<String, Object> oneCntt: favoriteList) {
                if (oneMediaType==null||!(oneCntt.get("MediaType")+"").equals(oneMediaType.get("MediaType")+"")) {
                    if (oneMediaType!=null&&oneMediaTypeList!=null) {
                        oneMediaType.put("AllCount",i);
                        oneMediaType.put("List", oneMediaTypeList);
                        retMediaTypeList.add(oneMediaType);
                    }
                    oneMediaType=new HashMap<String, Object>();
                    oneMediaType.put("MediaType", oneCntt.get("MediaType"));
                    i=0;
                    oneMediaTypeList=new ArrayList<Map<String, Object>>();
                }
                oneMediaTypeList.add(oneCntt);
                i++;
            }
            if (oneMediaType!=null&&oneMediaTypeList!=null) {//处理最后一个
                oneMediaType.put("AllCount",i);
                oneMediaType.put("List", oneMediaTypeList);
                retMediaTypeList.add(oneMediaType);
            }

            param.put("ResultType", resultType);
            param.put("Page", page);
            param.put("PageSize", pageSize);
            if (favoriteList.size()<pageSize) beginCatalogId="ENDEND";
            param.put("BeginCatalogId", beginCatalogId);
            param.put("FavoriteList", retMediaTypeList);
        } else {//组装喜欢的平铺列表
            param.put("ResultType", resultType);
            param.put("AllCount", resultPage.getDataCount());
            param.put("Page", resultPage.getPageIndex());
            param.put("PageSize", resultPage.getPageSize());
            param.put("FavoriteList", favoriteList);
        }

        //五、返回数据
        return param;
    }
    private void add2FavoretList(Map<String, Object>[] favoriteArray, Map<String, Object> oneContent, List<Map<String, Object>> fList) {
        for (int i=0; i<fList.size(); i++) {
            if (equalContent(oneContent, fList.get(i))) {
                favoriteArray[i]=oneContent;
                break;
            }
        }
        
    }
    private void add2FavoretList4Extract(Map<String, Object>[] favoriteArray, Map<String, Object> oneContent, List<Map<String, Object>> fList) {
        for (int i=0; i<fList.size(); i++) {
            @SuppressWarnings("unchecked")
            Map<String, Object> srcConn=(Map<String, Object>)oneContent.get("SeqInfo");
            if (equalContent(srcConn, fList.get(i))) {
                favoriteArray[i]=oneContent;
                break;
            }
        }
        
    }
    private boolean equalContent(Map<String, Object> oneContent, Map<String, Object> oneFavorite) {
        if (!(oneContent.get("ContentId")+"").equals(oneFavorite.get("resId")+"")) return false;
        if (!(ContentUtils.getResTableName(oneContent.get("MediaType")+"")).equals(oneFavorite.get("resTableName")+"")) return false;
        return true;
    }
    private boolean fillTypeList(String cnttType, List<Map<String, Object>> fList, int perSize, int pageSize, Map<String, Object> param) {
        param.put("resTableName", ContentUtils.getResTableName(cnttType));
        List<UserFavoritePo> thisTypeList=userFavoriteDao.queryForList(param);
        for (int i=perSize-1; i<thisTypeList.size()-1; i++) thisTypeList.remove(i);
        for (int i=0; i<thisTypeList.size(); i++) {
            fList.add(thisTypeList.get(i).toHashMapAsBean());
        }
        if (fList.size()>pageSize) return false;
        return true;
    }

    /**
     * 得到某人或设备的喜欢内容按列表，只获得喜欢的Po，为内容列表填充做准备
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return 某人的喜欢内容
     */
    public List<UserFavoritePo> getPureFavoriteList(MobileUDKey mUdk) {
        if (mUdk==null) return null;

        Map<String, Object> param=new HashMap<String, Object>();
        param.put("mobileId", mUdk.getDeviceId());
        if (mUdk.isUser()) param.put("userId", mUdk.getUserId());
        return userFavoriteDao.queryForList("getFavoriteAssets", param);
    }

    /**
     * 得到某人或设备的喜欢内容按内容类型的分布情况
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return 若无数据，返回空Map，否则返回各内容类型的个数，若某内荣分类无数据，则不返回该项的数据
     */
    public Map<String, Object> getFavoriteMTypeDistri(MobileUDKey mUdk) {
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("mobileId", mUdk.getDeviceId());
        if (mUdk.isUser()) param.put("userId", mUdk.getUserId());
        List<Map<String, Object>> distriuteData=userFavoriteDao.queryForListAutoTranform("getDistriuteData", param);
        param.clear();
        if (distriuteData!=null&&distriuteData.size()>0) {
            for (Map<String, Object> m: distriuteData) {
                String assetType=m.get("resTableName")+"";
                int mediaCount=Integer.parseInt(m.get("typeCount")+"");
                param.put(ContentUtils.getMediaType(assetType), mediaCount);
            }
        }
        return param;
    }

    /**
     * 批量删除所新欢的内容
     * @param delInfos 喜欢内容的列表，用字符串方式处理——{类型::Id,类型::Id,类型::Id,类型::Id},如"AUDIO::23ddea234,AUDIO::3ea34f5,SEQU::a32b3f3"
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return 对每一个删除的回复，是一个Map的List，每个Map内容为：类型-CType，Id-CId，处理结果-Result，其中Result=1删除成功;0没有对应记录无法删除;-1媒体类型不合法
     */
    public List<Map<String, Object>> delFavorites(String delInfos, MobileUDKey mUdk) {
        String[] oneC=delInfos.split(",");
        List<Map<String, Object>> ret=new ArrayList<Map<String, Object>>();
        for (int i=0; i<oneC.length; i++) {
            String[] cInfo=oneC[i].split("::");
            Map<String, Object> oneResult=new HashMap<String, Object>();
            if (cInfo.length!=2) {
                oneResult.put("ErrorKey", oneC[i]);
            } else {
                oneResult.put("ContentType", cInfo[0]);
                oneResult.put("ContentId", cInfo[1]);
                String CType=cInfo[0].toUpperCase();
                if (!CType.equals("RADIO")&&!CType.equals("AUDIO")&&!CType.equals("SEQU")&&!CType.equals("TEXT")) oneResult.put("Result", "-1");
                else {//删除
                    Map<String, Object> paraDel=new HashMap<String, Object>();
                    paraDel.put("resTableName", ContentUtils.getResTableName(CType));
                    paraDel.put("resId", cInfo[1]);
                    paraDel.put("ownerType", "202");
                    paraDel.put("ownerId", mUdk.getDeviceId());
                    userFavoriteDao.delete("deleteByEntity",paraDel);
                    if (mUdk.isUser()) {
                        paraDel.put("ownerType", "201");
                        paraDel.put("ownerId", mUdk.getUserId());
                        userFavoriteDao.delete("deleteByEntity",paraDel);
                    }
                    oneResult.put("Result", "1");
                }
            }
            ret.add(oneResult);
        }
        return ret;
    }
}