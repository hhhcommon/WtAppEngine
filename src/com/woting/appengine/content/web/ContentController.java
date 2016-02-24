package com.woting.appengine.content.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.content.service.ContentService;
import com.woting.appengine.mobile.session.model.MobileSession;

@Controller
@RequestMapping(value="/content/")
public class ContentController {
    @Resource
    private ContentService contentService;

    @RequestMapping(value="mainPage.do")
    @ResponseBody
    public Map<String,Object> mainPage(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
            }
            if (map.get("ReturnType")!=null) return map;

            List<Map<String, Object>> ml = new ArrayList<Map<String, Object>>();
            List<Map<String, Object>> sl=null;
            Map<String, Object> bcClass=null, bcItem=null;
            bcClass=new HashMap<String, Object>();//一个分类
            bcClass.put("CatalogType", "1");
            bcClass.put("CatalogId", "001");
            bcClass.put("CatalogName", "推荐");
            bcClass.put("CatalogImg", "a.jpg");
            bcClass.put("PageSize", "3");//当前列表元素个数
            bcClass.put("AllListSize", "7");//本分类列表元素个数
            //------------------
            sl = new ArrayList<Map<String, Object>>();
            bcItem=new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京新闻广播");
            bcItem.put("ContentId", "003");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "mms://alive.rbc.cn/fm1006");
            bcItem.put("CurrentContent", "时政要闻");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "AUDIO"); //电台
            bcItem.put("ContentName", "天下大事");
            bcItem.put("ContentId", "002");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "mms://alive.rbc.cn/fm1073");
            bcItem.put("Actor", "李界观");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "SEQU"); //系列节目
            bcItem.put("ContentName", "观复嘟嘟");
            bcItem.put("ContentId", "001");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "/getSeqMaInfo.do?ContentId=001");
            bcItem.put("Actor", "马未都");//当前节目
            sl.add(bcItem);
            bcClass.put("SubList", sl);
            //------------------
            ml.add(bcClass);
            bcClass=new HashMap<String, Object>();//一个分类
            bcClass.put("CatalogType", "1");
            bcClass.put("CatalogId", "002");
            bcClass.put("CatalogName", "排行");
            bcClass.put("CatalogImg", "a.jpg");
            bcClass.put("PageSize", "3");//当前列表元素个数
            bcClass.put("AllListSize", "50");//本分类列表元素个数
            //------------------
            sl = new ArrayList<Map<String, Object>>();
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京体育广播");
            bcItem.put("ContentId", "002");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "mms://alive.rbc.cn/fm1025");
            bcItem.put("CurrentContent", "经典回顾");//当前节目
            sl.add(bcItem);
            bcItem=new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京音乐广播");
            bcItem.put("ContentId", "004");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "mms://alive.rbc.cn/fm974");
            bcItem.put("CurrentContent", "财经报道");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京外语广播");
            bcItem.put("ContentId", "001");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "mms://alive.rbc.cn/am774");
            bcItem.put("CurrentContent", "路况信息");//当前节目
            sl.add(bcItem);
            bcClass.put("SubList", sl);
            //------------------
            ml.add(bcClass);
            bcClass=new HashMap<String, Object>();//一个分类
            bcClass.put("CatalogType", "1");
            bcClass.put("CatalogId", "003");
            bcClass.put("CatalogName", "历史");
            bcClass.put("CatalogImg", "a.jpg");
            bcClass.put("PageSize", "3");//当前列表元素个数
            bcClass.put("AllListSize", "8");//本分类列表元素个数
            //------------------
            sl = new ArrayList<Map<String, Object>>();
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京爱家广播");
            bcItem.put("ContentId", "005");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "mms://alive.rbc.cn/am927");
            bcItem.put("CurrentContent", "评书联播");//当前节目
            sl.add(bcItem);
            bcItem=new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京古典音乐");
            bcItem.put("ContentId", "004");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "mms://alive.rbc.cn/cfm986");
            bcItem.put("CurrentContent", "财经报道");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京通俗音乐");
            bcItem.put("ContentId", "001");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "mms://alive.rbc.cn/cfm970");
            bcItem.put("CurrentContent", "路况信息");//当前节目
            sl.add(bcItem);
            bcClass.put("SubList", sl);
            //------------------
            ml.add(bcClass);
            map.put("ReturnType", "1001");
            map.put("ResultList", ml);
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="getListByCatalog.do")
    @ResponseBody
    public Map<String,Object> getListByCatelog(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
            }
            if (map.get("ReturnType")!=null) return map;

            String _temp=m.get("CatalogType")+"";
            if (_temp!=null&&_temp.equals("001")) {
                return getListByZone(request);
            } else {
                //1-获取列表
                List<Map<String, Object>> sl=null;
                Map<String, Object> bcClass=null, bcItem=null;
                bcClass=new HashMap<String, Object>();//一个分类
                bcClass.put("CatalogType", "001");
                bcClass.put("CatalogId", "002");
                bcClass.put("CatalogName", "娱乐");
                bcClass.put("CatalogImg", "a.jpg");
                bcClass.put("PageSize", "3");//当前列表元素个数
                bcClass.put("AllListSize", "50");//本分类列表元素个数
                //------------------
                sl = new ArrayList<Map<String, Object>>();
                bcItem=new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("ContentName", "北京怀旧金曲");
                bcItem.put("ContentId", "001");
                bcItem.put("ContentImg", "images/dft_broadcast.png");
                bcItem.put("ContentURI", "mms://alive.rbc.cn/cfm1075");
                bcItem.put("CurrentContent", "激情岁月");//当前节目
                sl.add(bcItem);
                bcItem=new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("ContentName", "北京教学广播");
                bcItem.put("ContentId", "001");
                bcItem.put("ContentImg", "images/dft_broadcast.png");
                bcItem.put("ContentURI", "mms://alive.rbc.cn/cfm994");
                bcItem.put("CurrentContent", "古典音乐");//当前节目
                sl.add(bcItem);
                bcItem=new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("ContentName", "北京长书广播");
                bcItem.put("ContentId", "008");
                bcItem.put("ContentImg", "images/dft_broadcast.png");
                bcItem.put("ContentURI", "mms://alive.rbc.cn/cfm1043");
                bcItem.put("CurrentContent", "华语排行榜");//当前节目
                sl.add(bcItem);
                bcItem = new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("ContentName", "北京戏曲曲艺");
                bcItem.put("ContentId", "101");
                bcItem.put("ContentImg", "images/dft_broadcast.png");
                bcItem.put("ContentURI", "mms://alive.rbc.cn/cfm1051");
                bcItem.put("CurrentContent", "津门乐声");//当前节目
                sl.add(bcItem);
                bcItem = new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("ContentName", "北京欢乐时光");
                bcItem.put("ContentId", "201");
                bcItem.put("ContentImg", "images/dft_broadcast.png");
                bcItem.put("ContentURI", "mms://alive.rbc.cn/cfm1065");
                bcItem.put("CurrentContent", "港台音乐");//当前节目
                sl.add(bcItem);
                bcItem = new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("ContentName", "河北新闻广播");
                bcItem.put("ContentId", "301");
                bcItem.put("ContentImg", "images/dft_broadcast.png");
                bcItem.put("ContentURI", "mms://audio1.hebradio.com/live1");
                bcItem.put("CurrentContent", "快乐童年");//当前节目
                sl.add(bcItem);
                bcItem = new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("ContentName", "河北经济广播");
                bcItem.put("ContentId", "401");
                bcItem.put("ContentImg", "images/dft_broadcast.png");
                bcItem.put("ContentURI", "mms://audio1.hebradio.com/live2");
                bcItem.put("CurrentContent", "古筝课堂");//当前节目
                sl.add(bcItem);
                bcItem = new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("ContentName", "河北交通广播");
                bcItem.put("ContentId", "201");
                bcItem.put("ContentImg", "images/dft_broadcast.png");
                bcItem.put("ContentURI", "mms://audio1.hebradio.com/live3");
                bcItem.put("CurrentContent", "文化报道");//当前节目
                sl.add(bcItem);
                bcClass.put("SubList", sl);
                //------------------
                map.put("ReturnType", "1001");
                map.put("ResultList", bcClass);
                return map;
            }
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    private Map<String,Object> getListByZone(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //1-获取版本
            List<Map<String, Object>> sl=null;
            Map<String, Object> bcClass=null, bcItem=null;
            bcClass=new HashMap<String, Object>();//一个分类
            bcClass.put("CatalogType", "1");
            bcClass.put("CatalogId", "001");
            bcClass.put("CatalogName", "大连");
            bcClass.put("CatalogImg", "a.jpg");
            bcClass.put("PageSize", "8");//当前列表元素个数
            bcClass.put("AllListSize", "24");//本分类列表元素个数
            //------------------
            sl = new ArrayList<Map<String, Object>>();
            bcItem=new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "大连新闻广播");
            bcItem.put("ContentId", "001");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "mms://218.61.6.228/xwgb?NSMwIzE=");
            bcItem.put("CurrentContent", "激情岁月");//当前节目
            sl.add(bcItem);
            bcItem=new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "大连体育休闲广播");
            bcItem.put("ContentId", "001");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "mms://218.61.6.228/tygb?OCMwIzE=");
            bcItem.put("CurrentContent", "整点快报");//当前节目
            sl.add(bcItem);
            bcItem=new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "大连交通广播");
            bcItem.put("ContentId", "008");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "mms://218.61.6.228/jtgb?NiMwIzE=");
            bcItem.put("CurrentContent", "广播购物");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "大连财经广播");
            bcItem.put("ContentId", "101");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "mms://218.61.6.228/cjgb?MTEjMCMx");
            bcItem.put("CurrentContent", "英语PK台");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "大连新城市广播");
            bcItem.put("ContentId", "201");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentURI", "mms://218.61.6.228/sqgb?NyMwIzE=");
            bcItem.put("CurrentContent", "健康加油站");//当前节目
            sl.add(bcItem);
