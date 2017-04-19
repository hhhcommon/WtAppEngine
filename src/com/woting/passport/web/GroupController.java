package com.woting.passport.web;

import java.sql.Timestamp;
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
import com.spiritdata.framework.util.SpiritRandom;
import com.spiritdata.framework.util.StringUtils;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.RequestUtils;
import com.woting.dataanal.gather.API.ApiGatherUtils;
import com.woting.dataanal.gather.API.mem.ApiGatherMemory;
import com.woting.dataanal.gather.API.persis.pojo.ApiLogPo;
import com.woting.passport.UGA.model.Group;
import com.woting.passport.UGA.persis.pojo.GroupPo;
import com.woting.passport.UGA.persis.pojo.UserPo;
import com.woting.passport.UGA.service.GroupService;
import com.woting.passport.UGA.service.UserService;
import com.woting.passport.mobile.MobileParam;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.DeviceType;
import com.woting.passport.session.SessionService;
import com.woting.passport.useralias.mem.UserAliasMemoryManage;
import com.woting.passport.useralias.model.UserAliasKey;
import com.woting.passport.useralias.persis.pojo.UserAliasPo;

@Controller
@RequestMapping(value="/passport/group/")
public class GroupController {
    private UserAliasMemoryManage uamm=UserAliasMemoryManage.getInstance();

    @Resource
    private GroupService groupService;
    @Resource
    private UserService userService;
    @Resource(name="redisSessionService")
    private SessionService sessionService;

