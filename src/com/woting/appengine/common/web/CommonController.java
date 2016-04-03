package com.woting.appengine.common.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spiritdata.framework.util.DateUtils;
import com.spiritdata.framework.util.StringUtils;
import com.woting.WtAppEngineConstants;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.content.service.ContentService;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.model.MobileParam;
import com.woting.appengine.mobile.session.mem.SessionMemoryManage;
import com.woting.appengine.mobile.session.model.MobileSession;
import com.woting.cm.core.channel.mem._CacheChannel;
import com.woting.cm.core.dict.mem._CacheDictionary;
import com.woting.cm.core.dict.model.DictDetail;
import com.woting.cm.core.dict.model.DictModel;
import com.woting.common.TreeUtils;
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.woting.passport.UGA.service.UserService;
import com.woting.passport.login.service.MobileUsedService;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.core.cache.CacheEle;

@Lazy(true)
@Controller
public class CommonController {
    @Resource
    private UserService userService;
    @Resource
    private MobileUsedService muService;
    @Resource
    private ContentService contentService;

    private SessionMemoryManage smm=SessionMemoryManage.getInstance();
    private _CacheDictionary _cd=null;
    private _CacheChannel _cc=null;

    @PostConstruct
    public void initParam() {
        //初始化栏目结构
        _cd=((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)).getContent();
        _cc=((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)).getContent();
    }