//            bcItem = new HashMap<String, Object>();
//            bcItem.put("MediaType", "RADIO"); //电台
//            bcItem.put("ContentName", "北京古典音乐");
//            bcItem.put("ContentId", "301");
//            bcItem.put("ContentImg", "images/dft_broadcast.png");
//            bcItem.put("ContentURI", "mms://a.b.c/aaa.mpg");
//            bcItem.put("CurrentContent", "华夏神韵");//当前节目
//            sl.add(bcItem);
//            bcItem = new HashMap<String, Object>();
//            bcItem.put("MediaType", "RADIO"); //电台
//            bcItem.put("ContentName", "北京教育广播");
//            bcItem.put("ContentId", "401");
//            bcItem.put("ContentImg", "images/dft_broadcast.png");
//            bcItem.put("ContentURI", "mms://a.b.c/aaa.mpg");
//            bcItem.put("CurrentContent", "现代汉语");//当前节目
//            sl.add(bcItem);
//            bcItem = new HashMap<String, Object>();
//            bcItem.put("MediaType", "RADIO"); //电台
//            bcItem.put("ContentName", "北京故事广播");
//            bcItem.put("ContentId", "201");
//            bcItem.put("ContentImg", "images/dft_broadcast.png");
//            bcItem.put("ContentURI", "mms://a.b.c/aaa.mpg");
//            bcItem.put("CurrentContent", "阳光茶园");//当前节目
//            sl.add(bcItem);
            bcClass.put("SubList", sl);
            //------------------
            map.put("ReturnType", "1001");
            map.put("ResultList", bcClass);
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    /**
     * 获得标题图
     * @param request
     * @return 标题图格式
     */
    @RequestMapping(value="getLoopImgs.do")
    @ResponseBody
    public Map<String,Object> getLoopImgs(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
            }
            if (map.get("ReturnType")!=null) return map;

            //1-获取列表
            List<Map<String, Object>> sl=null;
            Map<String, Object> bcClass=null, bcItem=null;
            bcClass=new HashMap<String, Object>();//一个分类
            bcClass.put("CatalogType", "001");
            bcClass.put("CatalogId", "002");
            bcClass.put("CatalogName", "娱乐");
            bcClass.put("CatalogImg", "a.jpg");
            bcClass.put("PageSize", "3");//当前列表元素个数
            bcClass.put("AllListSize", "3");//本分类列表元素个数
            //------------------
            sl = new ArrayList<Map<String, Object>>();
            bcItem=new HashMap<String, Object>();
            bcItem.put("ImgUrl", "asset/contents/imgs/1280115949992.jpg");
            bcItem.put("ImgIdx", "0");
            bcItem.put("ImgDesc", "北京怀旧金曲");
            bcItem.put("ImgContentUrl", "abc/content.do?contentId=1");
            sl.add(bcItem);
            bcItem.put("ImgUrl", "asset/contents/imgs/1303967876491.jpg");
            bcItem.put("ImgIdx", "1");
            bcItem.put("ImgDesc", "故事新编");
            bcItem.put("ImgContentUrl", "abc/content.do?contentId=2");
            sl.add(bcItem);
            bcItem.put("ImgUrl", "asset/contents/imgs/m_1303967844788.jpg");
            bcItem.put("ImgIdx", "2");
            bcItem.put("ImgDesc", "新闻纵览");
            bcItem.put("ImgContentUrl", "abc/content.do?contentId=3");
            sl.add(bcItem);
            bcItem.put("ImgUrl", "asset/contents/imgs/m_1303967870670.jpg");
            bcItem.put("ImgIdx", "3");
            bcItem.put("ImgDesc", "体坛风云");
            bcItem.put("ImgContentUrl", "abc/content.do?contentId=3");
            sl.add(bcItem);
           bcClass.put("SubList", sl);
            //------------------
            map.put("ReturnType", "1001");
            map.put("ResultList", bcClass);
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    /**
     * 获得系列节目内容
     * @param request
     * @return 系列节目内容
     */
    @RequestMapping(value="getSeqMaInfo.do")
    @ResponseBody
    public Map<String,Object> getSeqMaInfo(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
            }
            if (map.get("ReturnType")!=null) return map;

            //2-得到系列内容的Id
            String contentId=m.get("ContentId")+"";
            if (StringUtils.isNullOrEmptyOrSpace(contentId)) {
                contentId=request.getParameter("ContentId");
            }
            if (StringUtils.isNullOrEmptyOrSpace(contentId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获得系列内容Id");
                return map;
            }
            Map<String, Object> smInfo=contentService.getSeqMaInfo(contentId);
            if (smInfo!=null&&smInfo.size()>0) {
                map.put("ResultList", smInfo);
                map.put("ReturnType", "1001");
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "没有查到任何内容");
            }
            return map;
