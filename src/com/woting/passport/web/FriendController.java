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
import com.woting.appengine.common.util.MobileUtils;
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

    /**
     * 得到陌生人列表
     */
    @RequestMapping(value="searchStranger.do")
    @ResponseBody
    public Map<String,Object> searchStranger(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
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
                } else if ((retM.get("ReturnType")+"").equals("2003")) {
                    map.put("ReturnType", "200");
                    map.put("Message", "需要登录");
                } else {
                    ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                    if (ms.getKey().isUser()) userId=ms.getKey().getUserId();
                }
                if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "无法获取用户Id");
                }
            }
            if (map.get("ReturnType")!=null) return map;


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
            e.printStackTrace();
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
            String userId="";
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
                } else if ((retM.get("ReturnType")+"").equals("2003")) {
                    map.put("ReturnType", "200");
                    map.put("Message", "需要登录");
                } else {
                    ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                    if (ms.getKey().isUser()) userId=ms.getKey().getUserId();
                }
                if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "无法获取用户Id");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-获取被邀请人Id
            String beInviteUserId=(String)m.get("BeInviteUserId");
            if (StringUtils.isNullOrEmptyOrSpace(beInviteUserId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "被邀请人Id为空");
                return map;
            } else {
                UserPo u=userService.getUserById(beInviteUserId);
                if (u==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户Id为["+beInviteUserId+"]的被邀请用户");
                    return map;
                }
            }
            String inviteMsg=(String)m.get("InviteMsg");
            map.putAll(friendService.inviteFriend(userId, beInviteUserId, inviteMsg));
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
     * 得到邀请我列表
     */
    @RequestMapping(value="getInvitedMeList.do")
    @ResponseBody
    public Map<String,Object> getInvitedMeList(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
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
                } else if ((retM.get("ReturnType")+"").equals("2003")) {
                    map.put("ReturnType", "200");
                    map.put("Message", "需要登录");
                } else {
                    ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                    if (ms.getKey().isUser()) userId=ms.getKey().getUserId();
                }
                if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "无法获取用户Id");
                }
            }
            if (map.get("ReturnType")!=null) return map;

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
     * 处理邀请
     */
    @RequestMapping(value="inviteDeal.do")
    @ResponseBody
    public Map<String,Object> inviteDeal(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
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
                } else if ((retM.get("ReturnType")+"").equals("2003")) {
                    map.put("ReturnType", "200");
                    map.put("Message", "需要登录");
                } else {
                    ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                    if (ms.getKey().isUser()) userId=ms.getKey().getUserId();
                }
                if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "无法获取用户Id");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-邀请人id
            String inviteUserId=(String)m.get("InviteUserId");
            if (StringUtils.isNullOrEmptyOrSpace(inviteUserId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "邀请人Id为空");
                return map;
            } else {
                UserPo u=userService.getUserById(inviteUserId);
                if (u==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户Id为["+inviteUserId+"]的邀请人");
                    return map;
                }
            }
            //3-获得处理类型
            String dealType=(String)m.get("DealType");
            if (StringUtils.isNullOrEmptyOrSpace(dealType)) {
                map.put("ReturnType", "1004");
                map.put("Message", "没有处理类型dealType，无法处理");
                return map;
            }
            //4-获得拒绝理由
            String refuseMsg=(String)m.get("RefuseMsg");
            //4-邀请处理
            map.putAll(friendService.deal(userId, inviteUserId, dealType.equals("2"), refuseMsg));
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
     * 删除好友
     */
    @RequestMapping(value="delFriend.do")
    @ResponseBody
    public Map<String,Object> delFriend(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
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
                } else if ((retM.get("ReturnType")+"").equals("2003")) {
                    map.put("ReturnType", "200");
                    map.put("Message", "需要登录");
                } else {
                    ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                    if (ms.getKey().isUser()) userId=ms.getKey().getUserId();
                }
                if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "无法获取用户Id");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-好友Id
            String friendUserId=(String)m.get("FriendUserId");
            if (StringUtils.isNullOrEmptyOrSpace(friendUserId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "好友Id为空");
                return map;
            } else {
                UserPo u=userService.getUserById(friendUserId);
                if (u==null) {
                    map.put("ReturnType", "1004");
                    map.put("Message", "好友不存在");
                    return map;
                }
            }

            //4-邀请处理
            map.putAll(friendService.del(userId, friendUserId));
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
     * 得到好友列表
     */
    @RequestMapping(value="getList.do")
    @ResponseBody
    public Map<String,Object> getFriendList(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
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
                } else if ((retM.get("ReturnType")+"").equals("2003")) {
                    map.put("ReturnType", "200");
                    map.put("Message", "需要登录");
                } else {
                    ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                    if (ms.getKey().isUser()) userId=ms.getKey().getUserId();
                }
                if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "无法获取用户Id");
                }
            }
            if (map.get("ReturnType")!=null) return map;

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
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    /**
     * 修改好友信息
     */
    @RequestMapping(value="updateFriendAlias.do")
    @ResponseBody
    public Map<String,Object> updateFriendAlias(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
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
                } else if ((retM.get("ReturnType")+"").equals("2003")) {
                    map.put("ReturnType", "200");
                    map.put("Message", "需要登录");
                } else {
                    ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                    if (ms.getKey().isUser()) userId=ms.getKey().getUserId();
                }
                if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "无法获取用户Id");
                }
            }
            if (map.get("ReturnType")!=null) return map;

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
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }
}