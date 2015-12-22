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
import com.woting.mobile.MobileUtils;
import com.woting.mobile.model.MobileParam;
import com.woting.mobile.session.mem.SessionMemoryManage;
import com.woting.mobile.session.model.MobileSession;
import com.woting.mobile.model.MobileKey;
import com.woting.passport.UGA.persistence.pojo.GroupPo;
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.woting.passport.UGA.service.GroupService;
import com.woting.passport.UGA.service.UserService;

@Controller
@RequestMapping(value="/passport/group/")
public class GroupController {
    @Resource
    private GroupService groupService;
    @Resource
    private UserService userService;

    private SessionMemoryManage smm=SessionMemoryManage.getInstance();

    /**
     * 创建用户组
     */
    @RequestMapping(value="buildGroup.do")
    @ResponseBody
    public Map<String,Object> buildGroup(HttpServletRequest request) {
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
            //1-得到创建者，并处理访问
            String creator=sk.isUserSession()?sk.getUserId():null;
            if (sk!=null) {
                map.put("SessionId", sk.getSessionId());
                MobileSession ms=smm.getSession(sk);
                if (ms==null) {
                    ms=new MobileSession(sk);
                    smm.addOneSession(ms);
                } else {
                    ms.access();
                    if (creator==null) {
                        UserPo u=(UserPo)ms.getAttribute("user");
                        if (u!=null) creator=u.getUserId();
                        
                    }
                }
            }
            if (StringUtils.isNullOrEmptyOrSpace(creator)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法得到创建者");
                return map;
            }

            //创建用户组
            String members=(String)m.get("Members");
            if (StringUtils.isNullOrEmptyOrSpace(members)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法得到组员信息");
            } else {
                members=creator+","+members;
                String[] mArray=members.split(",");
                members="";
                for (int i=0; i<mArray.length;i++) {
                    members+=",'"+mArray[i].trim()+"'";
                }
                List<UserPo> ml=userService.getMembers4BuildGroup(members.substring(1));
                if (ml==null||ml.size()==0) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "给定的组员信息不存在");
                } else if (ml.size()==1) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "只有一个有效成员，无法构建用户组");
                } else {
                    //得到组名
                    String groupName=(String)m.get("GroupName");
                    if (StringUtils.isNullOrEmptyOrSpace(groupName)) {
                        groupName="";
                        for (UserPo u:ml) {
                            groupName+=","+u.getLoginName();
                        }
                        groupName=groupName.substring(1);
                    }
                    //创建组
                    GroupPo g=new GroupPo();
                    g.setCreateUserId(creator);
                    g.setGroupName(groupName);
                    g.setGroupUsers(ml);
                    groupService.insertGroup(g);
                    map.put("ReturnType", "1001");
                    map.put("GroupName", groupName);
                }
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
     * 获得用户组
     */
    @RequestMapping(value="getGroupList.do")
    @ResponseBody
    public Map<String,Object> getGroupList(HttpServletRequest request) {
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
            String userId=sk.isUserSession()?sk.getUserId():null;
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
            //2-得到用户组
            List<GroupPo> gl=groupService.getGroupsByUserId(userId);
            List<Map<String, Object>> rgl=new ArrayList<Map<String, Object>>();
            if (gl!=null&&gl.size()>0) {
                Map<String, Object> gm;
                for (GroupPo g:gl) {
                    gm=new HashMap<String, Object>();
                    gm.put("GroupId", g.getGroupId());
                    gm.put("GroupName", g.getGroupName());
                    gm.put("GroupCount", g.getGroupCount());
                    gm.put("GroupImg", "images/group.png");
                    gm.put("InnerPhoneNum", g.getInnerPhoneNum());
                    gm.put("Descripte", g.getDescn());
                    rgl.add(gm);
                }
                map.put("ReturnType", "1001");
                map.put("GroupList", rgl);
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无所属用户组");
            }
            return map;
        } catch(Exception e) {
            map.put("ReturnType", "T");
            map.put("SessionId", e.getMessage());
            return map;
        }
    }

    /**
     * 获得组成员列表
     */
    @RequestMapping(value="getGroupMembers.do")
    @ResponseBody
    public Map<String,Object> getGroupMembers(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获得参数
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获得需要的参数");
                return map;
            }
            MobileParam mp=MobileUtils.getMobileParam(m);
            MobileKey sk=(mp==null?null:mp.getMobileKey());
            //1-处理Session
            if (sk!=null) {
                map.put("SessionId", sk.getSessionId());
                MobileSession ms=smm.getSession(sk);
                if (ms==null) {
                    ms=new MobileSession(sk);
                    smm.addOneSession(ms);
                } else {
                    ms.access();
                }
            }
            //2-得到用户组Id
            String groupId=(String)m.get("GroupId");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获取组Id");
            } else {
                List<Map<String, Object>> rul=new ArrayList<Map<String, Object>>();
                List<UserPo> ul=groupService.getGroupMembers(groupId);
                if (ul!=null&&ul.size()>0) {
                    Map<String, Object> um;
                    for (UserPo u: ul) {
                        um=new HashMap<String, Object>();
                        um.put("UserId", u.getUserId());
                        um.put("UserName", u.getLoginName());
                        um.put("Portrait", "images/person.png");
                        rul.add(um);
                    }
                    map.put("ReturnType", "1001");
                    map.put("UserList", rul);
                } else {
                    map.put("ReturnType", "1011");
                    map.put("Message", "组中无成员");
                }
            }
            return map;
        } catch(Exception e) {
            map.put("ReturnType", "T");
            map.put("SessionId", e.getMessage());
            return map;
        }
    }

    /**
     * 获得用户组
     */
    @RequestMapping(value="exitGroup.do")
    @ResponseBody
    public Map<String,Object> exitGroup(HttpServletRequest request) {
        return null;
    }
}