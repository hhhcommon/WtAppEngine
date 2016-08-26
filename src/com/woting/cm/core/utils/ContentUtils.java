package com.woting.cm.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容方法类：内容数据的转换，主要——存储对象转换为显示对象。
 * 注意：这里的方法，对传入的参数不做强制的校验
 * @author wanghui
 */
public abstract class ContentUtils {
    /*
     * 填充扩展信息
     * @param one 被填充的信息
     * @param mediaType 内容类型
     * @param personList 对应的人员信息列表
     * @param cataList 对应的字典分类信息列表
     * @param favoriteList 对应的喜欢信息列表
     * @param pubChannelList 发布栏目信息，已审核通过的栏目信息
     */
    private static void fillExtInfo(Map<String, Object> one, String mediaType,
                                      List<Map<String, Object>> personList,
                                      List<Map<String, Object>> cataList,
                                      List<Map<String, Object>> pubChannelList,
                                      List<Map<String, Object>> favoriteList) {
        //P12-公共：相关人员列表
        Object temp=fetchPersons(personList, getResTableName(mediaType), one.get("ContentId")+"");
        if (temp!=null) one.put("ContentPersons", temp);
        //P13-公共：所有分类列表
        temp=fetchCatas(cataList, getResTableName(mediaType), one.get("ContentId")+"");
        if (temp!=null) one.put("ContentCatalogs", temp);
        //P14-公共：发布情况
        Object cnls=fetchChannels(pubChannelList, getResTableName(mediaType), one.get("ContentId")+"");
        if (cnls!=null) one.put("ContentPubChannels", cnls);
        //P15-公共：是否喜欢
        temp=fetchFavorite(favoriteList, getResTableName(mediaType), one.get("ContentId")+"");
        one.put("ContentFavorite", (temp==null?0:(((Integer)temp)==1?(cnls==null?"您喜欢的内容已经下架":1):0))+"");
        //P16-公共：播放次数
        one.put("PlayCount", "1234");
    }
    /**
     * 把单条内容信息转换为电台信息。参数中的列表是对电台外围信息的补充。<br/>
     * 为充分利用数据库资源，提升处理速度，不对每一条数据进行外围数据的获取，而是对符合条件的列表中的所有外围信息进行查找，并作为参数传递给该方法，由本方法进行组合。<br/>
     * 目前有的外围信息包括：字典分类、相关人员、喜欢、发布栏目；今后可能包括：点击数量、(所属栏目、所属专辑，这两类App端用不上，后台管理可能会用到)
     * @param one 电台信息的Map，注意：这里不对map是否是电台数据进行校验
     * @param personList 对应的人员信息列表
     * @param cataList 对应的字典分类信息列表
     * @param pubChannelList 发布栏目信息，已审核通过的栏目信息
     * @param favoriteList 对应的喜欢信息列表
     * @return
     */
    public static Map<String, Object> convert2Bc(Map<String, Object> one,
                                                  List<Map<String, Object>> personList,
                                                  List<Map<String, Object>> cataList,
                                                  List<Map<String, Object>> pubChannelList,
                                                  List<Map<String, Object>> favoriteList) {
        Map<String, Object> retM=new HashMap<String, Object>();

        retM.put("MediaType", "RADIO");

        retM.put("ContentId", one.get("id"));//P01-公共：ID
        retM.put("ContentName", one.get("bcTitle"));//P02-公共：名称
        retM.put("ContentPub", one.get("bcPublisher"));//P03-公共：发布者，集团名称
        retM.put("ContentImg", one.get("bcImg"));//P07-公共：相关图片
        retM.put("ContentPlay", one.get("flowURI"));//P08-公共：主播放Url
        retM.put("ContentShareURL", getShareUrl_DT(preAddr, one.get("id")+""));//分享地址
        retM.put("ContentSource", one.get("bcSource"));//P09-公共：来源名称
        retM.put("ContentURIS", null);//P10-公共：其他播放地址列表，目前为空
        retM.put("ContentDesc", one.get("descn"));//P11-公共：说明

        fillExtInfo(retM, "RADIO", personList, cataList, pubChannelList, favoriteList);//填充扩展信息

        retM.put("ContentFreq", one.get(""));//S01-特有：主频率，目前为空
        retM.put("ContentFreqs", one.get(""));//S02-特有：频率列表，目前为空
        retM.put("ContentList", one.get(""));//S03-特有：节目单列表，目前为空

        retM.put("CTime", one.get("CTime"));//A1-管控：节目创建时间，目前以此进行排序

        return retM;
    }