    /**
     * 进入App
     * @param request 请求对象。数据包含在Data流中，以json格式存储，其中必须包括手机串号。如：{"imei":"123456789023456789"}
     * @return 分为如下情况<br/>
     *   若有异常：{ReturnType:T, TClass:exception.class, Message: e.getMessage()}
     *   已经登录：{ReturnType:1001, sessionId:sid, userInfo:{userName:un, mphone:138XXXX2345, email:a@b.c, realName:实名, headImg:hiUrl}}
     *     其中用户信息若没有相关内容，则相关的key:value对就不存在
     *   还未登录：{ReturnType:1002, sessionId:sid}
     */
    @RequestMapping(value="/common/entryApp.do")
    @ResponseBody
    public Map<String,Object> entryApp(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
                return map;
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 1);
                if ((retM.get("ReturnType")+"").equals("2001")) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    MobileSession ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                    if ((retM.get("ReturnType")+"").equals("1002")) {
                        map.put("ReturnType", "1002");
                    } if ((retM.get("ReturnType")+"").equals("2002")) {
                        map.put("ReturnType", "2002");
                        map.put("Message", "无法找到相应的用户");
                    }else {
                        map.put("ReturnType", "1001");
                        map.put("UserInfo", ((UserPo)ms.getAttribute("user")).toHashMap4Mobile());
                    }
                }
                map.put("ServerStatus", "1"); //服务器状态
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="/common/getVersion.do")
    @ResponseBody
    public Map<String,Object> getVersion(HttpServletRequest request) {//不需要登录
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m!=null&&m.size()>0) {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                try {
                    map.put("SessionId", ms.getKey().getSessionId());
                } catch(Exception e) {}
                //1-获取版本
                String _temp=MobileUtils.getLastVersion();
                if (_temp==null) {
                    map.put("Version", "0.0.0.0.0");
                    map.put("PublishDate", "2015-09-18");
                } else {
                    String[] vs=_temp.split(",");
                    map.put("Version", vs[0]);
                    if (vs.length>1) {
                        map.put("PublishDate", vs[1]);
                    } else {
                        map.put("PublishDate", DateUtils.convert2LocalStr("yyyy-MM-dd", new Date()));
                    }
                    if (vs.length>2) {
                        String patchInfo="";
                        for (int i=2; i<vs.length; i++) {
                            patchInfo+=vs[i];
                        }
                        map.put("PatchInfo", patchInfo);
                    }
                }
            }
            map.put("ServerStatus", "1");
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="/common/getZoneList.do")
    @ResponseBody
    public Map<String,Object> getZoneList(HttpServletRequest request) {
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
                //获得上级地区分类Id
            }
            //1-获取地区信息
            List<Map<String, Object>> zl=new ArrayList<Map<String, Object>>();
            Map<String, Object> zone;
            zone=new HashMap<String, Object>();
            zone.put("ZoneId", "001");
            zone.put("ZoneName", "北京");
            zl.add(zone);
            zone=new HashMap<String, Object>();
            zone.put("ZoneId", "001");
            zone.put("ZoneName", "北京");
            zl.add(zone);
            zone.put("ZoneId", "002");
            zone.put("ZoneName", "天津");
            zl.add(zone);
            zone.put("ZoneId", "003");
            zone.put("ZoneName", "上海");
            zl.add(zone);
            zone.put("ZoneId", "004");
            zone.put("ZoneName", "广州");
            zl.add(zone);
            zone.put("ZoneId", "005");
            zone.put("ZoneName", "深圳");
            zl.add(zone);
            zone.put("ZoneId", "006");
            zone.put("ZoneName", "重庆");
            zl.add(zone);
            zone.put("ZoneId", "007");
            zone.put("ZoneName", "杭州");
            zl.add(zone);
            map.put("ReturnType", "1001");
            map.put("ZoneList", zl);
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="/mainPage.do")
    @ResponseBody
    public Map<String,Object> mainPage(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            MobileSession ms=null;
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
                if ((retM.get("ReturnType")+"").equals("2001")) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                }
            }
            if (map.get("ReturnType")!=null) return map;

            Map<String, Object> cl=contentService.getMainPage(ms.getKey().getUserId());
            if (cl!=null&&cl.size()>0) {
                map.put("ResultList", cl);
                map.put("ReturnType", "1001");
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "没有查到任何内容");
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="/getHotKeys.do")
    @ResponseBody
    public Map<String,Object> getHotKeys(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
                if ((retM.get("ReturnType")+"").equals("2001")) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    MobileSession ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                }
            }
            if (map.get("ReturnType")!=null) return map;

            map.put("ReturnType", "1001");
            map.put("KeyList", "逻辑思维,郭德纲,芈月传奇,数学,恐怖主义,鬼吹灯,盗墓笔记,老梁说事");
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="/getCatalogInfo.do")
    @ResponseBody
    public Map<String,Object> getCatalogInfo(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
                if ((retM.get("ReturnType")+"").equals("2001")) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    MobileSession ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-得到模式Id
            String catalogType=(String)m.get("CatalogType");
            if (StringUtils.isNullOrEmptyOrSpace(catalogType)) {
                catalogType=request.getParameter("CatalogType");
            }
            if (StringUtils.isNullOrEmptyOrSpace(catalogType)) {
                catalogType="-1";
            }
            //2-得到字典项Id或父栏目Id
            String catalogId=(String)m.get("CatalogId");
            if (StringUtils.isNullOrEmptyOrSpace(catalogId)) {
                catalogId=request.getParameter("CatalogId");
            }
            if (StringUtils.isNullOrEmptyOrSpace(catalogId)) {
                catalogId=null;
            }
            //3-得到返回类型
            String resultType=(String)m.get("ResultType");
            if (StringUtils.isNullOrEmptyOrSpace(resultType)) {
                resultType=request.getParameter("ResultType");
            }
            if (!StringUtils.isNullOrEmptyOrSpace(resultType)) {
                resultType="2";
            }
            //4-得到相对层次
            String relLevel=(String)m.get("RelLevel");
            if (StringUtils.isNullOrEmptyOrSpace(relLevel)) {
                relLevel=request.getParameter("RelLevel");
            }
            if (!StringUtils.isNullOrEmptyOrSpace(relLevel)) {
                relLevel="1";
            }

            TreeNode<? extends TreeNodeBean> root=null;
            if (catalogType.equals("-1")) {
                root=_cc.channelTree;
            } else {
                DictModel dm=_cd.getDictModelById(catalogType);
                if (dm!=null&&dm.dictTree!=null) root=dm.dictTree;
            }
            if (root!=null) {
                if (catalogId!=null) root=root.findNode(catalogId);
                if (root!=null) {
                    TreeUtils.getLevelTree(root, Integer.parseInt(relLevel));
                }
            }
            map.put("ReturnType", "1001");
            List<Map<String, String>> demoData=new ArrayList<Map<String, String>>();
            Map<String, String> item=new HashMap<String, String>();
            item.put("CatalogType", "001");item.put("CatalogId", "001");item.put("CatalogImg", "img/a.jpg");item.put("CatalogName", "段子笑话");demoData.add(item);
            item=new HashMap<String, String>();
            item.put("CatalogType", "001");item.put("CatalogId", "002");item.put("CatalogImg", "img/a.jpg");item.put("CatalogName", "心理推理");demoData.add(item);
            item=new HashMap<String, String>();
            item.put("CatalogType", "001");item.put("CatalogId", "003");item.put("CatalogImg", "img/a.jpg");item.put("CatalogName", "生活百科");demoData.add(item);
            item=new HashMap<String, String>();
            item.put("CatalogType", "001");item.put("CatalogId", "004");item.put("CatalogImg", "img/a.jpg");item.put("CatalogName", "两性情感");demoData.add(item);
            item=new HashMap<String, String>();
            item.put("CatalogType", "001");item.put("CatalogId", "005");item.put("CatalogImg", "img/a.jpg");item.put("CatalogName", "星座风水");demoData.add(item);
            item=new HashMap<String, String>();
            item.put("CatalogType", "001");item.put("CatalogId", "005");item.put("CatalogImg", "img/a.jpg");item.put("CatalogName", "商业财经");demoData.add(item);
            item=new HashMap<String, String>();
            item.put("CatalogType", "001");item.put("CatalogId", "006");item.put("CatalogImg", "img/a.jpg");item.put("CatalogName", "军事前沿");demoData.add(item);
            item=new HashMap<String, String>();
            item.put("CatalogType", "001");item.put("CatalogId", "007");item.put("CatalogImg", "img/a.jpg");item.put("CatalogName", "历史地理");demoData.add(item);
            item=new HashMap<String, String>();
            item.put("CatalogType", "001");item.put("CatalogId", "008");item.put("CatalogImg", "img/a.jpg");item.put("CatalogName", "儿童亲子");demoData.add(item);
            item=new HashMap<String, String>();
            item.put("CatalogType", "001");item.put("CatalogId", "009");item.put("CatalogImg", "img/a.jpg");item.put("CatalogName", "公开课堂");demoData.add(item);
            item=new HashMap<String, String>();
            item.put("CatalogType", "001");item.put("CatalogId", "010");item.put("CatalogImg", "img/a.jpg");item.put("CatalogName", "教育学习");demoData.add(item);
            item=new HashMap<String, String>();
            item.put("CatalogType", "001");item.put("CatalogId", "011");item.put("CatalogImg", "img/a.jpg");item.put("CatalogName", "女性时尚");demoData.add(item);
            item=new HashMap<String, String>();
            item.put("CatalogType", "001");item.put("CatalogId", "012");item.put("CatalogImg", "img/a.jpg");item.put("CatalogName", "体育世界");demoData.add(item);

            map.put("CatalogTree", demoData);
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="/searchByText.do")
    @ResponseBody
    public Map<String,Object> searchByText(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
                if ((retM.get("ReturnType")+"").equals("2001")) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    MobileSession ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //获得查询串
            String searchStr=(String)m.get("SearchStr");
            if (StringUtils.isNullOrEmptyOrSpace(searchStr)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法得到查询串");
                return map;
            }
            //获得结果类型，0获得一个列表，1获得分类列表，这个列表根据content字段处理，这个字段目前没有用到
            int resultType=0;
            try {
                resultType=Integer.parseInt(m.get("ResultType")+"");
            } catch(Exception e) {
            }

            Map<String, Object> cl=contentService.searchAll(searchStr, resultType);
            if (cl!=null&&cl.size()>0) {
                map.put("ResultType", cl.get("ResultType"));
                cl.remove("ResultType");
                map.put("ResultList", cl);
                map.put("ReturnType", "1001");
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "没有查到任何内容");
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="/searchByVoice.do")
    @ResponseBody
    public Map<String,Object> searchByVoice(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
                if ((retM.get("ReturnType")+"").equals("2001")) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    MobileSession ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //获得查询串
            String searchStr=(String)m.get("SearchStr");
            if (StringUtils.isNullOrEmptyOrSpace(searchStr)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法得到查询串");
                return map;
            }
            //获得结果类型，0获得一个列表，1获得分类列表，这个列表根据content字段处理，这个字段目前没有用到
            int resultType=0;
            try {
                resultType=Integer.parseInt(m.get("ResultType")+"");
            } catch(Exception e) {
            }

            Map<String, Object> cl=contentService.searchAll(searchStr, resultType);
            if (cl!=null&&cl.size()>0) {
                map.put("ResultType", cl.get("ResultType"));
                cl.remove("ResultType");
                map.put("ResultList", cl);
                map.put("ReturnType", "1001");
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "没有查到任何内容");
            }
            return map;
