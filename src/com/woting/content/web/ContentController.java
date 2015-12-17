package com.woting.content.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.woting.mobile.MobileUtils;
import com.woting.mobile.model.MobileParam;
import com.woting.mobile.session.mem.SessionMemoryManage;
import com.woting.mobile.session.model.MobileSession;
import com.woting.mobile.model.MobileKey;

@Controller
@RequestMapping(value="/content/")
public class ContentController {
    private SessionMemoryManage smm=SessionMemoryManage.getInstance();

    @RequestMapping(value="mainPage.do")
    @ResponseBody
    public Map<String,Object> mainPage(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-处理访问
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m!=null&&m.size()>0) {
                MobileParam mp=MobileUtils.getMobileParam(m);
                MobileKey sk=(mp==null?null:mp.getMobileKey());
                if (sk!=null){
                    map.put("SessionId", sk.getSessionId());
                    MobileSession ms=smm.getSession(sk);
                    if (ms!=null) ms.access();
                }
                //获得SessionId，从而得到用户信息，获得偏好信息
            }
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
            bcItem.put("RadioName", "北京新闻广播");
            bcItem.put("RadioId", "003");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://alive.rbc.cn/fm1006");
            bcItem.put("CurrentContent", "时政要闻");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("RadioName", "北京城市广播");
            bcItem.put("RadioId", "002");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://alive.rbc.cn/fm1073");
            bcItem.put("CurrentContent", "经典回顾");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("RadioName", "北京故事广播");
            bcItem.put("RadioId", "001");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://alive.rbc.cn/fm1039");
            bcItem.put("CurrentContent", "路况信息");//当前节目
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
            bcItem.put("RadioName", "北京体育广播");
            bcItem.put("RadioId", "002");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://alive.rbc.cn/fm1025");
            bcItem.put("CurrentContent", "经典回顾");//当前节目
            sl.add(bcItem);
            bcItem=new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("RadioName", "北京音乐广播");
            bcItem.put("RadioId", "004");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://alive.rbc.cn/fm974");
            bcItem.put("CurrentContent", "财经报道");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("RadioName", "北京外语广播");
            bcItem.put("RadioId", "001");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://alive.rbc.cn/am774");
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
            bcItem.put("RadioName", "北京爱家广播");
            bcItem.put("RadioId", "005");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://alive.rbc.cn/am927");
            bcItem.put("CurrentContent", "评书联播");//当前节目
            sl.add(bcItem);
            bcItem=new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("RadioName", "北京古典音乐");
            bcItem.put("RadioId", "004");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://alive.rbc.cn/cfm986");
            bcItem.put("CurrentContent", "财经报道");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("RadioName", "北京通俗音乐");
            bcItem.put("RadioId", "001");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://alive.rbc.cn/cfm970");
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
            //0-处理访问
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m!=null&&m.size()>0) {
                MobileParam mp=MobileUtils.getMobileParam(m);
                MobileKey sk=(mp==null?null:mp.getMobileKey());
                if (sk!=null){
                    map.put("SessionId", sk.getSessionId());
                    MobileSession ms=smm.getSession(sk);
                    if (ms!=null) ms.access();
                }
                //获得SessionId，从而得到用户信息，获得偏好信息
            }
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
                bcItem.put("RadioName", "北京怀旧金曲");
                bcItem.put("RadioId", "001");
                bcItem.put("RadioImg", "images/dft_broadcast.png");
                bcItem.put("RadioURI", "mms://alive.rbc.cn/cfm1075");
                bcItem.put("CurrentContent", "激情岁月");//当前节目
                sl.add(bcItem);
                bcItem=new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("RadioName", "北京教学广播");
                bcItem.put("RadioId", "001");
                bcItem.put("RadioImg", "images/dft_broadcast.png");
                bcItem.put("RadioURI", "mms://alive.rbc.cn/cfm994");
                bcItem.put("CurrentContent", "古典音乐");//当前节目
                sl.add(bcItem);
                bcItem=new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("RadioName", "北京长书广播");
                bcItem.put("RadioId", "008");
                bcItem.put("RadioImg", "images/dft_broadcast.png");
                bcItem.put("RadioURI", "mms://alive.rbc.cn/cfm1043");
                bcItem.put("CurrentContent", "华语排行榜");//当前节目
                sl.add(bcItem);
                bcItem = new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("RadioName", "北京戏曲曲艺");
                bcItem.put("RadioId", "101");
                bcItem.put("RadioImg", "images/dft_broadcast.png");
                bcItem.put("RadioURI", "mms://alive.rbc.cn/cfm1051");
                bcItem.put("CurrentContent", "津门乐声");//当前节目
                sl.add(bcItem);
                bcItem = new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("RadioName", "北京欢乐时光");
                bcItem.put("RadioId", "201");
                bcItem.put("RadioImg", "images/dft_broadcast.png");
                bcItem.put("RadioURI", "mms://alive.rbc.cn/cfm1065");
                bcItem.put("CurrentContent", "港台音乐");//当前节目
                sl.add(bcItem);
                bcItem = new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("RadioName", "河北新闻广播");
                bcItem.put("RadioId", "301");
                bcItem.put("RadioImg", "images/dft_broadcast.png");
                bcItem.put("RadioURI", "mms://audio1.hebradio.com/live1");
                bcItem.put("CurrentContent", "快乐童年");//当前节目
                sl.add(bcItem);
                bcItem = new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("RadioName", "河北经济广播");
                bcItem.put("RadioId", "401");
                bcItem.put("RadioImg", "images/dft_broadcast.png");
                bcItem.put("RadioURI", "mms://audio1.hebradio.com/live2");
                bcItem.put("CurrentContent", "古筝课堂");//当前节目
                sl.add(bcItem);
                bcItem = new HashMap<String, Object>();
                bcItem.put("MediaType", "RADIO"); //电台
                bcItem.put("RadioName", "河北交通广播");
                bcItem.put("RadioId", "201");
                bcItem.put("RadioImg", "images/dft_broadcast.png");
                bcItem.put("RadioURI", "mms://audio1.hebradio.com/live3");
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
//            //0-处理访问
//            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
//            if (m!=null&&m.size()>0) {
//                MobileParam mp=MobileUtils.getMobileParam(m);
//                MobileKey sk=(mp==null?null:mp.getMobileKey());
//                if (sk!=null){
//                    map.put("SessionId", sk.getSessionId());
//                    MobileSession ms=smm.getSession(sk);
//                    if (ms!=null) ms.access();
//                }
//                //获得SessionId，从而得到用户信息，获得偏好信息
//            }
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
            bcItem.put("RadioName", "大连新闻广播");
            bcItem.put("RadioId", "001");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://218.61.6.228/xwgb?NSMwIzE=");
            bcItem.put("CurrentContent", "激情岁月");//当前节目
            sl.add(bcItem);
            bcItem=new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("RadioName", "大连体育休闲广播");
            bcItem.put("RadioId", "001");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://218.61.6.228/tygb?OCMwIzE=");
            bcItem.put("CurrentContent", "整点快报");//当前节目
            sl.add(bcItem);
            bcItem=new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("RadioName", "大连交通广播");
            bcItem.put("RadioId", "008");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://218.61.6.228/jtgb?NiMwIzE=");
            bcItem.put("CurrentContent", "广播购物");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("RadioName", "大连财经广播");
            bcItem.put("RadioId", "101");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://218.61.6.228/cjgb?MTEjMCMx");
            bcItem.put("CurrentContent", "英语PK台");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("RadioName", "大连新城市广播");
            bcItem.put("RadioId", "201");
            bcItem.put("RadioImg", "images/dft_broadcast.png");
            bcItem.put("RadioURI", "mms://218.61.6.228/sqgb?NyMwIzE=");
            bcItem.put("CurrentContent", "健康加油站");//当前节目
            sl.add(bcItem);