//            //0-处理访问
//            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
//            if (m!=null&&m.size()>0) {
//                MobileParam mp=MobileUtils.getMobileParam(m);
//                MobileKey sk=(mp==null?null:mp.getMobileKey());
//                if (sk!=null){con
//                    map.put("SessionId", sk.getSessionId());
//                    MobileSession ms=smm.getSession(sk);
//                    if (ms!=null) ms.access();
//                }
//                //获得SessionId，从而得到用户信息，获得偏好信息
//            }
//            //1-获取列表
//            List<Map<String, Object>> sl=null;
//            Map<String, Object> bcClass=null, bcItem=null;
//            bcClass=new HashMap<String, Object>();//一个分类
//            bcClass.put("SequId", "001");
//            bcClass.put("ContentName", "观复嘟嘟");
//            bcClass.put("ContentId", "001");
//            bcClass.put("ContentImg", "images/dft_broadcast.png");
//            bcClass.put("Actor", "马未都");//当前节目
//            bcClass.put("PageSize", "3");//当前列表元素个数
//            bcClass.put("AllListSize", "3");//本分类列表元素个数
//            //------------------
//            sl = new ArrayList<Map<String, Object>>();
//            bcItem=new HashMap<String, Object>();
//            bcItem.put("MediaType", "AUDIO");
//            bcItem.put("ContentName", "001说勤");
//            bcItem.put("ContentNum", "系列号");
//            bcItem.put("ContentId", "002");
//            bcItem.put("ContentImg", "images/dft_broadcast.png");
//            bcItem.put("ContentURI", "mms://alive.rbc.cn/fm1073");
//            bcItem.put("Actor", "马未都");//当前节目
//            bcItem.put("Desc", "说明........");//当前节目
//            sl.add(bcItem);
//            bcItem=new HashMap<String, Object>();
//            bcItem.put("MediaType", "AUDIO");
//            bcItem.put("ContentName", "002说悍");
//            bcItem.put("ContentNum", "2");
//            bcItem.put("ContentId", "002");
//            bcItem.put("ContentImg", "images/dft_broadcast.png");
//            bcItem.put("ContentURI", "mms://alive.rbc.cn/fm1073");
//            bcItem.put("Actor", "马未都");//当前节目
//            bcItem.put("Desc", "说明........");//当前节目
//            sl.add(bcItem);
//            bcItem=new HashMap<String, Object>();
//            bcItem.put("MediaType", "AUDIO");
//            bcItem.put("ContentName", "003说禁");
//            bcItem.put("ContentNum", "3");
//            bcItem.put("ContentId", "002");
//            bcItem.put("ContentImg", "images/dft_broadcast.png");
//            bcItem.put("ContentURI", "mms://alive.rbc.cn/fm1073");
//            bcItem.put("Actor", "马未都");//当前节目
//            bcItem.put("Desc", "说明........");//当前节目
//            sl.add(bcItem);
//            bcItem=new HashMap<String, Object>();
//            bcItem.put("MediaType", "AUDIO");
//            bcItem.put("ContentName", "004说堂");
//            bcItem.put("ContentNum", "4");
//            bcItem.put("ContentId", "002");
//            bcItem.put("ContentImg", "images/dft_broadcast.png");
//            bcItem.put("ContentURI", "mms://alive.rbc.cn/fm1073");
//            bcItem.put("Actor", "马未都");//当前节目
//            bcItem.put("Desc", "说明........");//当前节目
//            sl.add(bcItem);
//            bcItem=new HashMap<String, Object>();
//            bcItem.put("MediaType", "AUDIO");
//            bcItem.put("ContentName", "005说颂");
//            bcItem.put("ContentNum", "5");
//            bcItem.put("ContentId", "002");
//            bcItem.put("ContentImg", "images/dft_broadcast.png");
//            bcItem.put("ContentURI", "mms://alive.rbc.cn/fm1073");
//            bcItem.put("Actor", "马未都");//当前节目
//            bcItem.put("Desc", "说明........");//当前节目
//            sl.add(bcItem);
//            bcClass.put("SubList", sl);
//            //------------------
//            map.put("ReturnType", "1001");
//            map.put("ResultList", bcClass);
//            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }
}