package com.woting.appengine.common.util;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.model.MobileParam;
import com.woting.push.core.message.Message;
import com.woting.push.core.message.MsgNormal;
import com.woting.appengine.mobile.session.mem.SessionMemoryManage;
import com.woting.appengine.mobile.session.model.MobileSession;
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.woting.passport.UGA.service.UserService;
import com.woting.passport.login.persistence.pojo.MobileUsedPo;
import com.woting.passport.login.service.MobileUsedService;

/**
 * 移动的公共处理
 * @author wh
 */
public abstract class MobileUtils {
    public static MobileParam getMobileParam(Map<String, Object> m) {
        if (m==null||m.size()==0) return null;

        MobileParam mp=new MobileParam();

        Object o=m.get("IMEI");
        String __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setImei(__tmp);
        o=m.get("PCDType");
        __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setPCDType(__tmp);
        o=m.get("MobileClass");
        o=m.get("GPS-longitude");
        __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setGpsLongitude(__tmp);
        o=m.get("GPS-Latitude");
        __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setGpsLatitude(__tmp);
        o=m.get("ScreenSize");
        __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setScreenSize(__tmp);
        o=m.get("SessionId");
        __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setSessionId(__tmp);
        o=m.get("UserId");
        __tmp=o==null?"":o+"";
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setUserId(__tmp);

        if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getGpsLongitude())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getGpsLatitude())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getScreenSize())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getSessionId())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getUserId())) {
            return null;
        } else {
            return mp;
        }
    }

    /*
     * 判断UserId是否合法
     */
    public static boolean isValidUserId(String userId) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return false;
        return userId.length()==12;
    }

    /**
     * 从一个Map获得移动的Key
     * @param m
     * @return
     */
    public static MobileKey getMobileKey(Map<String, Object> m) {
        if (m==null||m.size()==0) return null;

        Object o=m.get("IMEI");
        String __tmp=o==null?"":o+"";
        if (StringUtils.isNullOrEmptyOrSpace(__tmp)) return null;

        MobileKey ret=new MobileKey();
        ret.setMobileId(__tmp);
        o=m.get("PCDType");
        __tmp=o==null?"1":o+"";
        try {ret.setPCDType(Integer.parseInt(__tmp));} catch(Exception e) {}
        o=m.get("UserId");
        __tmp=o==null?"":o+"";
        if ("".equals(__tmp)) {
            o=m.get("SessionId");
            __tmp=o==null?"":o+"";
        }
        ret.setUserId(__tmp);
        return ret;
    }

    /**
     * 从一个Message获得key
     * @param m
     * @return
     */
    public static MobileKey getMobileKey(Message m) {
        if (m==null) return null;

        if (!(m instanceof MsgNormal)) return null;

        MsgNormal mn=(MsgNormal)m;
        if (StringUtils.isNullOrEmptyOrSpace(mn.getIMEI())) return null;

        MobileKey ret=new MobileKey();
        ret.setMobileId(mn.getIMEI());
        ret.setPCDType(mn.getPCDType());
        ret.setUserId(mn.getUserId());

        return ret;
    }

    /**
     * 根据MobileKey生成地址
     * @param mk 
     * @return 地址字符串
     */
    public static String getAddr(MobileKey mk) {
        String ret="";
        if (mk.isMobile()) {
            ret="{()@@("+mk.getMobileId()+"::"+mk.getPCDType()+"||M)}";
        } else {
            ret="{("+mk.getUserId()+"||wt)@@("+mk.getMobileId()+"::"+mk.getPCDType()+"||M)}";
        }
        return ret;
    }

    /**
     * 从Message的地址，获得MobileKey
     * @param m 消息数据
     * @param type =1发送地址FromAddr;=2(!=1)接收地址ToAddr
     * @return 若合规，返回正常的MobileKey，否则返回空
     */
    //还有问题，没有做全部的解析，先这样
