package com.woting.passport.web;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.SpiritRandom;
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.model.MobileParam;
import com.woting.appengine.mobile.session.mem.SessionMemoryManage;
import com.woting.appengine.mobile.session.model.MobileSession;
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.woting.passport.UGA.service.GroupService;
import com.woting.passport.UGA.service.UserService;
import com.woting.passport.friend.service.FriendService;
import com.woting.passport.login.persistence.pojo.MobileUsedPo;
import com.woting.passport.login.service.MobileUsedService;

@Controller
@RequestMapping(value="/passport/")
public class PassportController {
    @Resource
    private UserService userService;
    @Resource
    private GroupService groupService;
    @Resource
    private MobileUsedService muService;
    @Resource
    private FriendService friendService;

    private SessionMemoryManage smm=SessionMemoryManage.getInstance();

    /**
     * 用户登录
     * @throws IOException
     */
    @RequestMapping(value="user/mlogin.do")
    @ResponseBody
    public Map<String,Object> login(HttpServletRequest request) {
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
            if (sk!=null) map.put("SessionId", sk.getSessionId());

            String ln=(String)m.get("UserName");
            String pwd=(String)m.get("Password");
            String errMsg="";
            if (StringUtils.isNullOrEmptyOrSpace(ln)) errMsg+=",用户名为空";
            if (StringUtils.isNullOrEmptyOrSpace(pwd)) errMsg+=",密码为空";
            if (!StringUtils.isNullOrEmptyOrSpace(errMsg)) {
                errMsg=errMsg.substring(1);
                map.put("ReturnType", "0000");
                map.put("Message", errMsg+",无法登陆");
                return map;
            }
            UserPo u=userService.getUserByLoginName(ln);
            //1-判断是否存在用户
            if (u==null) { //无用户
                map.put("ReturnType", "1002");
                map.put("Message", "无登录名为["+ln+"]的用户.");
                return map;
            }
            //2-判断密码是否匹配
            if (!u.getPassword().equals(pwd)) {
                map.put("ReturnType", "1003");
                map.put("Message", "密码不匹配.");
                return map;
            }
            //3-用户登录成功
            if (sk!=null) {
                sk.setUserId(u.getUserId());
                map.put("SessionId", sk.getSessionId());
                //3.1-处理Session
                smm.expireAllSessionByIMEI(sk.getMobileId()); //作废所有imei对应的Session
                MobileSession ms=new MobileSession(sk);
                ms.addAttribute("user", u);
                smm.addOneSession(ms);
                //3.2-保存使用情况
                MobileUsedPo mu=new MobileUsedPo();
                mu.setImei(sk.getMobileId());
                mu.setStatus(1);
                mu.setUserId(u.getUserId());
                muService.saveMobileUsed(mu);
            }
            //4-返回成功，若没有IMEI也返回成功
            map.put("ReturnType", "1001");
            map.put("UserInfo", u.toHashMap4Mobile());
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
     * 用户注册
     * @throws IOException
     */
    @RequestMapping(value="user/register.do")
    @ResponseBody
    public Map<String,Object> register(HttpServletRequest request) {
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

            String ln=(String)m.get("UserName");
            String pwd=(String)m.get("Password");
            String errMsg="";
            if (StringUtils.isNullOrEmptyOrSpace(ln)) errMsg+=",用户名为空";
            if (StringUtils.isNullOrEmptyOrSpace(pwd)) errMsg+=",密码为空";
            if (!StringUtils.isNullOrEmptyOrSpace(errMsg)) {
                errMsg=errMsg.substring(1);
                map.put("ReturnType", "1002");
                map.put("Message", errMsg+",无法注册");
                return map;
            }
            UserPo nu=new UserPo();
            nu.setLoginName(ln);
            nu.setPassword(pwd);
            //1-判断是否有重复的用户
            UserPo oldUser=userService.getUserByLoginName(ln);
            if (oldUser!=null) { //重复
                map.put("ReturnType", "1003");
                map.put("Message", "登录名重复,无法注册.");
                return map;
            }
            //2-保存用户
            nu.setCTime(new Timestamp(System.currentTimeMillis()));
            nu.setUserType(1);
            nu.setUserId(SequenceUUID.getUUIDSubSegment(4));
            int rflag=userService.insertUser(nu);
            if (rflag!=1) {
                map.put("ReturnType", "1004");
                map.put("Message", "注册失败，新增用户存储失败");
                return map;
            }
            //3-注册成功后，自动登陆，及后处理
            if (ms!=null) {
                map.put("SessionId", nu.getUserId());
                //3.1-处理Session
                smm.expireAllSessionByIMEI(ms.getKey().getMobileId()); //作废所有imei对应的Session
                MobileKey newMk=ms.getKey();
                newMk.setUserId(nu.getUserId());
                MobileSession nms=new MobileSession(newMk);
                ms.addAttribute("user", nu);
                smm.addOneSession(ms);
                //3.2-保存使用情况
                MobileUsedPo mu=new MobileUsedPo();
                mu.setImei(newMk.getMobileId());
                mu.setStatus(1);
                mu.setUserId(nu.getUserId());
                mu.setPCDType(newMk.getPCDType());
                muService.saveMobileUsed(mu);
            }
            //4-返回成功，若没有IMEI也返回成功
            map.put("ReturnType", "1001");
            map.put("UserId", nu.getUserId());
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
     * 用户注销
     * @throws IOException
     */
    @RequestMapping(value="user/mlogout.do")
    @ResponseBody
    public Map<String,Object> mlogout(HttpServletRequest request) {
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

            if (ms!=null) ms.remove("user");
            //4-返回成功，不管后台处理情况，总返回成功
            map.put("ReturnType", "1001");
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
     * 绑定用户的其他信息，目前有手机/eMail
     * @throws IOException
     */
    @RequestMapping(value="user/bindExtUserInfo.do")
    @ResponseBody
    public Map<String,Object> bindExtUserInfo(HttpServletRequest request) {
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

            String phoneNum=(String)m.get("PhoneNum");
            String mail=(String)m.get("MailAddr");
            String userNum=(String)m.get("UserNum");
            if (StringUtils.isNullOrEmptyOrSpace(phoneNum)&&StringUtils.isNullOrEmptyOrSpace(mail)&&StringUtils.isNullOrEmptyOrSpace(userNum)) {
                map.put("ReturnType", "1003");
                map.put("Message", "邮箱、手机号码或用户号不能同时为空");
            } else {
                UserPo u=(UserPo)ms.getAttribute("user");
                if (!StringUtils.isNullOrEmptyOrSpace(userNum)) u.setUserNum(userNum);
                if (!StringUtils.isNullOrEmptyOrSpace(phoneNum)) u.setMainPhoneNum(phoneNum);
                if (!StringUtils.isNullOrEmptyOrSpace(mail)) u.setMailAddress(mail);
                int retFlag=userService.updateUser(u);
                if (retFlag==1) map.put("ReturnType", "1001");
                else if (retFlag==1) {
                    map.put("ReturnType", "10011");
                    map.put("Message", "用户号重复，其他信息保存成功");
                } else {
                    map.put("ReturnType", "1004");
                    map.put("Message", "存储账户绑定信息失败");
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

    /**
     * 随机获得可用的用户号码
     */
    @RequestMapping(value="user/getRandomUserNum.do")
    @ResponseBody
    public Map<String,Object> getRandomUserNum(HttpServletRequest request) {
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
            int newUserNum=getNewUserNumber();
            while (true) {
                if (userService.getUserByNum("u"+newUserNum)==null) break;
                newUserNum=getNewUserNumber();
            }
            map.put("ReturnType", "1001");
            map.put("NewUserNum", "u"+newUserNum);
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
     * 修改密码
     */
    @RequestMapping(value="user/updatePwd.do")
    @ResponseBody
    public Map<String,Object> updatePwd(HttpServletRequest request) {
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

            //2-获取其他参数
            String oldPwd=(String)m.get("OldPassword");
            String newPwd=(String)m.get("NewPassword");
            String errMsg="";
            if (StringUtils.isNullOrEmptyOrSpace(oldPwd)) errMsg+=",旧密码为空";
            if (StringUtils.isNullOrEmptyOrSpace(newPwd)) errMsg+=",新密码为空";
            if (!StringUtils.isNullOrEmptyOrSpace(errMsg)) {
                errMsg=errMsg.substring(1);
                map.put("ReturnType", "1003");
                map.put("Message", errMsg+",无法需改密码");
                return map;
            }
            if (oldPwd.equals(newPwd)) {
                map.put("ReturnType", "1004");
                map.put("Message", "新旧密码不能相同");
                return map;
            }
            UserPo u=(UserPo)ms.getAttribute("user");
            if (u.getPassword().equals(oldPwd)) {
                u.setPassword(newPwd);
                int retFlag=userService.updateUser(u);
                if (retFlag==1) map.put("ReturnType", "1001");
                else {
                    map.put("ReturnType", "1006");
                    map.put("Message", "存储新密码失败");
                }
            } else {
                map.put("ReturnType", "1005");
                map.put("Message", "旧密码不匹配");
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
     * 找回密码——通过手机
     * @throws IOException
     */
    @RequestMapping(value="user/retrieveByPwd.do")
    @ResponseBody
    public Map<String,Object> retrieveByPwd(HttpServletRequest request) {
        System.out.println("===================");
        //返回登录的情况
        return null;
    }

    /**
     * 找回密码——通过邮箱
     */
    @RequestMapping(value="user/retrieveByEmail.do")
    @ResponseBody
    public Map<String,Object> retrieveByEmail(HttpServletRequest request) {
        System.out.println("===================");
        //返回登录的情况
        return null;
    }

    /**
     * 得到历史访问列表
     */
    @RequestMapping(value="getGroupsAndFriends.do")
    @ResponseBody
    public Map<String,Object> getGroupsAndFriends(HttpServletRequest request) {
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

            //2-得到用户组及好友
            int size=0;
            List<Map<String, Object>> gl=groupService.getGroupsByUserId(userId);
            Map<String, Object> topItem=new HashMap<String, Object>();//一个分类
            if (gl!=null&&gl.size()>0) size=gl.size();
            topItem.put("Type", "group");
            topItem.put("Name", "群组");
            topItem.put("PageSize", size);
            topItem.put("AllSize", size);
            if (size>0) {
                List<Map<String, Object>> rgl=new ArrayList<Map<String, Object>>();
                Map<String, Object> gm;
                for (Map<String, Object> g:gl) {
                    gm=new HashMap<String, Object>();
                    gm.put("GroupId", g.get("id"));
                    gm.put("GroupNum", g.get("groupNum"));
                    gm.put("GroupType", g.get("groupType"));
                    if (!StringUtils.isNullOrEmptyOrSpace((String)g.get("groupImg"))) gm.put("GroupImg", g.get("groupImg"));
                    gm.put("GroupName", g.get("groupName"));
                    if (!StringUtils.isNullOrEmptyOrSpace((String)g.get("groupSignature"))) gm.put("GroupSignature", g.get("groupSignature"));
                    if (!StringUtils.isNullOrEmptyOrSpace((String)g.get("createUserId"))) gm.put("GroupCreator", g.get("createUserId"));
                    if (!StringUtils.isNullOrEmptyOrSpace((String)g.get("adminUserIds"))) gm.put("GroupManager", g.get("adminUserIds"));
                    gm.put("GroupCount", g.get("groupCound"));
                    if (!StringUtils.isNullOrEmptyOrSpace((String)g.get("descn"))) gm.put("GroupOriDescn", g.get("descn"));
                    if (!StringUtils.isNullOrEmptyOrSpace((String)g.get("groupDescn"))) gm.put("GroupMyDesc", g.get("groupDescn"));
                    if (!StringUtils.isNullOrEmptyOrSpace((String)g.get("groupAlias"))) gm.put("GroupMyAlias", g.get("groupAlias"));
                    rgl.add(gm);
                }
                topItem.put("Groups", rgl);
            }
            map.put("GroupList", topItem);

            size=0;
            List<UserPo> ul=friendService.getFriendList(userId);
            if (ul!=null&&ul.size()>0) size=ul.size();
            topItem=new HashMap<String, Object>();//一个分类
            topItem.put("Type", "user");
            topItem.put("Name", "好友");
            topItem.put("PageSize", size);
            topItem.put("AllSize", size);
            if (size>0) {
                List<Map<String, Object>> rul=new ArrayList<Map<String, Object>>();
                for (UserPo u: ul) {
                    if (!u.getUserId().equals(userId)) rul.add(u.toHashMap4Mobile());
                }
                topItem.put("Friends", rul);
            }
            map.put("FriendList", topItem);

            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    /*
     * 获得新的用户编码
     * @return
     */
    private int getNewUserNumber() {
        return SpiritRandom.getRandom(new Random(), 0, 999999);
    }
}

//
//List<Map<String, Object>> ul = new ArrayList<Map<String, Object>>();
//Map<String, Object> u = new HashMap<String, Object>();
//u.put("UserId", "123456");
//u.put("UserName", "张先生1");
//u.put("Portrait", "images/person.png");
//ul.add(u);
//u = new HashMap<String, Object>();
//u.put("UserId", "334455");
//u.put("UserName", "张先生2");
//u.put("Portrait", "images/person.png");
//ul.add(u);
//u = new HashMap<String, Object>();
//u.put("UserId", "336655");
//u.put("UserName", "张先生3");
//u.put("Portrait", "images/person.png");
//ul.add(u);
//u = new HashMap<String, Object>();
//u.put("UserId", "333sd5");
//u.put("UserName", "张先生4");
//u.put("Portrait", "images/person.png");
//ul.add(u);
//map.put("ReturnType", "1001");
//map.put("SessionId", SequenceUUID.getUUIDSubSegment(4));
//map.put("UserList", ul);
//