//            List<Map<String, Object>> rl=new ArrayList<Map<String, Object>>();
//            Map<String, Object> media;
//            media=new HashMap<String, Object>();
//            media.put("MediaType", "RES"); //文件资源
//            media.put("ResType", "mp3");
//            media.put("ResClass", "评书");
//            media.put("ResStyle", "文学名著");
//            media.put("ResActor", "张三");
//            media.put("ResName", "三打白骨精");
//            media.put("ResImg", "images/dft_res.png");
//            media.put("ResURI", "http://www.woting.fm/resource/124osdf3.mp3");
//            media.put("ResTime", "14:35");
//            rl.add(media);
//            media=new HashMap<String, Object>();
//            media.put("MediaType", "RADIO"); //电台
//            media.put("RadioName", "CRI英语漫听电台");
//            media.put("RadioId", "001");
//            media.put("RadioImg", "images/dft_broadcast.png");
//            media.put("RadioURI", "mms://live.cri.cn/english");
//            media.put("CurrentContent", "路况信息");//当前节目
//            rl.add(media);
//            media=new HashMap<String, Object>();
//            media.put("MediaType", "RES"); //文件资源
//            media.put("ResType", "mp3");
//            media.put("ResClass", "歌曲");
//            media.put("ResStyle", "摇滚");
//            media.put("ResActor", "李四");
//            media.put("ResName", "歌曲名称");
//            media.put("ResImg", "images/dft_actor.png");
//            media.put("ResURI", "http://www.woting.fm/resource/124osdf3.mp3");
//            media.put("ResTime", "4:35");
//            rl.add(media);
//            media=new HashMap<String, Object>();
//            media.put("MediaType", "RADIO"); //电台
//            media.put("RadioName", "CRI乡村民谣音乐");
//            media.put("RadioId", "003");
//            media.put("RadioImg", "images/dft_broadcast.png");
//            media.put("RadioURI", "mms://live.cri.cn/country");
//            media.put("CurrentContent", "时政要闻");//当前节目
//            rl.add(media);
//            media=new HashMap<String, Object>();
//            media.put("MediaType", "RES"); //文件资源
//            media.put("ResType", "mp3");
//            media.put("ResClass", "脱口秀");
//            media.put("ResStyle", "文化");
//            media.put("ResSeries", "逻辑思维");
//            media.put("ResActor", "罗某某");
//            media.put("ResName", "逻辑思维001");
//            media.put("ResImg", "images/dft_actor.png");
//            media.put("ResURI", "http://www.woting.fm/resource/124osdf3.mp3");
//            media.put("ResTime", "4:35");
//            rl.add(media);
//            media=new HashMap<String, Object>();
//            media.put("MediaType", "RADIO"); //电台
//            media.put("RadioName", "CRI肯尼亚调频");
//            media.put("RadioId", "002");
//            media.put("RadioImg", "images/dft_broadcast.png");
//            media.put("RadioURI", "mms://livexwb.cri.com.cn/kenya");
//            media.put("CurrentContent", "经典回顾");//当前节目
//            rl.add(media);
//            map.put("ReturnType", "1001");
//            map.put("ResultList", rl);
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