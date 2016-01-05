package com.woting.appengine.common.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spiritdata.framework.util.DateUtils;
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.model.MobileParam;
import com.woting.appengine.mobile.session.mem.SessionMemoryManage;
import com.woting.appengine.mobile.session.model.MobileSession;
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.woting.passport.UGA.service.UserService;
import com.woting.passport.login.persistence.pojo.MobileUsedPo;
import com.woting.passport.login.service.MobileUsedService;

@Controller
public class CommonController {
    @Resource
    private UserService userService;
    @Resource
    private MobileUsedService muService;

    private SessionMemoryManage smm=SessionMemoryManage.getInstance();

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
            }
            MobileParam mp=MobileUtils.getMobileParam(m);
            MobileKey sk=(mp==null?null:mp.getMobileKey());
            if (sk==null) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取设备Id(IMEI)");
                return map;
            }
            smm.expireAllSessionByIMEI(sk.getMobileId()); //作废所有imei对应的Session
            //2-处理
            map.put("SessionId", sk.getSessionId());
            MobileUsedPo mu = muService.getUsedInfo(sk.getMobileId());
            MobileSession ms=null;
            if (mu==null||mu.getStatus()==2) {//上次未登陆
                boolean canCreateSession=false;
                if (mu==null) canCreateSession=true;
                else { //上次是注销
                    ms=smm.getSession(sk); //看看Session中是否有
                    if (ms==null) canCreateSession=true;
                }
                if (canCreateSession) {
                    ms=new MobileSession(sk);
                    smm.addOneSession(ms);
                } else if (ms!=null) {
                    ms.access();
                    ms.clearBody();
                }
                map.put("ReturnType", "1002");
            } else { //上次是登录
                ms=smm.getUserSession(mu.getUserId(), sk.getMobileId());
                if (ms==null) { //找不到对应的移动会话
                    ms=new MobileSession(sk);
                    smm.addOneSession(ms);
                } else { //找到了对应的对话，直接应用
                    ms.access();
                }
                UserPo u = userService.getUserById(mu.getUserId());
                ms.addAttribute("user", u);
                map.put("ReturnType", "1001"); //已登录
                map.put("UserInfo", u.toHashMap4Mobile());
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
    public Map<String,Object> getVersion(HttpServletRequest request) {
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
            }
            //1-获取版本,逗号分开的序列，前面是版本号，后面是发布日期
            String _temp=MobileUtils.getLastVersion();
            if (_temp==null) {
                map.put("Version", "内部版本0.0.x");
                map.put("PublishDate", "2015-09-18");
            } else {
                String[] vs = _temp.split(",");
                map.put("Version", vs[0]);
                if (vs.length>1) {
                    map.put("PublishDate", vs[1]);
                } else {
                    map.put("PublishDate", DateUtils.convert2LocalStr("yyyy-MM-dd", new Date()));
                }
                if (vs.length>2) {
                    map.put("PatchInfo", vs[2]);
                }
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
            List<Map<String, Object>> zl = new ArrayList<Map<String, Object>>();
            Map<String, Object> zone;
            zone = new HashMap<String, Object>();
            zone.put("ZoneId", "001");
            zone.put("ZoneName", "北京");
            zl.add(zone);
            zone = new HashMap<String, Object>();
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
                //获得SessionId
            }
            //1-获取地区信息
            List<Map<String, Object>> ml = new ArrayList<Map<String, Object>>();
            Map<String, Object> media;
            media = new HashMap<String, Object>();
            media.put("MediaType", "RADIO"); //电台
            media.put("RadioName", "CRI 轻松调频");
            media.put("RadioId", "001");
            media.put("RadioImg", "images/dft_broadcast.png");
            media.put("RadioURI", "mms://live.cri.cn/en4");
            media.put("CurrentContent", "路况信息");//当前节目
            ml.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RES"); //文件资源
            media.put("ResType", "mp3");
            media.put("ResClass", "评书");
            media.put("ResStyle", "文学名著");
            media.put("ResActor", "张三");
            media.put("ResName", "三打白骨精");
            media.put("ResImg", "images/dft_res.png");
            media.put("ResURI", "http://www.woting.fm/resource/124osdf3.mp3");
            media.put("ResTime", "14:35");
            ml.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RES"); //文件资源
            media.put("ResType", "mp3");
            media.put("ResClass", "歌曲");
            media.put("ResStyle", "摇滚");
            media.put("ResActor", "李四");
            media.put("ResName", "歌曲名称");
            media.put("ResImg", "images/dft_actor.png");
            media.put("ResURI", "http://www.woting.fm/resource/124osdf3.mp3");
            media.put("ResTime", "4:35");
            ml.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RADIO"); //电台
            media.put("RadioName", "CRI怀旧金曲音乐频道");
            media.put("RadioId", "002");
            media.put("RadioImg", "images/dft_broadcast.png");
            media.put("RadioURI", "mms://a.b.c/aaa.mpg");
            media.put("CurrentContent", "经典回顾");//当前节目
            ml.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RADIO"); //电台
            media.put("RadioName", "央广新闻");
            media.put("RadioId", "003");
            media.put("RadioImg", "images/dft_broadcast.png");
            media.put("RadioURI", "mms://live.cri.cn/oldies");
            media.put("CurrentContent", "时政要闻");//当前节目
            ml.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RES"); //文件资源
            media.put("ResType", "mp3");
            media.put("ResClass", "脱口秀");
            media.put("ResStyle", "文化");
            media.put("ResSeries", "逻辑思维");
            media.put("ResActor", "罗某某");
            media.put("ResName", "逻辑思维001");
            media.put("ResImg", "images/dft_actor.png");
            media.put("ResURI", "http://www.woting.fm/resource/124osdf3.mp3");
            media.put("ResTime", "4:35");
            ml.add(media);
            map.put("ReturnType", "1001");
            map.put("MainList", ml);
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
                //获得语音转文字的结果
            }
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
                //获得语音转文字的结果
            }
            map.put("ReturnType", "1001");
            List<Map<String, String>> demoData = new ArrayList<Map<String, String>>();
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
                //获得语音转文字的结果
            }
            //1-获取地区信息
            List<Map<String, Object>> rl = new ArrayList<Map<String, Object>>();
            Map<String, Object> media;
            media = new HashMap<String, Object>();
            media.put("MediaType", "RES"); //文件资源
            media.put("ResType", "mp3");
            media.put("ResClass", "评书");
            media.put("ResStyle", "文学名著");
            media.put("ResActor", "张三");
            media.put("ResName", "三打白骨精");
            media.put("ResImg", "images/dft_res.png");
            media.put("ResURI", "http://www.woting.fm/resource/124osdf3.mp3");
            media.put("ResTime", "14:35");
            rl.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RADIO"); //电台
            media.put("RadioName", "CRI英语漫听电台");
            media.put("RadioId", "001");
            media.put("RadioImg", "images/dft_broadcast.png");
            media.put("RadioURI", "mms://live.cri.cn/english");
            media.put("CurrentContent", "路况信息");//当前节目
            rl.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RES"); //文件资源
            media.put("ResType", "mp3");
            media.put("ResClass", "歌曲");
            media.put("ResStyle", "摇滚");
            media.put("ResActor", "李四");
            media.put("ResName", "歌曲名称");
            media.put("ResImg", "images/dft_actor.png");
            media.put("ResURI", "http://www.woting.fm/resource/124osdf3.mp3");
            media.put("ResTime", "4:35");
            rl.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RADIO"); //电台
            media.put("RadioName", "CRI乡村民谣音乐");
            media.put("RadioId", "003");
            media.put("RadioImg", "images/dft_broadcast.png");
            media.put("RadioURI", "mms://live.cri.cn/country");
            media.put("CurrentContent", "时政要闻");//当前节目
            rl.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RES"); //文件资源
            media.put("ResType", "mp3");
            media.put("ResClass", "脱口秀");
            media.put("ResStyle", "文化");
            media.put("ResSeries", "逻辑思维");
            media.put("ResActor", "罗某某");
            media.put("ResName", "逻辑思维001");
            media.put("ResImg", "images/dft_actor.png");
            media.put("ResURI", "http://www.woting.fm/resource/124osdf3.mp3");
            media.put("ResTime", "4:35");
            rl.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RADIO"); //电台
            media.put("RadioName", "CRI肯尼亚调频");
            media.put("RadioId", "002");
            media.put("RadioImg", "images/dft_broadcast.png");
            media.put("RadioURI", "mms://livexwb.cri.com.cn/kenya");
            media.put("CurrentContent", "经典回顾");//当前节目
            rl.add(media);
            map.put("ReturnType", "1001");
            map.put("ResultList", rl);
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
                //获得语音转文字的结果
            }
            //1-获取地区信息
            List<Map<String, Object>> rl = new ArrayList<Map<String, Object>>();
            Map<String, Object> media;
            media = new HashMap<String, Object>();
            media.put("MediaType", "RES"); //文件资源
            media.put("ResType", "mp3");
            media.put("ResClass", "评书");
            media.put("ResStyle", "文学名著");
            media.put("ResActor", "张三");
            media.put("ResName", "三打白骨精");
            media.put("ResImg", "images/dft_res.png");
            media.put("ResURI", "http://www.woting.fm/resource/124osdf3.mp3");
            media.put("ResTime", "14:35");
            rl.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RADIO"); //电台
            media.put("RadioName", "CRI英语漫听电台");
            media.put("RadioId", "001");
            media.put("RadioImg", "images/dft_broadcast.png");
            media.put("RadioURI", "mms://live.cri.cn/english");
            media.put("CurrentContent", "路况信息");//当前节目
            rl.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RES"); //文件资源
            media.put("ResType", "mp3");
            media.put("ResClass", "歌曲");
            media.put("ResStyle", "摇滚");
            media.put("ResActor", "李四");
            media.put("ResName", "歌曲名称");
            media.put("ResImg", "images/dft_actor.png");
            media.put("ResURI", "http://www.woting.fm/resource/124osdf3.mp3");
            media.put("ResTime", "4:35");
            rl.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RADIO"); //电台
            media.put("RadioName", "CRI乡村民谣音乐");
            media.put("RadioId", "003");
            media.put("RadioImg", "images/dft_broadcast.png");
            media.put("RadioURI", "mms://live.cri.cn/country");
            media.put("CurrentContent", "时政要闻");//当前节目
            rl.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RES"); //文件资源
            media.put("ResType", "mp3");
            media.put("ResClass", "脱口秀");
            media.put("ResStyle", "文化");
            media.put("ResSeries", "逻辑思维");
            media.put("ResActor", "罗某某");
            media.put("ResName", "逻辑思维001");
            media.put("ResImg", "images/dft_actor.png");
            media.put("ResURI", "http://www.woting.fm/resource/124osdf3.mp3");
            media.put("ResTime", "4:35");
            rl.add(media);
            media = new HashMap<String, Object>();
            media.put("MediaType", "RADIO"); //电台
            media.put("RadioName", "CRI肯尼亚调频");
            media.put("RadioId", "002");
            media.put("RadioImg", "images/dft_broadcast.png");
            media.put("RadioURI", "mms://livexwb.cri.com.cn/kenya");
            media.put("CurrentContent", "经典回顾");//当前节目
            rl.add(media);
            map.put("ReturnType", "1001");
            map.put("ResultList", rl);
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
     * 保存图片
     */
    @RequestMapping(value="/common/uploadImg.do")
    @ResponseBody
    public Map<String,Object> uploadImg(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=MobileUtils.getDataFromRequestParam(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
                return map;
            }
            MobileParam mp=MobileUtils.getMobileParam(m);
            MobileKey sk=(mp==null?null:mp.getMobileKey());
            //1-得到用户id
            String userId=(String)m.get("UserId");
            UserPo u = null;
            if (sk!=null) {
                map.put("SessionId", sk.getSessionId());
                MobileSession ms=smm.getSession(sk);
                if (ms!=null) {
                    if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                        userId=sk.getSessionId();
                        if (userId.length()!=12) {
                            userId=null;
                            u = (UserPo)ms.getAttribute("user");
                            if (u!=null) userId = u.getUserId();
                        }
                    }
                    ms.access();
                } else {
                    ms=new MobileSession(sk);
                    smm.addOneSession(ms);
                }
            }
            if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获取用户Id，不能保存图片");
            } else {
                request.getQueryString();
                Map<String, Object> sfMap=MobileUtils.saveTypePictrue(request, userId);
                if (sfMap.get("rType").equals("ok")){//成功
                    //数据库处理：这里保存的是
                    if (u==null) u=userService.getUserById(userId);
                    if (u!=null) {
                        u.setProtraitBig((String)sfMap.get("bigUri"));
                        u.setProtraitMini((String)sfMap.get("miniUri"));
                        userService.updateUser(u);
                    }
                    map.put("ReturnType", "1001");
                    map.put("BigUri", (String)sfMap.get("bigUri"));
                    map.put("MiniUri", (String)sfMap.get("miniUri"));
                } else {
                    map.put("ReturnType", "1003");
                    map.put("Message", sfMap.get("Message"));
                }
            }
            return map;
        } catch (Exception e) {
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }
}