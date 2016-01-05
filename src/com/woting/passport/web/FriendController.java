package com.woting.passport.web;

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
import com.woting.appengine.mobile.MobileUtils;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.model.MobileParam;
import com.woting.appengine.mobile.session.mem.SessionMemoryManage;
import com.woting.appengine.mobile.session.model.MobileSession;
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.woting.passport.UGA.service.UserService;
import com.woting.passport.friend.service.FriendService;

@Controller
@RequestMapping(value="/passport/friend/")
public class FriendController {
    @Resource
    private FriendService friendService;
    @Resource
    private UserService userService;

    private SessionMemoryManage smm=SessionMemoryManage.getInstance();

    /**
     * 得到好友列表
     */
    @RequestMapping(value="searchStranger.do")
    @ResponseBody
    public Map<String,Object> searchStranger(HttpServletRequest request) {
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
            //1-获取UserId，并处理访问
            String userId=sk.isUser()?sk.getUserId():null;
            if (sk!=null) {
                map.put("SessionId", sk.getSessionId());
                MobileSession ms=smm.getSession(sk);
                if (ms==null) {
                    ms=new MobileSession(sk);
                    smm.addOneSession(ms);
                } else {
                    ms.access();
                    if (userId==null) {
                        UserPo u=(UserPo)ms.getAttribute("user");
                        if (u!=null) userId=u.getUserId();
                    }
                }
            }
            if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获取用户Id");
                return map;
            }
            //2-获取搜索条件
            String searchStr=(String)m.get("SearchStr");
            if (StringUtils.isNullOrEmptyOrSpace(searchStr)) {
                map.put("ReturnType", "1003");
                map.put("Message", "搜索条件不能为空");
                return map;
            }
            try {
                List<UserPo> ul=friendService.getStrangers(userId, searchStr);
                if (ul!=null&&ul.size()>0) {
                    List<Map<String, Object>> rul=new ArrayList<Map<String, Object>>();
                    for (UserPo u: ul) {
                        if (!u.getUserId().equals(userId)) rul.add(u.toHashMap4Mobile());
                    }
                    map.put("ReturnType", "1001");
                    map.put("UserList", rul);
                } else {
                    map.put("ReturnType", "1011");
                    map.put("Message", "没有陌生人");
                }
            } catch (Exception ei) {
                map.put("ReturnType", "1004");
                map.put("Message", "获得陌生人列表失败："+ei.getMessage());
            }
            return map;
        } catch(Exception e) {
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    /**
     * 邀请陌生人为好友
     */
    @RequestMapping(value="invite.do")
    @ResponseBody
    public Map<String,Object> invite(HttpServletRequest request) {
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
            //1-获取UserId，并处理访问
            String userId=sk.isUser()?sk.getUserId():null;
            if (sk!=null) {
                map.put("SessionId", sk.getSessionId());
                MobileSession ms=smm.getSession(sk);
                if (ms==null) {
                    ms=new MobileSession(sk);
                    smm.addOneSession(ms);
                } else {
                    ms.access();
                    if (userId==null) {
                        UserPo u=(UserPo)ms.getAttribute("user");
                        if (u!=null) userId=u.getUserId();
                    }
                }
            }
            if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获取用户Id");
                return map;
            }
            //2-获取被邀请人Id
            UserPo u=null;
            String inviteUserId=(String)m.get("InviteUserId");
            if (StringUtils.isNullOrEmptyOrSpace(inviteUserId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "被邀请人Id为空");
                return map;
            } else {
                u=userService.getUserById(inviteUserId);
                if (u==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户Id为["+inviteUserId+"]的被邀请用户");
                    return map;
                }
            }
            String inviteMsg=(String)m.get("InviteMsg");
            try {
                map.putAll(friendService.inviteFriend(userId, inviteUserId, inviteMsg));
            } catch(Exception ei) {
                map.put("ReturnType", "1004");
                map.put("Message", "邀请失败："+ei.getMessage());
                ei.printStackTrace();
            }
            return map;
        } catch(Exception e) {
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    /**
     * 得到邀请我列表
     */
    @RequestMapping(value="getInvitedMeList.do")
    @ResponseBody
    public Map<String,Object> getInvitedMeList(HttpServletRequest request) {
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
            //1-获取UserId，并处理访问
            String userId=sk.isUser()?sk.getUserId():null;
            if (sk!=null) {
                map.put("SessionId", sk.getSessionId());
                MobileSession ms=smm.getSession(sk);
                if (ms==null) {
                    ms=new MobileSession(sk);
                    smm.addOneSession(ms);
                } else {
                    ms.access();
                    if (userId==null) {
                        UserPo u=(UserPo)ms.getAttribute("user");
                        if (u!=null) userId=u.getUserId();
                    }
                }
            }
            if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获取用户Id");
                return map;
            }
            try {
                List<Map<String, Object>> ul=friendService.getInvitedMeList(userId);
                if (ul!=null&&ul.size()>0) {
                    List<Map<String, Object>> rul=new ArrayList<Map<String, Object>>();
                    Map<String, Object> um;
                    for (Map<String, Object> u: ul) {
                        um=new HashMap<String, Object>();
                        um.put("UserId", u.get("id"));
                        um.put("UserName", u.get("loginName"));
                        um.put("InviteMesage", u.get("inviteMessage"));
                        um.put("Portrait", u.get("protraitMini"));
                        rul.add(um);
                    }
                    map.put("ReturnType", "1001");
                    map.put("UserList", rul);
                } else {
                    map.put("ReturnType", "1011");
                    map.put("Message", "邀请我的信息都已处理");
                }
            } catch(Exception ei) {
                map.put("ReturnType", "1003");
                map.put("Message", ei.getMessage());
            }
            return map;
        } catch(Exception e) {
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    /**
     * 处理邀请
     */
    @RequestMapping(value="inviteDeal.do")
    @ResponseBody
    public Map<String,Object> inviteDeal(HttpServletRequest request) {
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
            //1-获取UserId，并处理访问
            String userId=sk.isUser()?sk.getUserId():null;
            if (sk!=null) {
                map.put("SessionId", sk.getSessionId());
                MobileSession ms=smm.getSession(sk);
                if (ms==null) {
                    ms=new MobileSession(sk);
                    smm.addOneSession(ms);
                } else {
                    ms.access();
                    if (userId==null) {
                        UserPo u=(UserPo)ms.getAttribute("user");
                        if (u!=null) userId=u.getUserId();
                    }
                }
            }
            if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获取用户Id");
                return map;
            }
            //2-获得处理类型
            String dealType=(String)m.get("DealType");
            if (StringUtils.isNullOrEmptyOrSpace(dealType)) {
                map.put("ReturnType", "1002");
                map.put("Message", "没有处理类型dealType，无法处理");
                return map;
            }
            //3-邀请人id
            String inviteUserId=(String)m.get("InviteUserId");
            UserPo u=null;
            if (StringUtils.isNullOrEmptyOrSpace(inviteUserId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "邀请人Id为空");
                return map;
            } else {
                u=userService.getUserById(inviteUserId);
                if (u==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户Id为["+inviteUserId+"]的邀请用户");
                    return map;
                }
            }
            
            //4-获得拒绝理由
            String refuseMsg=(String)m.get("RefuseMsg");
            //4-邀请处理
            try {
                map.putAll(friendService.deal(userId, inviteUserId, dealType.equals("2"), refuseMsg));
            } catch(Exception ei) {
                map.put("ReturnType", "1003");
                map.put("Message", ei.getMessage());
            }
            return map;
        } catch(Exception e) {
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

   /**
     * 得到好友列表
     */
    @RequestMapping(value="getList.do")
    @ResponseBody
    public Map<String,Object> getFriendList(HttpServletRequest request) {
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
            //1-获取UserId，并处理访问
            String userId=sk.isUser()?sk.getUserId():null;
            if (sk!=null) {
                map.put("SessionId", sk.getSessionId());
                MobileSession ms=smm.getSession(sk);
                if (ms==null) {
                    ms=new MobileSession(sk);
                    smm.addOneSession(ms);
                } else {
                    ms.access();
                    if (userId==null) {
                        UserPo u=(UserPo)ms.getAttribute("user");
                        if (u!=null) userId=u.getUserId();
                    }
                }
            }
            if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获取用户Id");
                return map;
            }
            List<UserPo> ul=friendService.getFriendList(userId);
            if (ul!=null&&ul.size()>0) {
                List<Map<String, Object>> rul=new ArrayList<Map<String, Object>>();
                for (UserPo u: ul) {
                    if (!u.getUserId().equals(userId)) rul.add(u.toHashMap4Mobile());
                }
                map.put("ReturnType", "1001");
                map.put("UserList", rul);
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "没有好友");
            }
            return map;
        } catch(Exception e) {
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }
}