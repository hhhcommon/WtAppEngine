package com.woting.favorite.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.core.model.Page;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.content.ContentUtils;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.cm.core.channel.service.ChannelService;
import com.woting.favorite.persis.po.UserFavoritePo;
import com.woting.passport.UGA.persistence.pojo.GroupPo;

@Lazy(true)
@Service
public class FavoriteService {
    @Resource
    private ChannelService channelService;
    @Resource(name="defaultDAO")
    private MybatisDAO<UserFavoritePo> userFavoriteDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<GroupPo> groupDao;

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
    public int favorite(String mediaType, String contentId, int flag, MobileKey mk) {
        String CType=mediaType.toUpperCase();
        if (!CType.equals("RADIO")&&!CType.equals("AUDIO")&&!CType.equals("SEQU")&&!CType.equals("TEXT")) return -100;

        String assetType=ContentUtils.getResTableName(mediaType);
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("resTableName", assetType);
        param.put("resId", contentId);

        if (flag==1) {
            if (!channelService.isPub(assetType, contentId)) return 0;
            if (mk.isUser()) {
                param.put("ownerType", "201");
                param.put("ownerId", mk.getUserId());
            } else {
                param.put("ownerType", "202");
                param.put("ownerId", mk.getMobileId());
            }
            if (userFavoriteDao.getCount(param)>0) return 2;
            param.put("id", SequenceUUID.getUUIDSubSegment(4));
            userFavoriteDao.insert(param);//加入喜欢队列
        } else {
            param.put("mobileId", mk.getMobileId());
            if (mk.isUser()) param.put("userId", mk.getUserId());
            if (userFavoriteDao.getCount("getCount4Favorite", param)==0) return -1;
            //设备删除
            param.put("ownerType", "202");
            param.put("ownerId", mk.getMobileId());
            userFavoriteDao.delete("deleteByEntity",param);
            //用户删除
            if (mk.isUser()) {
                param.put("ownerType", "201");
                param.put("ownerId", mk.getUserId());
                userFavoriteDao.delete("deleteByEntity",param);
            }
        }
        return 1;
    }