//    public static MobileKey getMobileKey(Message m, int type) {
//        if (m==null) return null;
//        if (type==1&&StringUtils.isNullOrEmptyOrSpace(m.getFromAddr())) return null;
//        if (type!=1&&StringUtils.isNullOrEmptyOrSpace(m.getToAddr())) return null;
//
//        String _temp, _temp2;
//        _temp=type==1?m.getFromAddr():m.getToAddr();
//        if (_temp.charAt(0)!='{'||_temp.charAt(_temp.length()-1)!='}') return null;
//        _temp=_temp.substring(1, _temp.length()-1);
//        String[] ss=_temp.split("@@");
//        _temp=ss[0]; _temp2=ss[1];
//        MobileKey mk=new MobileKey();
//        //获得IMEI
//        if (_temp2.charAt(0)=='('&&_temp2.charAt(_temp2.length()-1)==')') {
//            _temp2=_temp2.substring(1, _temp2.length()-1);
//            ss=_temp2.split("\\u007C\\u007C");
//            if (ss.length==1) return null;
//            _temp2=ss[0];
//            String[] ss2=_temp2.split("::");
//            mk.setMobileId(ss2[0]);
//            mk.setPCDType(1);
//            if (ss2.length==2) {
//                try {mk.setPCDType(Integer.parseInt(ss2[1]));} catch(Exception e) {};
//            }
//        } else return null;
//        //获得userId
//        if (_temp.charAt(0)=='('&&_temp.charAt(_temp.length()-1)==')') {
//            _temp=_temp.substring(1, _temp.length()-1);
//            ss=_temp.split("\\u007C\\u007C");
//            if (ss.length==1) {
//                mk.setUserId(mk.getMobileId());
//            } else {
//                _temp=ss[0];
//                mk.setUserId(_temp);
//            }
//        } else mk.setUserId(mk.getMobileId());
//        return mk;
//    }

    /**
     * 为消息服务，处理客户端的连接
     * @param m 消息
     * @param type 类型：1=注册，第一次进入；2=仅绑定
     * @return 返回Map，情况如下：1=若参数整体无意义，则返回空；
     *   ReturnType="2001", Msg="未获得IMEI无法处理";
     */
    public static Map<String, Object> dealMobileLinked(Message m, int type) {
        //若参数整体无意义，则返回空
        if (m==null) return null;
        return MobileUtils._dealMobileLinked(MobileUtils.getMobileKey(m), type);
    }

    /**
     * 为Web服务，处理客户端的连接
     * @param m Web服务收到的客户端所传过来的数据的Map
     * @param type 类型：1=注册，第一次进入；2=仅绑定
     * @return 返回Map，情况如下：1=若参数整体无意义，则返回空
     *   ReturnType="2001", Msg="未获得IMEI无法处理";
     */
    public static Map<String, Object> dealMobileLinked(Map<String, Object> m, int type) {
        if (m==null||m.size()==0) return null;
        return MobileUtils._dealMobileLinked(MobileUtils.getMobileKey(m), type);
    }

    /**
     * 为Web服务，处理客户端的连接
     * @param m Web服务收到的客户端所传过来的数据的Map
     * @param type 类型：1=注册，第一次进入；2=仅绑定
     * @return 返回Map，情况如下：1=若参数整体无意义，则返回空
     *   ReturnType="2001", Msg="未获得IMEI无法处理";
     */
    public static Map<String, Object> dealMobileLinked(MsgNormal mn, int type) {
        if (mn==null) return null;
        return MobileUtils._dealMobileLinked(MobileUtils.getMobileKey(mn), type);
    }
    
    private static Map<String, Object> _dealMobileLinked(MobileKey mKey, int type) {
        if (mKey==null) return null;
        MobileKey _mKey=new MobileKey();
        _mKey.setMobileId(mKey.getMobileId());
        _mKey.setPCDType(mKey.getPCDType());
        _mKey.setUserId(mKey.getUserId());
        Map<String,Object> map=new HashMap<String, Object>();
        if (_mKey==null||StringUtils.isNullOrEmptyOrSpace(_mKey.getMobileId())) {//若无IMEI
            map.put("ReturnType", "2001");
            map.put("Msg", "未获得IMEI无法处理");
            return map;
        }

        if (type==1) {
            try {
                ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
                if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                    MobileUsedService muService=(MobileUsedService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("mobileUsedService");
                    MobileUsedPo mu=muService.getUsedInfo(_mKey.getMobileId(), _mKey.getPCDType());
                    if (mu!=null&&mu.getStatus()==1) {//上次登录
                        _mKey.setUserId(mu.getUserId());//修改mKey
                    } //未登录
                }
            } catch(Exception e) {
            }
        }

        boolean msExist=true;//缓存是否存在
        MobileSession ms=SessionMemoryManage.getInstance().getSession(_mKey);
        if (ms==null) {
            msExist=false;
            ms=new MobileSession(_mKey);
        }
        boolean needLogin=false;
        UserPo u=null;
        if (_mKey.isUser()) {
            try {
                u=(UserPo)ms.getAttribute("user");
            } catch(Exception e) {}
            if (type==1&&(u==null||!_mKey.getUserId().equals(u.getUserId()))) {//实现自动登录
                try {
                    ServletContext sc=(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent();
                    if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                        UserService userService=(UserService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("userService");
                        u=userService.getUserById(_mKey.getUserId());
                        if (u!=null) ms.addAttribute("user", u);
                    }
                } catch(Exception e) {}
            }
            String userIdInMem=u==null?"":u.getUserId();
            if (!_mKey.getUserId().equals(userIdInMem)) needLogin=true;//需要登录
        }

        //准备返回值
        if (mKey.isMobile()&&_mKey.isUser()) {
            if (u!=null) {
                map.put("ReturnType", "1001");
                map.put("Msg", "成功自动登录");
            } else {
                map.put("ReturnType", "1002");
                map.put("Msg", "不能找到相应的用户");
                ms=SessionMemoryManage.getInstance().getSession(mKey);
                if (ms==null) {
                    msExist=false;
                    ms=new MobileSession(mKey);
                    SessionMemoryManage.getInstance().addOneSession(ms);
                }
            }
        } else if (mKey.isMobile()&&_mKey.isMobile()) {
            map.put("ReturnType", "1001");
            map.put("Msg", "设备成功绑定");
        } else {
            if (u!=null) {
                map.put("ReturnType", "1001");
                map.put("Msg", "成功获得Session");
            } else {
                map.put("ReturnType", "2002");
                map.put("Msg", "不能找到相应的用户");
            }
        }

        if (needLogin) {
            map.put("ReturnType", "2003");
            map.put("Msg", "请先登录");
        }
        if (!msExist) SessionMemoryManage.getInstance().addOneSession(ms);
        ms.access();

        map.put("MobileSession", ms);
        return map;
    }
}