    /**
     * 把单条内容信息转换为单体节目信息。参数中的列表是对单体节目外围信息的补充。<br/>
     * 为充分利用数据库资源，提升处理速度，不对每一条数据进行外围数据的获取，而是对符合条件的列表中的所有外围信息进行查找，并作为参数传递给该方法，由本方法进行组合。<br/>
     * 目前有的外围信息包括：字典分类、相关人员、喜欢；今后可能包括：点击数量、(所属栏目、所属专辑，这两类App端用不上，后台管理可能会用到)
     * @param one 单条内容的Map，注意：这里不对map是否是单条内容数据进行校验
     * @param personList 对应的人员信息列表
     * @param cataList 对应的字典分类信息列表
     * @param pubChannelList 发布栏目信息，已审核通过的栏目信息
     * @param favoriteList 对应的喜欢信息列表
     * @return
     */
    public static Map<String, Object> convert2Ma(Map<String, Object> one,
                                                  List<Map<String, Object>> personList,
                                                  List<Map<String, Object>> cataList,
                                                  List<Map<String, Object>> pubChannelList,
                                                  List<Map<String, Object>> favoriteList) {
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
        retM.put("ContentShareURL", getShareUrl_JM(preAddr, one.get("id")+""));//分享地址
//        retM.put("ContentSource", one.get("maSource"));//P09-公共：来源名称
//        retM.put("ContentURIS", null);//P10-公共：其他播放地址列表，目前为空
        retM.put("ContentDesc", one.get("descn"));//P11-公共：说明
        retM.put("ContentStatus", one.get("maStatus"));

        fillExtInfo(retM, "AUDIO", personList, cataList, pubChannelList, favoriteList);//填充扩展信息


        retM.put("ContentTimes", one.get("timeLong"));//S01-特有：播放时长

        retM.put("CTime", one.get("CTime"));//A1-管控：节目创建时间，目前以此进行排序

        return retM;
    }

    /**
     * 把单条内容信息转换为专辑节目信息。参数中的列表是对专辑节目外围信息的补充。<br/>
     * 为充分利用数据库资源，提升处理速度，不对每一条数据进行外围数据的获取，而是对符合条件的列表中的所有外围信息进行查找，并作为参数传递给该方法，由本方法进行组合。<br/>
     * 目前有的外围信息包括：字典分类、相关人员、喜欢；今后可能包括：点击数量、(所属栏目、所属专辑，这两类App端用不上，后台管理可能会用到)
     * @param one 专辑节目信息的Map，注意：这里不对map是否是专辑节目数据进行校验
     * @param personList 对应的人员信息列表
     * @param cataList 对应的字典分类信息列表
     * @param pubChannelList 发布栏目信息，已审核通过的栏目信息
     * @param favoriteList 对应的喜欢信息列表
     * @return
     */
    public static Map<String, Object> convert2Sma(Map<String, Object> one,
                                                   List<Map<String, Object>> personList,
                                                   List<Map<String, Object>> cataList,
                                                   List<Map<String, Object>> pubChannelList,
                                                   List<Map<String, Object>> favoriteList) {
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
        retM.put("ContentShareURL", getShareUrl_ZJ(preAddr, one.get("id")+""));//分享地址
        retM.put("ContentDesc", one.get("descn"));//P11-公共：说明
        retM.put("ContentStatus", one.get("smaStatus"));

        fillExtInfo(retM, "SEQU", personList, cataList, pubChannelList, favoriteList);//填充扩展信息

        retM.put("ContentSubCount", one.get("count"));//S01-特有：下级节目的个数

        retM.put("CTime", one.get("CTime"));//A1-管控：节目创建时间，目前以此进行排序

        return retM;
    }

