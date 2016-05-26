package com.woting.appengine.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.woting.appengine.content.service.ContentService;

/**
 * 内容方法类：内容数据的转换，主要——存储对象转换为显示对象。
 * @author wanghui
 *
 */
public abstract class ContentUtils {
    //转换电台
    public static Map<String, Object> convert2MediaMap_1(Map<String, Object> one, List<Map<String, Object>> cataList, List<Map<String, Object>> personList) {
        Map<String, Object> retM=new HashMap<String, Object>();

        retM.put("MediaType", "RADIO");

        retM.put("ContentId", one.get("id"));//P01-公共：ID
        retM.put("ContentName", one.get("bcTitle"));//P02-公共：名称
        retM.put("ContentPub", one.get("bcPublisher"));//P03-公共：发布者，集团名称
        retM.put("ContentImg", one.get("bcImg"));//P07-公共：相关图片
        retM.put("ContentPlay", one.get("flowURI"));//P08-公共：主播放Url
        retM.put("ContentShareURL", ContentService.getShareUrl_DT(ContentService.preAddr, one.get("id")+""));//分享地址
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
    public static Map<String, Object> convert2MediaMap_2(Map<String, Object> one, List<Map<String, Object>> cataList, List<Map<String, Object>> personList) {
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
        retM.put("ContentShareURL", ContentService.getShareUrl_JM(ContentService.preAddr, one.get("id")+""));//分享地址
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
    public static Map<String, Object> convert2MediaMap_3(Map<String, Object> one, List<Map<String, Object>> cataList, List<Map<String, Object>> personList) {
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
        retM.put("ContentShareURL", ContentService.getShareUrl_ZJ(ContentService.preAddr, one.get("id")+""));//分享地址
        retM.put("ContentDesc", one.get("descn"));//P11-公共：说明
        retM.put("ContentPersons", fetchPersons(personList, 3, retM.get("ContentId")+""));//P12-公共：相关人员列表
        retM.put("ContentCatalogs", fetchCatas(cataList, 3, retM.get("ContentId")+""));//P13-公共：所有分类列表
        retM.put("PlayCount", "1234");//P14-公共：播放次数

        retM.put("ContentSubCount", one.get("count"));//S01-特有：下级节目的个数

        retM.put("CTime", one.get("cTime"));//A1-管控：节目创建时间，目前以此进行排序

        return retM;
    }

    public static List<Map<String, Object>> fetchPersons(List<Map<String, Object>> personList, int resType, String resId) {
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
    public static List<Map<String, Object>> fetchCatas(List<Map<String, Object>> cataList, int resType, String resId) {
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
}