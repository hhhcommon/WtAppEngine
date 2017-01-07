package com.woting.discuss.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.core.model.Page;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.WtAppEngineConstants;
import com.woting.cm.core.broadcast.service.BroadcastService;
import com.woting.cm.core.channel.mem._CacheChannel;
import com.woting.cm.core.channel.model.Channel;
import com.woting.cm.core.channel.persis.po.ChannelAssetPo;
import com.woting.cm.core.dict.persis.po.DictRefResPo;
import com.woting.cm.core.media.MediaType;
import com.woting.cm.core.media.service.MediaService;
import com.woting.cm.core.utils.ContentUtils;
import com.woting.discuss.model.Discuss;
import com.woting.discuss.persis.po.DiscussPo;
import com.woting.favorite.persis.po.UserFavoritePo;
import com.woting.passport.UGA.persis.pojo.GroupPo;

public class DiscussService {
    @Resource(name="defaultDAO")
    private MybatisDAO<DiscussPo> discussDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<ChannelAssetPo> channelAssetDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<UserFavoritePo> favoriteDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<DictRefResPo> dictRefDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<GroupPo> groupDao;
    @Resource
    private MediaService mediaService;
    @Resource
    private BroadcastService bcService;

    @PostConstruct
    public void initParam() {
        discussDao.setNamespace("WT_DISCUSS");
        channelAssetDao.setNamespace("A_CHANNELASSET");
        favoriteDao.setNamespace("DA_USERFAVORITE");
        dictRefDao.setNamespace("A_DREFRES");
        groupDao.setNamespace("WT_GROUP");
    }