    private static List<Map<String, Object>> fetchPersons(List<Map<String, Object>> personList, String resTableName, String resId) {//人员处理
        if (personList==null||personList.size()==0) return null;
        Map<String, Object> onePerson=null;
        List<Map<String, Object>> ret=new ArrayList<Map<String, Object>>();
        for (Map<String, Object> _p: personList) {
            if ((_p.get("resTableName")+"").equals(resTableName)&&(_p.get("resId")+"").equals(resId)) {
                onePerson=new HashMap<String, Object>();
                onePerson.put("RefName", _p.get("cName"));//关系名称
                onePerson.put("PerName", _p.get("pName"));//人员名称
                ret.add(onePerson);
            }
        }
        return ret.size()>0?ret:null;
    }
    private static List<Map<String, Object>> fetchCatas(List<Map<String, Object>> cataList, String resTableName, String resId) {//字典信息处理
        if (cataList==null||cataList.size()==0) return null;
        Map<String, Object> oneCata=null;
        List<Map<String, Object>> ret=new ArrayList<Map<String, Object>>();
        for (Map<String, Object> _c: cataList) {
            if ((_c.get("resTableName")+"").equals(resTableName)&&(_c.get("resId")+"").equals(resId)) {
                oneCata=new HashMap<String, Object>();
                oneCata.put("CataMName", _c.get("dictMName"));//大分类名称，树结构名称
                oneCata.put("CataMId", _c.get("dictMid"));//大分类Id
                oneCata.put("CataTitle", _c.get("pathNames"));//分类名称
                oneCata.put("CataDid", _c.get("dictDid"));//分类Id
                ret.add(oneCata);
            }
        }
        return ret.size()>0?ret:null;
    }
    private static List<Map<String, Object>> fetchChannels(List<Map<String, Object>> channelList, String resTableName, String resId) {//喜欢处理
        if (channelList==null||channelList.size()==0) return null;
        Map<String, Object> oneChn=null;
        List<Map<String, Object>> ret=new ArrayList<Map<String, Object>>();
        for (Map<String, Object> _c: channelList) {
            if ((_c.get("assetType")+"").equals(resTableName)&&(_c.get("assetId")+"").equals(resId)) {
                oneChn=new HashMap<String, Object>();
                oneChn.put("ChannelName", _c.get("channelName"));
                oneChn.put("PubTime", _c.get("pubTime"));
                oneChn.put("FlowFlag", _c.get("flowFlag"));
                oneChn.put("ChannelId", _c.get("channelId"));
                ret.add(oneChn);
            }
        }
        return ret.size()>0?ret:null;
    }
    private static int fetchFavorite(List<Map<String, Object>> favoriteList, String resTableName, String resId) {//喜欢处理
        if (favoriteList==null||favoriteList.size()==0) return 0;
        int ret=0;
        for (Map<String, Object> _f: favoriteList) {
            if ((_f.get("resTableName")+"").equals(resTableName)&&(_f.get("resId")+"").equals(resId)) {
                ret=1;
                break;
            }
        }
        return ret;
    }

    /** 计算分享地址的功能 */
    public static final String preAddr="http://www.wotingfm.com:908/CM/mweb";//分享地址前缀
    public static final String getShareUrl_JM(String preUrl, String contentId) {//的到节目的分享地址
        return preUrl+"/jm/"+contentId+"/content.html";
    }
    public static final String getShareUrl_ZJ(String preUrl, String contentId) {//的到专辑的分享地址
        return preUrl+"/zj/"+contentId+"/content.html";
    }
    public static final String getShareUrl_DT(String preUrl, String contentId) {//的到电台的分享地址
        return preUrl+"/dt/"+contentId+"/content.html";
    }

    /**
     * 从媒体类型转换为资源表名
     * @param mediaType 媒体类型
     * @return 资源表名
     */
    public static String getResTableName(String mediaType) {
        if (mediaType.equals("RADIO")) return "wt_Broadcast";
        else
        if (mediaType.equals("AUDIO")) return "wt_MediaAsset";
        else
        if (mediaType.equals("SEQU")) return "wt_SeqMediaAsset";
        else
        return "wt_Document";
    }

    /**
     * 从资源表名转换为媒体类型
     * @param resTableName 资源表名
     * @return 媒体类型
     */
    public static String getMediaType(String resTableName) {
        if (resTableName.equals("wt_Broadcast")) return "RADIO";
        else
        if (resTableName.equals("wt_MediaAsset")) return "AUDIO";
        else
        if (resTableName.equals("wt_SeqMediaAsset")) return "SEQU";
        else
        return "TEXT";
    }
}