//            bcItem = new HashMap<String, Object>();
//            bcItem.put("MediaType", "RADIO"); //电台
//            bcItem.put("RadioName", "北京古典音乐");
//            bcItem.put("RadioId", "301");
//            bcItem.put("RadioImg", "images/dft_broadcast.png");
//            bcItem.put("RadioURI", "mms://a.b.c/aaa.mpg");
//            bcItem.put("CurrentContent", "华夏神韵");//当前节目
//            sl.add(bcItem);
//            bcItem = new HashMap<String, Object>();
//            bcItem.put("MediaType", "RADIO"); //电台
//            bcItem.put("RadioName", "北京教育广播");
//            bcItem.put("RadioId", "401");
//            bcItem.put("RadioImg", "images/dft_broadcast.png");
//            bcItem.put("RadioURI", "mms://a.b.c/aaa.mpg");
//            bcItem.put("CurrentContent", "现代汉语");//当前节目
//            sl.add(bcItem);
//            bcItem = new HashMap<String, Object>();
//            bcItem.put("MediaType", "RADIO"); //电台
//            bcItem.put("RadioName", "北京故事广播");
//            bcItem.put("RadioId", "201");
//            bcItem.put("RadioImg", "images/dft_broadcast.png");
//            bcItem.put("RadioURI", "mms://a.b.c/aaa.mpg");
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
            //0-处理访问
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m!=null&&m.size()>0) {
                MobileParam mp=MobileUtils.getMobileParam(m);
                MobileKey sk=(mp==null?null:mp.getMobileKey());
                if (sk!=null){
                    map.put("SessionId", sk.getSessionId());
                    MobileSession ms=smm.getSession(sk);
                    if (ms!=null) ms.access();
                }
                //获得SessionId，从而得到用户信息，获得偏好信息
            }
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
            bcItem.put("ImgUrl", "abc/a.jpg");
            bcItem.put("ImgIdx", "0");
            bcItem.put("ImgDesc", "北京怀旧金曲");
            bcItem.put("ImgContentUrl", "abc/content.do?contentId=1");
            sl.add(bcItem);
            bcItem.put("ImgUrl", "abc/a.jpg");
            bcItem.put("ImgIdx", "1");
            bcItem.put("ImgDesc", "故事新编");
            bcItem.put("ImgContentUrl", "abc/content.do?contentId=2");
            sl.add(bcItem);
            bcItem.put("ImgUrl", "abc/a.jpg");
            bcItem.put("ImgIdx", "2");
            bcItem.put("ImgDesc", "新闻纵览");
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
}