    /**
     * 得到某人或设备的喜欢内容按列表
     * @param mediaType 过滤条件，若没有，则全部获取，注意，这里的mediaType可以是一个，也可以是多个,用逗号隔开
     * @param pageSize 没页个数
     * @param page 页数
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return 该页内容
     */
    public Map<String, Object> getFavoriteList(String mediaType, int pageSize, int page, MobileKey mk) {
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("mobileId", mk.getMobileId());
        if (mk.isUser()) param.put("userId", mk.getUserId());
        //处理过滤条件
        String typeSql="";
        String[] types=mediaType.split(",");
        for (int i=0; i<types.length; i++) {
            if (types[i].equals("RADIO")||types[i].equals("AUDIO")||!types[i].equals("SEQU")||!types[i].equals("TEXT")) {
                typeSql+="or resTableName='"+ContentUtils.getResTableName(types[i])+"'";
            }
        }
        if (typeSql.length()>0) typeSql=typeSql.substring(3);
        param.put("mediaFilterSql", typeSql);

        Page<UserFavoritePo> resultPage=userFavoriteDao.pageQueryAutoTranform(null, "getFavoriteAssets", param, page, pageSize);
        if (resultPage==null||resultPage.getDataCount()==0||resultPage.getResult()==null||resultPage.getResult().isEmpty()) return null;
        param.clear();
        param.put("AllSize", resultPage.getDataCount());
        param.put("CurPage", resultPage.getPageIndex());
        param.put("PageSize", resultPage.getPageSize());

        List<UserFavoritePo> fList=(List<UserFavoritePo>)resultPage.getResult();
        
        String bcIds="", maIds="", smaIds="";
        for (UserFavoritePo oneUf: fList) {
            if (oneUf.getResTableName()!=null&&oneUf.getResId()!=null) {
                if (oneUf.getResTableName().equals("wt_Broadcast")) bcIds+=",'"+oneUf.getResId()+"'";
                else
                if (oneUf.getResTableName().equals("wt_MediaAsset")) maIds+=",'"+oneUf.getResId()+"'";
                else
                if (oneUf.getResTableName().equals("wt_SeqMediaAsset")) smaIds+=",'"+oneUf.getResId()+"'";
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
        List<Map<String, Object>> pubChannelList=channelService.getPubChannelList(fList);

        //组装内容
        List<Map<String, Object>> favoriteList=new ArrayList<Map<String, Object>>(fList.size());
        Map<String, Object> oneContent;

        List<Map<String, Object>> tempList;
        if (bcIds.length()>0) {
            reParam.clear();
            reParam.put("inIds", bcIds);
            tempList=groupDao.queryForListAutoTranform("getBcList", reParam);
            if (tempList!=null&&!tempList.isEmpty()) {
                for (Map<String, Object> oneCntt: tempList) {
                    oneContent=ContentUtils.convert2Bc(oneCntt, personList, cataList, pubChannelList, fList);
                    add2FavoretList(favoriteList, oneContent, fList);
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
                    add2FavoretList(favoriteList, oneContent, fList);
                }
            }
        }
        if (smaIds.length()>0) {
            reParam.clear();
            reParam.put("inIds", smaIds);
            tempList=groupDao.queryForListAutoTranform("getSeqMaList", reParam);
            if (tempList!=null&&!tempList.isEmpty()) {
                for (Map<String, Object> oneCntt: tempList) {
                    oneContent=ContentUtils.convert2Sma(oneCntt, personList, cataList, pubChannelList, fList);
                    add2FavoretList(favoriteList, oneContent, fList);
                }
            }
        }
        for (int i=favoriteList.size()-1; i>=0; i--) {
            if (favoriteList.get(i)==null) favoriteList.remove(i);
        }
        param.put("FavoriteList", favoriteList);
        
        return param;
    }
    private void add2FavoretList(List<Map<String, Object>> favoriteList, Map<String, Object> oneContent, List<UserFavoritePo> fList) {
        for (int i=0; i<fList.size(); i++) {
            if (equalContent(oneContent, fList.get(i))) {
                favoriteList.add(i, oneContent);
                break;
            }
        }
        
    }
    private boolean equalContent(Map<String, Object> oneContent, UserFavoritePo oneFavorite) {
        if (!(oneContent.get("ContentId")+"").equals(oneFavorite.getResId())) return false;
        if (!(ContentUtils.getResTableName(oneContent.get("MediaType")+"")).equals(oneFavorite.getResTableName())) return false;
        return true;
    }

    /**
     * 得到某人或设备的喜欢内容按列表，只获得喜欢的Po，为内容列表填充做准备
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return 某人的喜欢内容
     */
    public List<UserFavoritePo> getPureFavoriteList(MobileKey mk) {
        if (mk==null) return null;

        Map<String, Object> param=new HashMap<String, Object>();
        param.put("mobileId", mk.getMobileId());
        if (mk.isUser()) param.put("userId", mk.getUserId());
        return userFavoriteDao.queryForList("getFavoriteAssets", param);
    }

    /**
     * 得到某人或设备的喜欢内容按内容类型的分布情况
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return 若无数据，返回空Map，否则返回各内容类型的个数，若某内荣分类无数据，则不返回该项的数据
     */
    public Map<String, Object> getFavoriteMTypeDistri(MobileKey mk) {
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("mobileId", mk.getMobileId());
        if (mk.isUser()) param.put("userId", mk.getUserId());
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
    public List<Map<String, Object>> delFavorites(String delInfos, MobileKey mk) {
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
                    paraDel.put("ownerId", mk.getMobileId());
                    userFavoriteDao.delete("deleteByEntity",paraDel);
                    if (mk.isUser()) {
                        paraDel.put("ownerType", "201");
                        paraDel.put("ownerId", mk.getUserId());
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