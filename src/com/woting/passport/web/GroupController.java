package com.woting.passport.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.spiritdata.framework.util.DateUtils;
import com.spiritdata.framework.util.SpiritRandom;
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.mobile.session.model.MobileSession;
import com.woting.passport.UGA.model.Group;
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

    /**
     * 创建用户组，根据用户组类型创建用户
     */
    @RequestMapping(value="buildGroup.do")
    @ResponseBody
    public Map<String,Object> buildGroup(HttpServletRequest request) {
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
                    map.put("Message", "无法得到创建者");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //创建用户组
            //得到组分类：验证群0；公开群1[原来的号码群]；密码群2
            int _groupType=0;
            try {
                _groupType=Integer.parseInt(m.get("GroupType")+"");
            } catch(Exception e) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法得到组分类");
                return map;
            }
            //若是密码群，得到密码
            String groupPwd=(String)m.get("GroupPwd");
            if (_groupType==2) {
                if (StringUtils.isNullOrEmptyOrSpace(groupPwd)) {
                    map.put("ReturnType", "1004");
                    map.put("Message", "无法得到组密码");
                    return map;
                }
            }
            //是否需要用户组成员
            boolean needMember=(m.get("NeedMember")+"").equals("1");
            String memNames="";
            List<UserPo> ml=null;
            if (needMember) {
                String members=(String)m.get("Members");
                if (StringUtils.isNullOrEmptyOrSpace(members)) {
                    map.put("ReturnType", "1005");
                    map.put("Message", "无法得到组员信息");
                    return map;
                }
                members=userId+","+members;
                String[] mArray=members.split(",");
                members="";
                for (int i=0; i<mArray.length;i++) {
                    members+=",'"+mArray[i].trim()+"'";
                }
                ml=userService.getMembers4BuildGroup(members.substring(1));
                if (ml==null||ml.size()==0) {
                    map.put("ReturnType", "1006");
                    map.put("Message", "给定的组员信息不存在");
                    return map;
                }
                if (ml.size()==1) {
                    map.put("ReturnType", "1007");
                    map.put("Message", "只有一个有效成员，无法构建用户组");
                    return map;
                }
                
                for (UserPo u:ml) {
                    memNames+=","+u.getLoginName();
                }
                memNames=memNames.substring(1);
            }

            //判断是否突破限制
            int c = groupService.getCreateGroupCount(userId);
            if (c>50) {
                map.put("ReturnType", "1008");
                map.put("Message", "您所创建的组已达50个，不能再创建了");
                return map;
            }
            c=groupService.getCreateGroupLimitTimeCount(userId, 20);
            if (c>5) {
                map.put("ReturnType", "1009");
                map.put("Message", "20分钟内创建组不能超过5个");
                return map;
            }
            //获得随机数，作为组号
            int newGroupNum=getNewGroupNumber();
            boolean flag=true;
            while (flag) {
                c=groupService.existGroupNum(newGroupNum+"");
                if (c==0) break;
                newGroupNum=SpiritRandom.getRandom(new Random(), 0, 999999);
            }
            //获得用户组名称
            String groupName=(String)m.get("GroupName");
            if (StringUtils.isNullOrEmptyOrSpace(groupName)) {
                if (_groupType==0) groupName="新建验证群"+(needMember?memNames:newGroupNum);
                if (_groupType==1) groupName="新建公开群"+newGroupNum;
                if (_groupType==2) groupName="新建密码群"+(needMember?memNames:newGroupNum);
            }

            //创建组
            if (ml==null) {
                ml=new ArrayList<UserPo>();
                UserPo u=(UserPo)ms.getAttribute("user");
                ml.add(u);
            }
            Group g=new Group();
            g.setGroupNum(""+newGroupNum);
            g.setGroupName(groupName);
            if (_groupType==2) g.setGroupPwd(groupPwd);
            g.setCreateUserId(userId);
            g.setAdminUserIds(userId);
            g.setGroupType(_groupType);
            g.setUserList(ml);
            g.setGroupType(_groupType);
            groupService.insertGroup(g);
            //组织返回值
            map.put("ReturnType", "1001");
            Map<String, String> groupMap=new HashMap<String, String>();
            groupMap.put("GroupId", g.getGroupId());
            groupMap.put("GroupNum", g.getGroupNum());
            groupMap.put("GroupType", g.getGroupType()+"");
            groupMap.put("GroupName", g.getGroupName());
            groupMap.put("GroupCreator", g.getCreateUserId());
            groupMap.put("GroupManager", g.getAdminUserIds());
            groupMap.put("GroupCount", ml.size()+"");
            groupMap.put("GroupDesc", g.getDescn());
            groupMap.put("GroupImg", g.getGroupImg());
            groupMap.put("CreateTime", DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss", new Date()));
            map.put("GroupInfo", groupMap);
            return map;
        } catch(Exception e) {
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    /**
     * 查找用户组
     */
    @RequestMapping(value="searchGroup.do")
    @ResponseBody
    public Map<String,Object> searchGroup(HttpServletRequest request) {
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
                    map.put("Message", "无法得到创建者");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //获得查询串
            String searchStr=(String)m.get("SearchStr");
            if (StringUtils.isNullOrEmptyOrSpace(searchStr)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法得到查询串");
                return map;
            }
            List<Group> gl=groupService.searchGroup(searchStr);
            if (gl!=null&&gl.size()>0) {
                map.put("ReturnType", "1001");
                List<Map<String, String>> groupList=new ArrayList<Map<String,String>>();
                for (int i=0; i<gl.size(); i++) {
                    Map<String, String> oneGroup= new HashMap<String, String>();
                    Group _g=gl.get(i);
                    oneGroup.put("GroupId", _g.getGroupId());
                    oneGroup.put("GroupNum", _g.getGroupNum());
                    oneGroup.put("GroupType", _g.getGroupType()+"");
                    oneGroup.put("GroupImg", _g.getGroupImg());
                    oneGroup.put("GroupName", _g.getGroupName());
                    oneGroup.put("GroupCreator", _g.getCreateUserId());
                    oneGroup.put("GroupManager", _g.getAdminUserIds());
                    oneGroup.put("GroupCount", _g.getUserList().size()+"");
                    oneGroup.put("GroupDesc", _g.getDescn());
                    oneGroup.put("CreateTime", DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss", new Date(_g.getCTime().getTime())));

                    List<UserPo> ul=_g.getUserList();
                    String userNames="";
                    for (int j=0;j<ul.size(); j++) {
                        userNames+=","+ul.get(j).getLoginName();
                    }
                    oneGroup.put("UserNames", userNames.substring(1));
                    groupList.add(oneGroup);
                }
                map.put("GroupList", groupList);
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无所属用户组");
            }
            return map;
        } catch(Exception e) {
            map.put("ReturnType", "T");
            e.printStackTrace();
            map.put("SessionId", e.getMessage());
            return map;
        }
    }

    /**
     * 更新用户组，注意这个更新不能更新用户组成员，也不能更新成员的组名称，只能更新组本身属性
     */
    @RequestMapping(value="updateGrup.do")
    @ResponseBody
    public Map<String,Object> updateGrup(HttpServletRequest request) {
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
                    map.put("Message", "无法得到创建者");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //获得更新的内容
            String GroupId=(String)m.get("GroupId");
            if (StringUtils.isNullOrEmptyOrSpace(GroupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法得到用户组Id");
                return map;
            }

            return map;
        } catch(Exception e) {
            map.put("ReturnType", "T");
            map.put("SessionId", e.getMessage());
            return map;
        }
    }

    /**
     * 获得我所在的用户组
     */
    @RequestMapping(value="getGroupList.do")
    @ResponseBody
    public Map<String,Object> getGroupList(HttpServletRequest request) {
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
                    map.put("Message", "无法得到创建者");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-得到用户组
            List<GroupPo> gl=groupService.getGroupsByUserId(userId);
            List<Map<String, Object>> rgl=new ArrayList<Map<String, Object>>();
            if (gl!=null&&gl.size()>0) {
                Map<String, Object> gm;
                for (GroupPo g:gl) {
                    gm=new HashMap<String, Object>();
                    gm.put("GroupId", g.getGroupId());
                    gm.put("GroupNum", g.getGroupNum());
                    gm.put("GroupType", g.getGroupType()+"");
                    gm.put("GroupImg", g.getGroupImg());
                    gm.put("GroupName", g.getGroupName());
                    gm.put("GroupCreator", g.getCreateUserId());
                    gm.put("GroupManager", g.getAdminUserIds());
                    gm.put("GroupCount", g.getGroupCount()+"");
                    gm.put("GroupDesc", g.getDescn());
                    gm.put("CreateTime", DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss", new Date(g.getCTime().getTime())));
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
     * 获得我所创建的用户组
     */
    @RequestMapping(value="getCreateGroupList.do")
    @ResponseBody
    public Map<String,Object> getCreateGroupList(HttpServletRequest request) {
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
                    map.put("Message", "无法得到创建者");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-得到用户组
            List<GroupPo> gl=groupService.getCreateGroupsByUserId(userId);
            List<Map<String, Object>> rgl=new ArrayList<Map<String, Object>>();
            if (gl!=null&&gl.size()>0) {
                Map<String, Object> gm;
                for (GroupPo g:gl) {
                    gm=new HashMap<String, Object>();
                    gm.put("GroupId", g.getGroupId());
                    gm.put("GroupNum", g.getGroupNum());
                    gm.put("GroupType", g.getGroupType()+"");
                    gm.put("GroupImg", g.getGroupImg());
                    gm.put("GroupName", g.getGroupName());
                    gm.put("GroupCreator", g.getCreateUserId());
                    gm.put("GroupManager", g.getAdminUserIds());
                    gm.put("GroupCount", g.getGroupCount());
                    gm.put("CreateTime", DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss", new Date(g.getCTime().getTime())));
                    gm.put("GroupDesc", g.getDescn());
                    gm.put("InnerPhoneNum", "3000");
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
                    map.put("Message", "无法得到创建者");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-得到用户组Id
            String groupId=(String)m.get("GroupId");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                List<Map<String, Object>> rul=new ArrayList<Map<String, Object>>();
                List<UserPo> ul=groupService.getGroupMembers(groupId);
                if (ul!=null&&ul.size()>0) {
                    for (UserPo u: ul) {
                        rul.add(u.toHashMap4Mobile());
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
     * 加入公开群
     */
    @RequestMapping(value="joinInGroup.do")
    @ResponseBody
    public Map<String,Object> joininGroup(HttpServletRequest request) {
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
                    map.put("Message", "无法得到创建者");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-判断用户组进入是否符合业务逻辑
            String groupNum=(String)m.get("GroupNum");
            if (StringUtils.isNullOrEmptyOrSpace(groupNum)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取用户组号码");
                return map;
            }
            m.clear();
            m.put("groupNum", groupNum);
            GroupPo gp=groupService.getGroup(m);
            if (gp==null) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取用户组号码");
                return map;
            }
            if (gp.getGroupType()==0) {
                map.put("ReturnType", "1005");
                map.put("Message", "加入的组需要验证，不能直接加入");
                return map;
            }
            //检查是否已经在组
            int c=groupService.existUserInGroup(gp.getGroupId(), userId);
            if (c==0&&gp.getGroupCount()>50) {//不存在
                map.put("ReturnType", "1004");
                map.put("Message", "该组用户数已达上限50人，不能再加入了");
                return map;
            }
            //3-加入用户组
            UserPo u=(UserPo)ms.getAttribute("user");
            if (c==0) groupService.insertGroupUser(gp, u, 1);
            //组织返回值
            map.put("ReturnType", (c==0?"1001":"1101"));
            //组信息
            Map<String, Object> gm = new HashMap<String, Object>();
            gm.put("GroupId", gp.getGroupId());
            gm.put("GroupNum", gp.getGroupNum());
            gm.put("GroupType", gp.getGroupType()+"");
            gm.put("GroupImg", gp.getGroupImg());
            gm.put("GroupName", gp.getGroupName());
            gm.put("GroupCreator", gp.getCreateUserId());
            gm.put("GroupManager", gp.getAdminUserIds());
            gm.put("CreateTime", DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss", new Date(gp.getCTime().getTime())));
            gm.put("GroupDesc", gp.getDescn());
            gm.put("InnerPhoneNum", "3000");

            //组成员
            List<Map<String, Object>> rul=new ArrayList<Map<String, Object>>();
            List<UserPo> ul=groupService.getGroupMembers(gp.getGroupId());
            if (ul!=null&&ul.size()>0) {
                gm.put("GroupCount", ul.size());
                for (UserPo _u: ul) {
                    rul.add(_u.toHashMap4Mobile());
                }
                map.put("UserList", rul);
            }
            gm.put("GroupCount", rul.size()+"");
            return map;
        } catch(Exception e) {
            map.put("ReturnType", "T");
            map.put("SessionId", e.getMessage());
            return map;
        }
    }

    /**
     * 退出用户组
     */
    @RequestMapping(value="exitGroup.do")
    @ResponseBody
    public Map<String,Object> exitGroup(HttpServletRequest request) {
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
                    map.put("Message", "无法得到创建者");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-判断用户组进入是否符合业务逻辑
            String groupId=(String)m.get("GroupId");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取用户组号码");
            } else {
                m.clear();
                m.put("groupId", groupId);
                GroupPo gp=groupService.getGroup(m);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "用户组不存在");
                    return map;
                }
                //检查是否已经在组
                UserPo u=(UserPo)ms.getAttribute("user");
                int c=groupService.exitUserFromGroup(gp, u);
                if (c==0) {//不存在此用户
                    map.put("ReturnType", "1004");
                    map.put("Message", "用户不在该组，无法删除");
                } else if (c==1) {
                    map.put("ReturnType", "1001");
                    map.put("Message", "用户已退出组");
                } else {
                    map.put("ReturnType", "1101");
                    map.put("Message", "用户已退出组，并已经删除此组");
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
     * 获得新的用户编码
     * @return
     */
    private int getNewGroupNumber() {
        return SpiritRandom.getRandom(new Random(), 0, 999999);
    }
}