    /**
     * 得到重复意见
     * @param opinion 意见信息
     * @return 创建用户成功返回1，否则返回0
     */
    public List<Discuss> getDuplicates(Discuss discuss) {
        try {
            List<Discuss> ret=new ArrayList<Discuss>();
            List<DiscussPo> _ret=discussDao.queryForList("getDuplicates", discuss.toHashMapAsBean());
            if (_ret!=null&&!_ret.isEmpty()) {
                for (DiscussPo dpo: _ret) {
                    Discuss ele=new Discuss();
                    ele.buildFromPo(dpo);
                    ret.add(ele);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 保存用户所提意见
     * @param opinion 意信息
     * @return 创建用户成功返回1，否则返回0
     */
    public int insertDiscuss(Discuss discuss) {
        int i=0;
        try {
            discuss.setId(SequenceUUID.getUUIDSubSegment(4));
            discussDao.insert(discuss.convert2Po());
            i=1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * 删除评论
     * @param discuss 要删除的评论信息
     * @return 0-删除失败;1删除成功;-1无对应的评论无法删除;-2无权删除
     */
    public int delDiscuss(Discuss discuss) {
        try {
            Map<String, Object> param=((DiscussPo)discuss.convert2Po()).toHashMapAsBean();
            DiscussPo dpo=discussDao.getInfoObject(param);
            if (dpo==null) return -1;
            if (discuss.getUserId().equals(dpo.getUserId())) {
                discussDao.delete(param);
                return 1;
            } else return -2;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 根据文章Id获得文章的评论列表
     * @param mt 内容分类
     * @param contentId 内容Id
     * @param isPub 是否发布
     * @param page 页数
     * @param pageSize 每页条数
     * @return 文章评论列表
     */
    public Map<String, Object> getArticleDiscusses(MediaType mt, String contentId, int isPub, int page, int pageSize) {
        try {
            Map<String, Object> param=new HashMap<String, Object>();
            List<DiscussPo> ol=null;
            long allCount=0;
            if (isPub==1) {
                param.put("resTableName", mt.getTabName());
                param.put("resId", contentId);
                param.put("sortByClause", " a.cTime desc");
                if (page>=0) { //分页
                    if (page==0) page=1;
                    if (pageSize<0) pageSize=10;
                    Page<DiscussPo> p=this.discussDao.pageQuery("getPubList", param, page, pageSize);
                    if (!p.getResult().isEmpty()) {
                        ol=new ArrayList<DiscussPo>();
                        ol.addAll(p.getResult());
                    }
                    allCount=p.getDataCount();
                } else { //获得所有
                    ol=this.discussDao.queryForList("getPubList", param);
                }
            } else {
                param.put("resTableName", mt.getTabName());
                param.put("resId", contentId);
                param.put("sortByClause", " cTime desc");
                if (page>=0) { //分页
                    if (page==0) page=1;
                    if (pageSize<0) pageSize=10;
                    Page<DiscussPo> p=this.discussDao.pageQuery(param, page, pageSize);
                    if (!p.getResult().isEmpty()) {
                        ol=new ArrayList<DiscussPo>();
                        ol.addAll(p.getResult());
                    }
                    allCount=p.getDataCount();
                } else { //获得所有
                    ol=this.discussDao.queryForList(param);
                }
            }
            if (ol!=null&&ol.size()>0) {
                List<Discuss> ret=new ArrayList<Discuss>();
                if (ol!=null&&!ol.isEmpty()) {
                    for (DiscussPo dpo: ol) {
                        Discuss ele=new Discuss();
                        ele.buildFromPo(dpo);
                        ret.add(ele);
                    }
                }
                param.clear();
                param.put("AllCount", allCount);
                param.put("List", ret);
                return param;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据用户Id,获得用户评论过的文章列表
     * @param userId 用户Id
     * @param ml 内容分类列表
     * @param isPub 是否发布
     * @param page 页数
     * @param pageSize 每页条数
     * @param rType 返回类型1=列表;2分类
     * @return 文章列表
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserDiscusses(String userId, List<MediaType> ml, int isPub, int page, int pageSize, int rType) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return null;

        if (page==0) page=1;
        if (pageSize<0) pageSize=10;
        try {
            Map<String, Object> param=new HashMap<String, Object>();
            List<DiscussPo> dl=null;
            long allCount=0;
            if (rType==1||(ml==null||ml.isEmpty())) { //按一个列表进行返回
                param.put("userId", userId);
                if (isPub==1) { //发布的内容
                    param.put("sortByClause", " a.cTime desc");
                    if (page>=0) { //分页
                        if (page==0) page=1;
                        if (pageSize<0) pageSize=10;
                        Page<DiscussPo> p=discussDao.pageQuery("getPubList", param, page, pageSize);
                        if (!p.getResult().isEmpty()) {
                            dl=new ArrayList<DiscussPo>();
                            dl.addAll(p.getResult());
                        }
                        allCount=p.getDataCount();
                    } else { //获得所有
                        dl=discussDao.queryForList("getPubList", param);
                        allCount=dl.size();
                    }
                } else {
                    param.put("sortByClause", " cTime desc");
                    if (page>=0) { //分页
                        if (page==0) page=1;
                        if (pageSize<0) pageSize=10;
                        Page<DiscussPo> p=discussDao.pageQuery(param, page, pageSize);
                        if (!p.getResult().isEmpty()) {
                            dl=new ArrayList<DiscussPo>();
                            dl.addAll(p.getResult());
                        }
                        allCount=p.getDataCount();
                    } else { //获得所有
                        dl=discussDao.queryForList(param);
                        allCount=dl.size();
                    }
                }
                if (dl==null||dl.isEmpty()) return null;

                //处理内容
                String s="", f="";
                String maIds="", seqMaIds="", bcIds="";
                for (DiscussPo dPo: dl) {
                    s+=" or (assetType='"+dPo.getResTableName()+"' and assetId='"+dPo.getResId()+"')";
                    f+=" or (resTableName='"+dPo.getResTableName()+"' and resId='"+dPo.getResId()+"')";
                    switch (MediaType.buildByTabName(dPo.getResTableName())) {
                        case RADIO: bcIds+=" or a.id='"+dPo.getResId()+"'";break;
                        case AUDIO: maIds+=" or a.id='"+dPo.getResId()+"'";break;
                        case SEQU: seqMaIds+=" or a.id='"+dPo.getResId()+"'";break;
                        default: ;
                    }
                }
                //相关栏目信息
                param.clear();
                param.put("whereByClause", s.substring(4));
                List<ChannelAssetPo> chas=channelAssetDao.queryForList("getListByWhere", param);
                List<Map<String, Object>> chaml=new ArrayList<Map<String, Object>>();
                if (chas!=null&&!chas.isEmpty()) {
                    _CacheChannel _cc=(SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)==null?null:((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)).getContent());
                    for (ChannelAssetPo caPo: chas) {
                        Map<String, Object> one=caPo.toHashMap();
                        if (_cc!=null) {
                            TreeNode<Channel> _c=(TreeNode<Channel>)_cc.channelTree.findNode(caPo.getChannelId());
                            if (_c!=null) one.put("channelName", _c.getNodeName());
                        }
                        chaml.add(one);
                    }
                }
                if (chaml.isEmpty()) chaml=null;
                //分类型获得列表
                param.clear();
                param.put("whereByClause", f.substring(4));
                List<DictRefResPo> drrs=dictRefDao.queryForList("getListByWhere", param);
                List<Map<String, Object>> cataml=new ArrayList<Map<String, Object>>();
                if (drrs!=null&&!drrs.isEmpty()) {
                    for (DictRefResPo drrPo: drrs) {
                        cataml.add(drrPo.toHashMap());
                    }
                }
                if (cataml.isEmpty()) cataml=null;
                //获得喜欢列表
                List<UserFavoritePo> fs=favoriteDao.queryForList("getListByWhere", param);
                List<Map<String, Object>> fml=new ArrayList<Map<String, Object>>();
                if (fs!=null&&!fs.isEmpty()) {
                    for (UserFavoritePo ufPo: fs) {
                        fml.add(ufPo.toHashMap());
                    }
                }
                if (fml.isEmpty()) fml=null;
                //获得播放次数
                param.clear();
                if (StringUtils.isNullOrEmptyOrSpace(bcIds)) {
                    bcIds=bcIds.substring(4);
                    param.put("bcIds", "a.resTableName='wt_Broadcast' or ("+bcIds+")");
                }
                if (StringUtils.isNullOrEmptyOrSpace(maIds)) {
                    maIds=maIds.substring(4);
                    param.put("maIds", "a.resTableName='wt_MediaAsset' or ("+maIds+")");
                }
                if (StringUtils.isNullOrEmptyOrSpace(seqMaIds)) {
                    seqMaIds=seqMaIds.substring(4);
                    param.put("seqMaIds", "a.resTableName='wt_SeqMediaAsset' or ("+seqMaIds+")");
                }
                List<Map<String, Object>> pcml=groupDao.queryForListAutoTranform("refPlayCountById", param);

                //组织返回值
                int i=0;
                Map<String, Object>[] rl=new Map[dl.size()];
                //单体
                if (!StringUtils.isNullOrEmpty(maIds)) {
                    List<Map<String, Object>> mas=mediaService.getMaListByWhereStr(maIds);
                    for (Map<String, Object> ma : mas) {
                        Map<String, Object> mam=ContentUtils.convert2Ma(ma, null, cataml, chaml, fml, pcml);
                        for (i=0; i<dl.size(); i++) {
                            DiscussPo dPo=dl.get(i);
                            if (dPo.getResTableName().equals(MediaType.AUDIO.getTabName())&&dPo.getResId().equals(ma.get("id"))) {
                                rl[i]=mam;
                            }
                        }
                    }
                }
                //专辑
                if (!StringUtils.isNullOrEmpty(seqMaIds)) {
                    List<Map<String, Object>> smas=mediaService.getSeqMaListByWhereStr(seqMaIds);
                    for (Map<String, Object> sma : smas) {
                        Map<String, Object> seqMam=ContentUtils.convert2Sma(sma, null, cataml, chaml, fml, pcml);
                        for (i=0; i<dl.size(); i++) {
                            DiscussPo dPo=dl.get(i);
                            if (dPo.getResTableName().equals(MediaType.SEQU.getTabName())&&dPo.getResId().equals(sma.get("id"))) {
                                rl[i]=seqMam;
                            }
                        }
                    }
                }
                //电台
                if (!StringUtils.isNullOrEmpty(bcIds)) {
                    List<Map<String, Object>> bcs=bcService.getListByWhereStr(bcIds);

                    //获得当前的播放列表
                    Calendar cal=Calendar.getInstance();
                    Date date=new Date();
                    cal.setTime(date);
                    int week=cal.get(Calendar.DAY_OF_WEEK);
                    DateFormat sdf=new SimpleDateFormat("HH:mm:ss");
                    String timestr=sdf.format(date);
                    param.put("bcIds", bcIds);
                    param.put("weekDay", week);
                    param.put("sort", 0);
                    param.put("timestr", timestr);
                    List<Map<String, Object>> playingList=groupDao.queryForListAutoTranform("playingBc", param);

                    for (Map<String, Object> bc : bcs) {
                        Map<String, Object> bcm=ContentUtils.convert2Bc(bc, null, cataml, chaml, fml, pcml, playingList);
                        for (i=0; i<dl.size(); i++) {
                            DiscussPo dPo=dl.get(i);
                            if (dPo.getResTableName().equals(MediaType.RADIO.getTabName())&&dPo.getResId().equals(bc.get("id"))) {
                                rl[i]=bcm;
                            }
                        }
                    }
                }
                param.clear();
                param.put("AllCount", allCount);
                param.put("ResultType", 1);
                param.put("ContentList", rl);
                return param;
            } else if (rType==2&&ml!=null&&!ml.isEmpty()) { //按分类列表进行返回
                //得到总数
                param.put("userId", userId);
                if (isPub==1) { //发布的内容
                    dl=this.discussDao.queryForList("getPubList", param);
                } else {
                    dl=this.discussDao.queryForList(param);
                }
                if (dl==null) return null;
                allCount=dl.size();

                List<Map<String, Object>> rl=new ArrayList<>();
                for (MediaType mt: ml) {
                    int typeCount=0;
                    param.clear();
                    param.put("resTableName", mt.getTabName());
                    param.put("userId", userId);
                    if (isPub==1) { //发布的内容
                        param.put("sortByClause", " a.cTime desc");
                        if (page>=0) { //分页
                            if (page==0) page=1;
                            if (pageSize<0) pageSize=10;
                            Page<DiscussPo> p=this.discussDao.pageQuery("getPubList", param, page, pageSize);
                            if (!p.getResult().isEmpty()) {
                                dl=new ArrayList<DiscussPo>();
                                dl.addAll(p.getResult());
                            }
                            typeCount=p.getDataCount();
                        } else { //获得所有
                            dl=this.discussDao.queryForList("getPubList", param);
                            typeCount=dl.size();
                        }
                    } else {
                        param.put("sortByClause", " cTime desc");
                        if (page>=0) { //分页
                            if (page==0) page=1;
                            if (pageSize<0) pageSize=10;
                            Page<DiscussPo> p=this.discussDao.pageQuery(param, page, pageSize);
                            if (!p.getResult().isEmpty()) {
                                dl=new ArrayList<DiscussPo>();
                                dl.addAll(p.getResult());
                            }
                            typeCount=p.getDataCount();
                        } else { //获得所有
                            dl=this.discussDao.queryForList(param);
                            typeCount=dl.size();
                        }
                    }
                    if (typeCount>0) {
                        //处理参数
                        String s="", f="";
                        String maIds="", seqMaIds="", bcIds="";
                        for (DiscussPo dPo: dl) {
                            s+=" or (assetType='"+dPo.getResTableName()+"' and assetId='"+dPo.getResId()+"')";
                            f+=" or (resTableName='"+dPo.getResTableName()+"' and resId='"+dPo.getResId()+"')";
                            switch (MediaType.buildByTabName(dPo.getResTableName())) {
                                case RADIO: bcIds+=" or a.id='"+dPo.getResId()+"'";break;
                                case AUDIO: maIds+=" or a.id='"+dPo.getResId()+"'";break;
                                case SEQU: seqMaIds+=" or a.id='"+dPo.getResId()+"'";break;
                                default: ;
                            }
                        }
                        //相关栏目信息
                        param.clear();
                        param.put("whereByClause", s.substring(4));
                        List<ChannelAssetPo> chas=channelAssetDao.queryForList("getListByWhere", param);
                        List<Map<String, Object>> chaml=new ArrayList<Map<String, Object>>();
                        if (chas!=null&&!chas.isEmpty()) {
                            _CacheChannel _cc=(SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)==null?null:((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)).getContent());
                            for (ChannelAssetPo caPo: chas) {
                                Map<String, Object> one=caPo.toHashMap();
                                if (_cc!=null) {
                                    TreeNode<Channel> _c=(TreeNode<Channel>)_cc.channelTree.findNode(caPo.getChannelId());
                                    if (_c!=null) one.put("channelName", _c.getNodeName());
                                }
                                chaml.add(one);
                            }
                        }
                        if (chaml.isEmpty()) chaml=null;
                        //分类型获得列表
                        param.clear();
                        param.put("whereByClause", f.substring(4));
                        List<DictRefResPo> drrs=dictRefDao.queryForList("getListByWhere", param);
                        List<Map<String, Object>> cataml=new ArrayList<Map<String, Object>>();
                        if (drrs!=null&&!drrs.isEmpty()) {
                            for (DictRefResPo drrPo: drrs) {
                                cataml.add(drrPo.toHashMap());
                            }
                        }
                        if (cataml.isEmpty()) cataml=null;
                        //获得喜欢列表
                        List<UserFavoritePo> fs=favoriteDao.queryForList("getListByWhere", param);
                        List<Map<String, Object>> fml=new ArrayList<Map<String, Object>>();
                        if (fs!=null&&!fs.isEmpty()) {
                            for (UserFavoritePo ufPo: fs) {
                                fml.add(ufPo.toHashMap());
                            }
                        }
                        if (fml.isEmpty()) fml=null;
                        //获得播放次数
                        param.clear();
                        if (StringUtils.isNullOrEmptyOrSpace(bcIds)) {
                            bcIds=bcIds.substring(4);
                            param.put("bcIds", "a.resTableName='wt_Broadcast' or ("+bcIds+")");
                        }
                        if (StringUtils.isNullOrEmptyOrSpace(maIds)) {
                            maIds=maIds.substring(4);
                            param.put("maIds", "a.resTableName='wt_MediaAsset' or ("+maIds+")");
                        }
                        if (StringUtils.isNullOrEmptyOrSpace(seqMaIds)) {
                            seqMaIds=seqMaIds.substring(4);
                            param.put("seqMaIds", "a.resTableName='wt_SeqMediaAsset' or ("+seqMaIds+")");
                        }
                        List<Map<String, Object>> pcml=groupDao.queryForListAutoTranform("refPlayCountById", param);

                        //组织返回值
                        int i=0;
                        Map<String, Object>[] typel=new Map[dl.size()];
                        //单体
                        if (!StringUtils.isNullOrEmpty(maIds)) {
                            List<Map<String, Object>> mas=mediaService.getMaListByWhereStr(maIds);
                            for (Map<String, Object> ma : mas) {
                                Map<String, Object> mam=ContentUtils.convert2Ma(ma, null, cataml, chaml, fml, pcml);
                                for (i=0; i<dl.size(); i++) {
                                    DiscussPo dPo=dl.get(i);
                                    if (dPo.getResTableName().equals(MediaType.AUDIO.getTabName())&&dPo.getResId().equals(ma.get("id"))) {
                                        typel[i]=mam;
                                    }
                                }
                            }
                        }
                        //专辑
                        if (!StringUtils.isNullOrEmpty(seqMaIds)) {
                            List<Map<String, Object>> smas=mediaService.getSeqMaListByWhereStr(seqMaIds);
                            for (Map<String, Object> sma : smas) {
                                Map<String, Object> seqMam=ContentUtils.convert2Sma(sma, null, cataml, chaml, fml, pcml);
                                for (i=0; i<dl.size(); i++) {
                                    DiscussPo dPo=dl.get(i);
                                    if (dPo.getResTableName().equals(MediaType.SEQU.getTabName())&&dPo.getResId().equals(sma.get("id"))) {
                                        typel[i]=seqMam;
                                    }
                                }
                            }
                        }
                        //电台
                        if (!StringUtils.isNullOrEmpty(bcIds)) {
                            List<Map<String, Object>> bcs=bcService.getListByWhereStr(bcIds);

                            //获得当前的播放列表
                            Calendar cal=Calendar.getInstance();
                            Date date=new Date();
                            cal.setTime(date);
                            int week=cal.get(Calendar.DAY_OF_WEEK);
                            DateFormat sdf=new SimpleDateFormat("HH:mm:ss");
                            String timestr=sdf.format(date);
                            param.put("bcIds", bcIds);
                            param.put("weekDay", week);
                            param.put("sort", 0);
                            param.put("timestr", timestr);
                            List<Map<String, Object>> playingList=groupDao.queryForListAutoTranform("playingBc", param);

                            for (Map<String, Object> bc : bcs) {
                                Map<String, Object> bcm=ContentUtils.convert2Bc(bc, null, cataml, chaml, fml, pcml, playingList);
                                for (i=0; i<dl.size(); i++) {
                                    DiscussPo dPo=dl.get(i);
                                    if (dPo.getResTableName().equals(MediaType.RADIO.getTabName())&&dPo.getResId().equals(bc.get("id"))) {
                                        typel[i]=bcm;
                                    }
                                }
                            }
                        }
                        Map<String, Object> oneMedie=new HashMap<String, Object>();
                        oneMedie.put("AllCount", typeCount);
                        oneMedie.put("MediaType", mt.getTypeName());
                        oneMedie.put("ContentList", typel);
                        rl.add(oneMedie);
                    } else {
                        Map<String, Object> oneMedie=new HashMap<String, Object>();
                        oneMedie.put("AllCount", typeCount);
                        oneMedie.put("MediaType", mt.getTypeName());
                        rl.add(oneMedie);
                    }
                }
                if (rl==null||rl.isEmpty()) return null;
                param.clear();
                param.put("AllCount", allCount);
                param.put("ResultType", 2);
                param.put("ContentList", rl);
                return param;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}