    /**
     * 创建用户组，根据用户组类型创建用户
     */
    @RequestMapping(value="buildGroup.do")
    @ResponseBody
    public Map<String,Object> buildGroup(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.1-passport/group/buildGroup");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/buildGroup");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //创建用户组
            //得到组分类：验证群0；公开群1[原来的号码群]；密码群2
            int groupType=0;
            try {
                groupType=Integer.parseInt(m.get("GroupType")+"");
            } catch(Exception e) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法得到组分类");
                return map;
            }
            //若是密码群，得到密码
            String groupPwd=(m.get("GroupPwd")==null?null:m.get("GroupPwd")+"");
            if (groupType==2) {
                if (StringUtils.isNullOrEmptyOrSpace(groupPwd)) {
                    map.put("ReturnType", "1004");
                    map.put("Message", "无法得到组密码");
                    return map;
                }
            }
            //是否需要用户组成员
            boolean needMember=(m.get("NeedMember")==null?false:(m.get("NeedMember")+"").equals("1"));
            String memNames="";
            List<UserPo> ml=null;
            if (needMember) {
                String members=(m.get("Members")==null?null:m.get("Members")+"");
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
            int c=groupService.getCreateGroupCount(userId);
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
            //判断频率是否
            String groupFreq=(m.get("GroupFreq")==null?null:m.get("GroupFreq")+"");
            if (!StringUtils.isNullOrEmptyOrSpace(groupFreq)) {
                //检查模拟对讲频率是否合法
                if (!checkFreqArr(groupFreq)) {
                    map.put("ReturnType", "1010");
                    map.put("Message", "模拟频率信息不合法");
                    return map;
                }
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
            String groupName=m.get("GroupName")+"";
            if (StringUtils.isNullOrEmptyOrSpace(groupName)) {
                if (groupType==0) groupName="新建验证群"+(needMember?memNames:newGroupNum);
                if (groupType==1) groupName="新建公开群"+newGroupNum;
                if (groupType==2) groupName="新建密码群"+(needMember?memNames:newGroupNum);
            }
            //获得组描述
            String groupDescn=(m.get("GroupDescn")==null?null:m.get("GroupDescn")+"");
            //获得组描述
            String groupSignature=(m.get("GroupSignature")==null?null:m.get("GroupSignature")+"");

            //创建组
            if (ml==null) {
                ml=new ArrayList<UserPo>();
                UserPo u=(UserPo)userService.getUserById(userId);
                ml.add(u);
            }
            Group g=new Group();
            g.setGroupNum(""+newGroupNum);
            g.setGroupName(groupName);
            if (groupType==2) g.setGroupPwd(groupPwd);
            g.setCreateUserId(userId);
            g.setAdminUserIds(userId);
            g.setGroupMasterId(userId);
            g.setGroupType(groupType);
            g.setUserList(ml);
            if (!StringUtils.isNullOrEmptyOrSpace(groupDescn)) g.setDescn(groupDescn);
            if (!StringUtils.isNullOrEmptyOrSpace(groupSignature)) g.setGroupSignature(groupSignature);
            if (!StringUtils.isNullOrEmptyOrSpace(groupFreq)) g.setDefaultFreq(groupFreq);

            int retFlag=groupService.insertGroup(g);
            if (retFlag==1) {
                //组织返回值
                map.put("ReturnType", "1001");
                g.setGroupCount(ml.size());
                g.toHashMap4View();
                Map<String, Object> groupMap=g.toHashMap4View();
                groupMap.put("CreateTime", System.currentTimeMillis()+"");
                map.put("GroupInfo", groupMap);
            } else {
                map.put("ReturnType", "1020");
                map.put("Message", "创建组失败");
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 获得我所在的用户组
     */
    @RequestMapping(value="getGroupList.do")
    @ResponseBody
    public Map<String,Object> getGroupList(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.2-passport/group/getGroupList");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/getGroupList");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-获取分页信息
            int page=0;//获取页数
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};
            int pageSize=10;//得到每页条数
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};

            //2-得到用户组
            List<GroupPo> gl=groupService.getGroupsByUserId(userId, pageSize, page);
            List<Map<String, Object>> rgl=new ArrayList<Map<String, Object>>();
            if (gl!=null&&gl.size()>0) {
                for (GroupPo g:gl) rgl.add(g.toHashMap4View());
                map.put("ReturnType", "1001");
                map.put("GroupList", rgl);
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无所属用户组");
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 获得我所创建的用户组
     */
    @RequestMapping(value="getCreateGroupList.do")
    @ResponseBody
    public Map<String,Object> getCreateGroupList(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.3-passport/group/getCreateGroupList");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/getCreateGroupList");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-获取分页信息
            int page=0;//获取页数
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};
            int pageSize=10;//得到每页条数
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};

            //2-得到用户组
            List<GroupPo> gl=groupService.getCreateGroupsByUserId(userId, pageSize, page);
            List<Map<String, Object>> rgl=new ArrayList<Map<String, Object>>();
            if (gl!=null&&gl.size()>0) {
                for (GroupPo g:gl) rgl.add(g.toHashMap4View());
                map.put("ReturnType", "1001");
                map.put("GroupList", rgl);
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无所属用户组");
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 获得我所能管理的组
     */
    @RequestMapping(value="getManageGroupList.do")
    @ResponseBody
    public Map<String,Object> getManageGroupList(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.26-passport/group/getManageGroupList");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/getManageGroupList");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-获取分页信息
            int page=0;//获取页数
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};
            int pageSize=10;//得到每页条数
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};

            //2-得到用户组
            List<GroupPo> gl=groupService.getManageGroupsByUserId(userId, pageSize, page);
            List<Map<String, Object>> rgl=new ArrayList<Map<String, Object>>();
            if (gl!=null&&gl.size()>0) {
                for (GroupPo g:gl) rgl.add(g.toHashMap4View());
                map.put("ReturnType", "1001");
                map.put("GroupList", rgl);
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无所属用户组");
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 获得我所能管理的组
     */
    @RequestMapping(value="getMasterGroupList.do")
    @ResponseBody
    public Map<String,Object> getMasterGroupList(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.27-passport/group/getMasterGroupList");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/getMasterGroupList");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-获取分页信息
            int page=0;//获取页数
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};
            int pageSize=10;//得到每页条数
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};

            //2-得到用户组
            List<GroupPo> gl=groupService.getMasterGroupsByUserId(userId, pageSize, page);
            List<Map<String, Object>> rgl=new ArrayList<Map<String, Object>>();
            if (gl!=null&&gl.size()>0) {
                for (GroupPo g:gl) rgl.add(g.toHashMap4View());
                map.put("ReturnType", "1001");
                map.put("GroupList", rgl);
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无所属用户组");
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 获得组成员列表
     */
    @RequestMapping(value="getGroupMembers.do")
    @ResponseBody
    public Map<String,Object> getGroupMembers(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.4-passport/group/getGroupMembers");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/getGroupMembers");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-获取分页信息
            int page=0;//获取页数
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};
            int pageSize=10;//得到每页条数
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};

            //2-得到用户组Id
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                List<Map<String, Object>> rul=new ArrayList<Map<String, Object>>();
                List<UserPo> ul=groupService.getGroupMembers(groupId, pageSize, page);
                if (ul!=null&&ul.size()>0) {
                    for (UserPo u: ul) {
                        Map<String, Object> userViewM=u.toHashMap4Mobile();
                        UserAliasKey uak=new UserAliasKey(groupId, userId, u.getUserId());
                        UserAliasPo uap=uamm.getOneUserAlias(uak);
                        if (uap!=null) {
                            if (uap.getAliasName()!=null&&!StringUtils.isNullOrEmptyOrSpace(uap.getAliasName())) userViewM.put("UserAliasName",uap.getAliasName());
                            if (uap.getAliasDescn()!=null&&!StringUtils.isNullOrEmptyOrSpace(uap.getAliasDescn())) userViewM.put("UserAliasDescn",uap.getAliasDescn());
                        }
                        rul.add(userViewM);
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
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 用户组邀请，任何组都能够进行邀请
     */
    @RequestMapping(value="groupInvite.do")
    @ResponseBody
    public synchronized Map<String,Object> inviteGroup(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.5-passport/group/groupInvite");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/groupInvite");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            int isManager=0;//是否是管理员，=0不是
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                GroupPo gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                }
                if (gp.getAdminUserIds().indexOf(userId)!=-1) isManager=1;//是管理员
            }
            if (map.get("ReturnType")!=null) return map;

            String beInvitedUserIds=(m.get("BeInvitedUserIds")==null?null:m.get("BeInvitedUserIds")+"");
            if (StringUtils.isNullOrEmptyOrSpace(beInvitedUserIds)) {
                map.put("ReturnType", "1004");
                map.put("Message", "被邀请人Id为空");
                return map;
            }
            if (beInvitedUserIds.equals(userId)) {
                map.put("ReturnType", "1005");
                map.put("Message", "邀请人和被邀请人不能是同一个人");
                return map;
            }

            String inviteMsg=(m.get("InviteMsg")==null?null:m.get("InviteMsg")+"");
            map.putAll(groupService.inviteGroup(userId, beInvitedUserIds, groupId, inviteMsg, isManager));
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 得到邀请我的用户组信息
     */
    @RequestMapping(value="getInviteMeList.do")
    @ResponseBody
    public Map<String,Object> getInviteMeList(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.6-passport/group/getInviteMeList");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/getInviteMeList");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-获取分页信息
            int page=0;//获取页数
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};
            int pageSize=10;//得到每页条数
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};

            List<Map<String, Object>> imgl=groupService.getInviteGroupList(userId, pageSize, page);
            if (imgl!=null&&imgl.size()>0) {
                map.put("ReturnType", "1001");
                map.put("GroupList", imgl);
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无邀请我的用户组");
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 组邀请的处理
     */
    @RequestMapping(value="inviteDeal.do")
    @ResponseBody
    public Map<String,Object> inviteDeal(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.7-passport/group/inviteDeal");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/inviteDeal");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-处理用户组Id
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                GroupPo gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-邀请人id
            String inviteUserId=(m.get("InviteUserId")==null?null:m.get("InviteUserId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(inviteUserId)) {
                map.put("ReturnType", "1004");
                map.put("Message", "邀请人Id为空");
            }
            if (map.get("ReturnType")!=null) return map;

            //3-获得处理类型
            String dealType=(m.get("DealType")==null?null:m.get("DealType")+"");
            if (StringUtils.isNullOrEmptyOrSpace(dealType)) {
                map.put("ReturnType", "1005");
                map.put("Message", "没有处理类型dealType，无法处理");
            }
            if (map.get("ReturnType")!=null) return map;

            //4-获得拒绝理由
            String refuseMsg=(m.get("RefuseMsg")==null?null:m.get("RefuseMsg")+"");
            //5-邀请处理
            map.putAll(groupService.dealInvite(userId, inviteUserId, groupId, dealType.equals("2"), refuseMsg, 1, userId));
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }
    
    /**
     * 用户组申请，目前只考虑验证组的情况，密码组和公开组不进行处理
     */
    @RequestMapping(value="groupApply.do")
    @ResponseBody
    public Map<String,Object> applyGroup(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.8-passport/group/groupApply");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/groupApply");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            String adminId=null;
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                GroupPo gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                } else {
                    adminId=gp.getAdminUserIds();
                    if (gp.getGroupType()==1) {
                        map.put("ReturnType", "10031");
                        map.put("Message", "公开组不必申请！");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            String inviteMsg=(m.get("ApplyMsg")==null?null:m.get("ApplyMsg")+"");
            map.putAll(groupService.applyGroup(userId, groupId, adminId, inviteMsg));
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 得到某用户组的申请人列表信息
     */
    @RequestMapping(value="getApplyUserList.do")
    @ResponseBody
    public Map<String,Object> getApplyUserList(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.9-passport/group/getApplyUserList");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/getApplyUserList");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-处理用户组Id
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                GroupPo gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                } else {
                    if (gp.getGroupType()==1) {
                        map.put("ReturnType", "10031");
                        map.put("Message", "公开群，不存在申请！");
                    }
                    if (gp.getAdminUserIds()==null) {
                        map.put("ReturnType", "10032");
                        map.put("Message", "该群未设置管理员！");
                    } else if (gp.getAdminUserIds().indexOf(userId)==-1) {
                        map.put("ReturnType", "10021");
                        map.put("Message", "用户不是该组的管理员！");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-获取分页信息
            int page=0;//获取页数
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};
            int pageSize=10;//得到每页条数
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};

            List<Map<String, Object>> aul=groupService.getApplyUserList(groupId, pageSize, page);
            if (aul!=null&&aul.size()>0) {
                map.put("ReturnType", "1001");
                map.put("UserList", aul);
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "该用户组下没有申请人列表");
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 得到我所管理的用户组，并且这些组中有未处理的申请人
     */
    @RequestMapping(value="getExistApplyUserGroupList.do")
    @ResponseBody
    public Map<String,Object> getExistApplyUserGroupList(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.10-passport/group/getExistApplyUserGroupList");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/getExistApplyUserGroupList");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-获取分页信息
            int page=0;//获取页数
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};
            int pageSize=10;//得到每页条数
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};

            List<Map<String, Object>> eauGl=groupService.getExistApplyUserGroupList(userId, pageSize, page);
            if (eauGl!=null&&eauGl.size()>0) {
                map.put("ReturnType", "1001");
                map.put("GroupList", eauGl);
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "该用户组下没有申请人列表");
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 组申请的处理
     */
    @RequestMapping(value="applyDeal.do")
    @ResponseBody
    public Map<String,Object> applyDeal(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.11-passport/group/applyDeal");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";//组管理员
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/applyDeal");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-处理用户组Id
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                GroupPo gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                } else {
                    if (gp.getGroupType()==1) {
                        map.put("ReturnType", "10031");
                        map.put("Message", "公开组，不存在申请！");
                    }
                    if (gp.getAdminUserIds()==null) {
                        map.put("ReturnType", "10032");
                        map.put("Message", "该群未设置管理员！");
                    } else if (gp.getAdminUserIds().indexOf(userId)==-1) {
                        map.put("ReturnType", "10021");
                        map.put("Message", "用户不是该组的管理员！");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-申请人id
            String applyUserId=(m.get("ApplyUserId")==null?null:m.get("ApplyUserId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(applyUserId)) {
                map.put("ReturnType", "1004");
                map.put("Message", "申请人Id为空");
            }
            if (map.get("ReturnType")!=null) return map;

            //3-获得处理类型
            String dealType=(m.get("DealType")==null?null:m.get("DealType")+"");
            if (StringUtils.isNullOrEmptyOrSpace(dealType)) {
                map.put("ReturnType", "1005");
                map.put("Message", "没有处理类型dealType，无法处理");
            }
            if (map.get("ReturnType")!=null) return map;

            //4-获得拒绝理由
            String refuseMsg=(m.get("RefuseMsg")==null?null:m.get("RefuseMsg")+"");
            //4-申请处理
            map.putAll(groupService.dealInvite(applyUserId, userId, groupId, dealType.equals("2"), refuseMsg, 2, userId));
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 加入公开群
     */
    @RequestMapping(value="joinInGroup.do")
    @ResponseBody
    public Map<String,Object> joinInGroup(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.12-passport/group/joinInGroup");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/joinInGroup");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-判断用户组进入是否符合业务逻辑
            String groupNum=(m.get("GroupNum")==null?null:m.get("GroupNum")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupNum)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取用户组号码");
                return map;
            }
            Map<String, Object> param=new HashMap<String, Object>();
            param.put("groupNum", groupNum);
            GroupPo gp=groupService.getGroup(param);
            if (gp==null) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取用户组号码");
            } else {
                if (gp.getGroupType()==0) {
                    map.put("ReturnType", "1005");
                    map.put("Message", "加入的组需要验证，不能直接加入");
                }
                if (gp.getGroupType()==2) {
                    String groupPwd=(m.get("GroupPwd")==null?null:m.get("GroupPwd")+"");
                    if (StringUtils.isNullOrEmptyOrSpace(groupPwd)) {
                        map.put("ReturnType", "1006");
                        map.put("Message", "加入密码组需要提供组密码");
                    } else {
                        if (!gp.getGroupPwd().equals(groupPwd)) {
                            map.put("ReturnType", "1007");
                            map.put("Message", "组密码不正确，不能加入组");
                        }
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //检查是否已经在组
            boolean exist=groupService.existUserInGroup(gp.getGroupId(), userId);
            if (exist&&gp.getGroupCount()>50) {
                map.put("ReturnType", "1004");
                map.put("Message", "该组用户数已达上限50人，不能再加入了");
                return map;
            }
            //3-加入用户组
            UserPo u=(UserPo)userService.getUserById(userId);
            if (!exist) groupService.insertGroupUser(gp, u, 1, true, userId);
            //组织返回值
            map.put("ReturnType", (!exist?"1001":"1101"));
            //组信息
            Map<String, Object> gm=gp.toHashMap4View();
            //组成员
            List<Map<String, Object>> rul=new ArrayList<Map<String, Object>>();
            List<UserPo> ul=groupService.getGroupMembers(gp.getGroupId(), 0, 0);
            if (ul!=null&&ul.size()>0) {
                for (UserPo _u: ul) rul.add(_u.toHashMap4Mobile());
                map.put("UserList", rul);
            }
            gm.put("GroupCount", rul.size()+"");
            map.put("GroupInfo", gm);
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 主动退出用户组
     */
    @RequestMapping(value="exitGroup.do")
    @ResponseBody
    public Map<String,Object> exitGroup(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.13-passport/group/exitGroup");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/exitGroup");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-判断用户组退出是否符合业务逻辑
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取用户组号码");
            } else {
                GroupPo gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                } else {
                    UserPo u=(UserPo)userService.getUserById(userId);
                    int c=groupService.exitUserFromGroup(gp, u);
                    if (c==0) {//不存在此用户
                        map.put("ReturnType", "1004");
                        map.put("Message", "用户不在该组，无法删除");
                    } else if (c==1) {
                        map.put("ReturnType", "1001");
                        map.put("Message", "用户退组成功");
                    } else {
                        map.put("ReturnType", "10011");
                        map.put("Message", "用退组成功，并已删除该组");
                    }
                }
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 管理员踢出用户
     */
    @RequestMapping(value="kickoutGroup.do")
    @ResponseBody
    public Map<String,Object> kickoutUsersFromGroup(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.14-passport/group/kickoutGroup");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/kickoutGroup");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-处理用户组Id
            GroupPo gp=null;
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                } else {
                    if (gp.getAdminUserIds()==null) {
                        map.put("ReturnType", "10032");
                        map.put("Message", "该群未设置管理员！");
                    } else if (!gp.getAdminUserIds().equals(userId)) {
                        map.put("ReturnType", "10021");
                        map.put("Message", "用户不是该组的管理员！");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-处理用户组Id
            String userIds=(m.get("UserIds")==null?null:m.get("UserIds")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1004");
                map.put("Message", "无法获取被踢出用户Id");
                return map;
            }

            //3-处理
            map.put("ReturnType", "1001");
            map.put("Result", groupService.kickoutGroup(gp, userIds, userId));
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 管理员解散用户组
     */
    @RequestMapping(value="dissolveGroup.do")
    @ResponseBody
    public Map<String,Object> dissolveGroup(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.15-passport/group/dissolveGroup");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/dissolveGroup");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-处理用户组Id
            GroupPo gp=null;
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                } else {
                    if (gp.getAdminUserIds()==null) {
                        map.put("ReturnType", "10032");
                        map.put("Message", "该群未设置管理员！");
                    } else  if (!gp.getAdminUserIds().equals(userId)) {
                        map.put("ReturnType", "10021");
                        map.put("Message", "用户不是该组的管理员！");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //3-处理
            map.putAll(groupService.dissolve(gp, userId));
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 管理员移交权限
     */
    @RequestMapping(value="changGroupAdminner.do")
    @ResponseBody
    public Map<String,Object> changGroupAdminner(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.16-passport/group/changGroupAdminner");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/changGroupAdminner");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-处理用户组Id
            GroupPo gp=null;
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                } else {
                    if (gp.getAdminUserIds()==null) {
                        map.put("ReturnType", "10032");
                        map.put("Message", "该群未设置管理员！");
                    } else if (gp.getAdminUserIds().indexOf(userId)==-1) {
                        map.put("ReturnType", "10021");
                        map.put("Message", "用户不是该组的管理员！");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-被移交用户Id
            String toUserId=(m.get("ToUserId")==null?null:m.get("ToUserId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(toUserId)) {
                map.put("ReturnType", "1004");
                map.put("Message", "无法获取被移交用户Id");
            }
            if (map.get("ReturnType")!=null) return map;

            //3-处理
            map.putAll(groupService.changGroupAdminner(gp, toUserId, userId));
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 查找用户组
     */
    @RequestMapping(value="searchGroup.do")
    @ResponseBody
    public Map<String,Object> searchGroup(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.17-passport/group/searchGroup");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/searchGroup");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //获得查询串
            String searchStr=(m.get("SearchStr")==null?null:m.get("SearchStr")+"");
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
                    if (!StringUtils.isNullOrEmptyOrSpace(_g.getGroupSignature())) oneGroup.put("GroupSignature", _g.getGroupSignature());
                    oneGroup.put("GroupType", _g.getGroupType()+"");
                    oneGroup.put("GroupImg", _g.getGroupImg());
                    oneGroup.put("GroupName", _g.getGroupName());
                    if (!StringUtils.isNullOrEmptyOrSpace(_g.getCreateUserId())) oneGroup.put("GroupCreator", _g.getCreateUserId());
                    if (!StringUtils.isNullOrEmptyOrSpace(_g.getAdminUserIds())) oneGroup.put("GroupManager", _g.getAdminUserIds());
                    oneGroup.put("GroupCount", _g.getUserList().size()+"");
                    if (!StringUtils.isNullOrEmptyOrSpace(_g.getDescn())) oneGroup.put("GroupOriDescn", _g.getDescn());
                    oneGroup.put("CreateTime", _g.getCTime().getTime()+"");

                    List<UserPo> ul=_g.getUserList();
                    if (ul!=null&&!ul.isEmpty()) {
                        String userNames="";
                        String userIds="";
                        for (int j=0;j<ul.size(); j++) {
                            userNames+=","+ul.get(j).getLoginName();
                            userIds+=","+ul.get(j).getUserId();
                        }
                        oneGroup.put("UserNames", userNames.substring(1));
                        oneGroup.put("UserIds", userIds.substring(1));
                    }
                    groupList.add(oneGroup);
                }
                map.put("GroupList", groupList);
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无所属用户组");
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 更新用户组，注意这个更新不能更新用户组成员，也不能更新成员的组名称，只能更新组本身属性
     * 包括个人为组定义的个性化信息
     */
    @RequestMapping(value="updateGroup.do")
    @ResponseBody
    public Map<String,Object> updateGroup(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.18-passport/group/updateGroup");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/updateGroup");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //获得用户组
            int isManager=0;//不是管理员
            GroupPo gp=null;
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法得到用户组Id");
            } else {
                gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                }
                if (gp.getAdminUserIds().equals(userId)) isManager=1;
            }
            if (map.get("ReturnType")!=null) return map;

            //获得用户更新内容
            String groupName=(m.get("GroupName")==null?null:m.get("GroupName")+"");
            String groupDescn=(m.get("Descn")==null?null:m.get("Descn")+"");
            String groupSignature=(m.get("GroupSignature")==null?null:m.get("GroupSignature")+"");
            String groupFreq=(m.get("GroupFreq")==null?null:m.get("GroupFreq")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupName)&&StringUtils.isNullOrEmptyOrSpace(groupDescn)&&StringUtils.isNullOrEmptyOrSpace(groupSignature)&&StringUtils.isNullOrEmptyOrSpace(groupFreq)) {
                map.put("ReturnType", "1004");
                map.put("Message", "无法获得修改所需的新信息");
                return map;
            }
            
            Map<String, Object> param=new HashMap<String, Object>();
            if (!StringUtils.isNullOrEmptyOrSpace(groupName)) param.put("groupName", groupName);
            if (!StringUtils.isNullOrEmptyOrSpace(groupDescn)) param.put("groupDescn", groupDescn);
            if (!StringUtils.isNullOrEmptyOrSpace(groupSignature)&&isManager==1) param.put("groupSignature", groupSignature);//只有管理员才能修改用户组的签名
            if (!StringUtils.isNullOrEmptyOrSpace(groupFreq)) {
                //检查模拟对讲频率是否合法
                if (checkFreqArr(groupFreq)) param.put("groupFreq", groupFreq);
                else {
                    map.put("ReturnType", "1005");
                    map.put("Message", "模拟频率信息不合法");
                    return map;
                }
            }
            groupService.updateGroup(param, userId, gp);
            map.put("ReturnType", "1001");
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 更改组密码，只有组管理员和密码组，才能调用这个方法
     */
    @RequestMapping(value="updatePwd.do")
    @ResponseBody
    public Map<String,Object> updatePwd(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.19-passport/group/updatePwd");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/updatePwd");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //获得用户组
            GroupPo gp=null;
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法得到用户组Id");
            } else {
                gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                } else {
                    if (gp.getGroupType()!=2) {
                        map.put("ReturnType", "1004");
                        map.put("Message", "非密码组，无法修改密码");
                    } else if (gp.getAdminUserIds()==null) {
                        map.put("ReturnType", "10032");
                        map.put("Message", "该群未设置管理员！");
                    } else if (!gp.getAdminUserIds().equals(userId)) {
                        map.put("ReturnType", "1005");
                        map.put("Message", "不是组管理员，无权修改密码");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-获取其他参数
            String oldPwd=(m.get("OldPassword")==null?null:m.get("OldPassword")+"");
            String newPwd=(m.get("NewPassword")==null?null:m.get("NewPassword")+"");
            String errMsg="";
            if (StringUtils.isNullOrEmptyOrSpace(oldPwd)) errMsg+=",旧密码为空";
            if (StringUtils.isNullOrEmptyOrSpace(newPwd)) errMsg+=",新密码为空";
            if (!StringUtils.isNullOrEmptyOrSpace(errMsg)) {
                errMsg=errMsg.substring(1);
                map.put("ReturnType", "1006");
                map.put("Message", errMsg+",无法需改密码");
            }
            if (oldPwd.equals(newPwd)) {
                map.put("ReturnType", "1007");
                map.put("Message", "新旧密码不能相同");
            } else if (!oldPwd.equals(gp.getGroupPwd())) {
                map.put("ReturnType", "1008");
                map.put("Message", "旧密码不正确");
            }
            if (map.get("ReturnType")!=null) return map;

            //获得用户更新内容
            gp.setGroupId(groupId);
            gp.setGroupPwd(newPwd);
            groupService.updateGroup(gp);
            map.put("ReturnType", "1001");
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 得到需要我审核的用户邀请信息，只对组管理员有效，只对审核群组有效
     */
    @RequestMapping(value="getNeedCheckInviteUserGroupList.do")
    @ResponseBody
    public Map<String,Object> getNeedCheckInviteUserGroupList(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.20-passport/group/getNeedCheckInviteUserGroupList");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/getNeedCheckInviteUserGroupList");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-处理用户组Id
            GroupPo gp=null;
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                } else {
                    if (gp.getGroupType()!=0) {
                        map.put("ReturnType", "10031");
                        map.put("Message", "用户组必须是审核组");
                    }
                    if (gp.getAdminUserIds()==null) {
                        map.put("ReturnType", "10032");
                        map.put("Message", "该群未设置管理员！");
                    } else if (!gp.getAdminUserIds().equals(userId)) {
                        map.put("ReturnType", "10021");
                        map.put("Message", "用户不是该组的管理员！");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //3-处理
            List<Map<String, Object>> rul=new ArrayList<Map<String, Object>>();
            List<Map<String, Object>> iuml=groupService.getNeedCheckInviteUserGroupList(groupId);
            if (iuml!=null&&iuml.size()>0) {
                Map<String, Object> ium;
                for (Map<String, Object> one:iuml) {
                    ium=new HashMap<String, Object>();
                    if (!StringUtils.isNullOrEmptyOrSpace((String)one.get("inviteMessage"))) ium.put("InviteMessage", one.get("inviteMessage"));
                    ium.put("InviteTime", ((Date)one.get("inviteTime")).getTime()+"");
                    ium.put("InviteCount", one.get("inviteVector"));
                    ium.put("InviteUserId", one.get("inviteUserId"));
                    ium.put("BeInviteUserId", one.get("userId"));
                    ium.put("UserName", one.get("loginName"));
                    if (!StringUtils.isNullOrEmptyOrSpace((String)one.get("userDescn"))) ium.put("UserDescn", one.get("userDescn"));
                    if (!StringUtils.isNullOrEmptyOrSpace((String)one.get("mailAddress"))) ium.put("Email", one.get("mailAddress"));
                    if (!StringUtils.isNullOrEmptyOrSpace((String)one.get("mainPhoneNum"))) ium.put("PhoneNum", one.get("mainPhoneNum"));
                    if (!StringUtils.isNullOrEmptyOrSpace((String)one.get("portraitBig"))) ium.put("PortraitBig", one.get("portraitBig"));
                    if (!StringUtils.isNullOrEmptyOrSpace((String)one.get("portraitMini"))) ium.put("PortraitMini", one.get("portraitMini"));
                    rul.add(ium);
                }
                map.put("ReturnType", "1001");
                map.put("InviteUserList", rul);
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无需要审核的邀请");
            }
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 审核处理，只对审核组的管理员开放
     */
    @RequestMapping(value="checkDeal.do")
    @ResponseBody
    public Map<String,Object> checkDeal(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.21-passport/group/checkDeal");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";//组管理员
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/checkDeal");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-处理用户组Id
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                GroupPo gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                } else {
                    if (gp.getGroupType()!=0) {
                        map.put("ReturnType", "10031");
                        map.put("Message", "不在审核组，不能完成此功能！");
                    }
                    if (gp.getAdminUserIds()==null) {
                        map.put("ReturnType", "10032");
                        map.put("Message", "该群未设置管理员！");
                    } else if (gp.getAdminUserIds().indexOf(userId)==-1) {
                        map.put("ReturnType", "10021");
                        map.put("Message", "用户不是该组的管理员！");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-邀请人
            String inviteUserId=(m.get("InviteUserId")==null?null:m.get("InviteUserId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(inviteUserId)) {
                map.put("ReturnType", "1004");
                map.put("Message", "邀请人Id无法获得");
            }
            if (map.get("ReturnType")!=null) return map;

            //3-被邀请人
            String beInvitedUserId=(m.get("BeInvitedUserId")==null?null:m.get("BeInvitedUserId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(beInvitedUserId)) {
                map.put("ReturnType", "1005");
                map.put("Message", "被邀请人Id无法获得");
            }
            if (map.get("ReturnType")!=null) return map;

            //4-获得处理类型
            String dealType=(m.get("DealType")==null?null:m.get("DealType")+"");
            if (StringUtils.isNullOrEmptyOrSpace(dealType)) {
                map.put("ReturnType", "1006");
                map.put("Message", "没有处理类型dealType，无法处理");
            }
            if (map.get("ReturnType")!=null) return map;

            //5-获得拒绝理由
            String refuseMsg=(m.get("RefuseMsg")==null?null:m.get("RefuseMsg")+"");

            //6-处理
            map.putAll(groupService.dealCheck(inviteUserId, beInvitedUserId, groupId, dealType.equals("2"), refuseMsg, userId));
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 修改组内成员信息，主要是别名
     */
    @RequestMapping(value="updateGroupUser.do")
    @ResponseBody
    public Map<String,Object> updateGroupUser(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.22-passport/group/updateGroupUser");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/updateGroupUser");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //获得用户组
            GroupPo gp=null;
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法得到用户组Id");
            } else {
                gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //获得所修改的用户Id
            String updateUserId=(m.get("UpdateUserId")==null?null:m.get("UpdateUserId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(updateUserId)) {
                map.put("ReturnType", "1004");
                map.put("Message", "无法获得被修改用户Id");
                return map;
            }
            String userAliasName=(m.get("UserAliasName")==null?null:m.get("UserAliasName")+"");
            String userAliasDescn=(m.get("UserAliasDescn")==null?null:m.get("UserAliasDescn")+"");
            if (StringUtils.isNullOrEmptyOrSpace(userAliasName)&&StringUtils.isNullOrEmptyOrSpace(userAliasName)) {
                map.put("ReturnType", "1005");
                map.put("Message", "无法获得修改所需的新信息");
                return map;
            }

            Map<String, String> param=new HashMap<String, String>();
            if (!StringUtils.isNullOrEmptyOrSpace(updateUserId)) param.put("updateUserId", updateUserId);
            if (!StringUtils.isNullOrEmptyOrSpace(userAliasName)) param.put("userAliasName", userAliasName);
            if (!StringUtils.isNullOrEmptyOrSpace(userAliasDescn)) param.put("userAliasDescn", userAliasDescn);

            map.putAll(groupService.updateGroupUser(param, userId, gp));
            map.put("ReturnType", "1001");
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 群主权限手工移交
     */
    @RequestMapping(value="changGroupMaster.do")
    @ResponseBody
    public Map<String,Object> changGroupMaster(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.23-passport/group/changGroupMaster");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/changGroupMaster");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-处理用户组Id
            GroupPo gp=null;
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                } else {
                    if (!gp.getGroupMasterId().equals(userId)) {
                        map.put("ReturnType", "10021");
                        map.put("Message", "用户不是该组的群主！");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-被移交用户Id
            String toUserId=(m.get("ToUserId")==null?null:m.get("ToUserId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(toUserId)) {
                map.put("ReturnType", "1004");
                map.put("Message", "无法获取被移交用户Id");
            } else {
                if (toUserId.equals(userId)) {
                    map.put("ReturnType", "1005");
                    map.put("Message", "自己不能把权限移交给自己");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //3-处理
            map.putAll(groupService.changGroupMaster(gp, toUserId, userId));
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * 群主设置管理员
     */
    @RequestMapping(value="setGroupAdmin.do")
    @ResponseBody
    public Map<String,Object> setGroupMaster(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.2.24-passport/group/setGroupAdmin");
        alPo.setObjType("005");//用户组对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            String userId="";
            MobileUDKey mUdk=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/group/setGroupAdmin");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");
                    } else {
                        map.putAll(retM);
                        if ((retM.get("ReturnType")+"").equals("1001")) map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
            //数据收集处理==2
            if (map.get("UserId")!=null&&!StringUtils.isNullOrEmptyOrSpace(map.get("UserId")+"")) {
                alPo.setOwnerId(map.get("UserId")+"");
            } else {
                //过客
                if (mUdk!=null) alPo.setOwnerId(mUdk.getDeviceId());
                else alPo.setOwnerId("0");
            }
            if (mUdk!=null) {
                alPo.setDeviceType(mUdk.getPCDType());
                alPo.setDeviceId(mUdk.getDeviceId());
            }
            if (m!=null) {
                if (mUdk!=null&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setExploreVer(m.get("MobileClass")+"");
                    }
                    if (m.get("exploreName")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("exploreName")+"")) {
                        alPo.setExploreName(m.get("exploreName")+"");
                    }
                } else {
                    if (m.get("MobileClass")!=null&&!StringUtils.isNullOrEmptyOrSpace(m.get("MobileClass")+"")) {
                        alPo.setDeviceClass(m.get("MobileClass")+"");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-处理用户组Id
            GroupPo gp=null;
            String groupId=(m.get("GroupId")==null?null:m.get("GroupId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(groupId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取组Id");
            } else {
                gp=groupService.getGroupById(groupId);
                if (gp==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获取用户组Id为["+groupId+"]的用户组");
                } else {
                    if (!gp.getGroupMasterId().equals(userId)) {
                        map.put("ReturnType", "10021");
                        map.put("Message", "用户不是该组的群主！");
                    }
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-得到参数
            String addAdminUserIds=(m.get("AddAdminUserIds")==null?null:m.get("AddAdminUserIds")+"");
            String delAdminUserIds=(m.get("DelAdminUserIds")==null?null:m.get("DelAdminUserIds")+"");
            if (StringUtils.isNullOrEmptyOrSpace(addAdminUserIds)&&StringUtils.isNullOrEmptyOrSpace(delAdminUserIds)) {
                map.put("ReturnType", "1004");
                map.put("Message", "设置列表或删除列表至少设置一个");
            }
            if (map.get("ReturnType")!=null) return map;

            //3-处理
            map.putAll(groupService.setGroupMaster(gp, addAdminUserIds, delAdminUserIds, userId));
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", StringUtils.getAllMessage(e));
            alPo.setDealFlag(2);
            return map;
        } finally {
            //数据收集处理=3
            alPo.setEndTime(new Timestamp(System.currentTimeMillis()));
            alPo.setReturnData(JsonUtils.objToJson(map));
            try {
                ApiGatherMemory.getInstance().put2Queue(alPo);
            } catch (InterruptedException e) {}
        }
    }

    /*
     * 获得新的用户编码
     * @return
     */
    private int getNewGroupNumber() {
        return SpiritRandom.getRandom(new Random(), 0, 999999);
    }

    private boolean checkFreqArr(String freqs) {
        if (StringUtils.isNullOrEmptyOrSpace(freqs)) return true;
        String s[]=freqs.split(",");
        if (s.length==0) return true;

        for (String o: s) {
            String ss[]=o.split("-");
            if (ss.length==1) {
                if (o.indexOf(".")==-1) return false;
                try {
                   Float.parseFloat(o);
                } catch(Exception e) {
                    return false;
                }
            } else if (ss.length==2){
                if (ss[0].indexOf(".")==-1||ss[1].indexOf(".")==-1) return false;
                Float f1, f2;
                try {
                    f1=Float.parseFloat(ss[0]);
                } catch(Exception e) {
                    return false;
                }
                try {
                    f2=Float.parseFloat(ss[1]);
                } catch(Exception e) {
                    return false;
                }
                if (f1>=f2) return false;
            } else return false;
        }
        return true;
    }
}