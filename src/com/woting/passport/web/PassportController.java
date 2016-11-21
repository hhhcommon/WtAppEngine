package com.woting.passport.web;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.SpiritRandom;
import com.spiritdata.framework.util.StringUtils;
import com.spiritdata.framework.core.lock.BlockLockConfig;
import com.spiritdata.framework.core.lock.ExpirableBlockKey;
import com.spiritdata.framework.ext.redis.lock.RedisBlockLock;
import com.spiritdata.framework.ext.spring.redis.RedisOperService;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.RequestUtils;
import com.woting.cm.core.dict.model.DictRefRes;
import com.woting.cm.core.dict.service.DictService;
import com.woting.dataanal.gather.API.ApiGatherUtils;
import com.woting.dataanal.gather.API.mem.ApiGatherMemory;
import com.woting.dataanal.gather.API.persis.pojo.ApiLogPo;
import com.woting.passport.UGA.persis.pojo.UserPo;
import com.woting.passport.UGA.service.GroupService;
import com.woting.passport.UGA.service.UserService;
import com.woting.passport.friend.service.FriendService;
import com.woting.passport.login.persis.pojo.MobileUsedPo;
import com.woting.passport.login.service.MobileUsedService;
import com.woting.passport.mobile.MobileParam;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.DeviceType;
import com.woting.passport.session.SessionService;
import com.woting.passport.session.redis.RedisUserDeviceKey;
import com.woting.passport.useralias.mem.UserAliasMemoryManage;
import com.woting.passport.useralias.model.UserAliasKey;
import com.woting.passport.useralias.persis.pojo.UserAliasPo;
import com.woting.plugins.sms.SendSMS;

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
    private DictService dictService;
    @Resource
    private FriendService friendService;
    @Resource(name="redisSessionService")
    private SessionService sessionService;
    @Resource
    JedisConnectionFactory redisConn;

    private UserAliasMemoryManage uamm=UserAliasMemoryManage.getInstance();

    /**
     * 用户登录
     * @throws IOException
     */
    @RequestMapping(value="user/mlogin.do")
    @ResponseBody
    public Map<String,Object> login(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.1-passport/user/mlogin");
        alPo.setObjType("003");//设置为用户
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            MobileParam mp=MobileParam.build(m);
            if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                mp.setImei(request.getSession().getId());
            }
            MobileUDKey mUdk=mp.getUserDeviceKey();
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
            if (m==null||m.size()==0||mUdk==null||!mUdk.isValidate()) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
                return map;
            }

            String ln=(m.get("UserName")==null?null:m.get("UserName")+"");
            String pwd=(m.get("Password")==null?null:m.get("Password")+"");
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
            if (u==null) u=userService.getUserByPhoneNum(ln);
            if (u==null) u=userService.getUserByUserNum(ln);
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
            mUdk.setUserId(u.getUserId());
            RedisUserDeviceKey redisUdk=new RedisUserDeviceKey(mUdk);
            RedisOperService roService=new RedisOperService(redisConn, 4);
            ExpirableBlockKey rLock=RedisBlockLock.lock(redisUdk.getKey_Lock(), roService, new BlockLockConfig(5, 2, 0, 50));
            try {
                sessionService.registUser(mUdk, u);
                MobileUsedPo mu=new MobileUsedPo();
                mu.setImei(mUdk.getDeviceId());
                mu.setStatus(1);
                mu.setPCDType(mUdk.getPCDType());
                mu.setUserId(u.getUserId());
                muService.saveMobileUsed(mu);
            } finally {
                rLock.unlock();
                roService.close();
                roService=null;
            }
            //4-返回成功，若没有IMEI也返回成功
            map.put("ReturnType", "1001");
            if (u!=null) {
                Map<String, Object> um=u.toDetailInfo();
                List<DictRefRes> dictRefList=dictService.getDictRefs("plat_User", u.getUserId());
                if (dictRefList!=null&&!dictRefList.isEmpty()) {
                    for (DictRefRes drr: dictRefList) {
                        if (drr.getDm().getId().equals("8")) {//性别
                            um.put("Sex", drr.getDd().getNodeName());
                        } else
                        if (drr.getDm().getId().equals("2")&&drr.getRefName().equals("地区")) {
                            um.put("Region", drr.getDd().getTreePathName());
                        }
                    }
                }
                if (u.getBirthday()!=null) um.put("Age", getAge(u.getBirthday().getTime())+"");
                map.put("ReturnType", "1001");
                map.put("UserInfo", um);
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
     * 用户注册
     * @throws IOException
     */
    @RequestMapping(value="user/register.do")
    @ResponseBody
    public Map<String,Object> register(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.2-passport/user/register");
        alPo.setObjType("003");//设置为用户
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
                if (StringUtils.isNullOrEmptyOrSpace(mUdk.getDeviceId())) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    map.putAll(mUdk.toHashMapAsBean());
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

            String ln=(m.get("UserName")==null?null:m.get("UserName")+"");
            String pwd=(m.get("Password")==null?null:m.get("Password")+"");
            String phonenum=m.get("MainPhoneNum")==null?null:m.get("Password")+"";
            String usePhone=(m.get("UsePhone")==null?null:m.get("UsePhone")+"");
            String errMsg="";

            if (StringUtils.isNullOrEmptyOrSpace(ln)) errMsg+=",用户名为空";
            if (ln!=null) {
                char[] c=ln.toCharArray();
                if (c[0]>='0' && c[0]<='9') errMsg+=",登录名第一个字符不能是数字";
            }
            if (StringUtils.isNullOrEmptyOrSpace(pwd)) errMsg+=",密码为空";
            if (usePhone!=null&&usePhone.equals("1")) {
                if(phonenum.toLowerCase().equals("null")) errMsg+=",手机号为空";
            }
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
            if (usePhone!=null&&usePhone.equals("1")) {
//                //1.5-手机号码注册
//                String getValue=roService.get(redisUdk.getKey_UserPhoneCheck());
//                String info=getValue==null?"":new String(getValue);
                nu.setMainPhoneNum(phonenum);
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
            mUdk.setUserId(nu.getUserId());
            RedisOperService roService=new RedisOperService(redisConn, 4);
            RedisUserDeviceKey redisUdk=new RedisUserDeviceKey(mUdk);
            ExpirableBlockKey rLock=RedisBlockLock.lock(redisUdk.getKey_Lock(), roService, new BlockLockConfig(5, 2, 0, 50));
            try {
                sessionService.registUser(mUdk, nu);
                MobileUsedPo mu=new MobileUsedPo();
                mu.setImei(mUdk.getDeviceId());
                mu.setStatus(1);
                mu.setPCDType(mUdk.getPCDType());
                mu.setUserId(nu.getUserId());
                muService.saveMobileUsed(mu);
            } finally {
                rLock.unlock();
                roService.close();
                roService=null;
            }
            //4-返回成功，若没有IMEI也返回成功
            map.put("ReturnType", "1001");
            map.put("UserId", nu.getUserId());
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
     * 第三方登录注册
     * @throws IOException
     */
    @RequestMapping(value="user/afterThirdAuth.do")
    @ResponseBody
    public Map<String,Object> afterThirdAuth(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.12-passport/user/afterThirdAuth");
        alPo.setObjType("003");//设置为用户
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
                if (StringUtils.isNullOrEmptyOrSpace(mUdk.getDeviceId())) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
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

            //1-获取业务参数
            //1.1-获取业务参数：第三方登录的分类：=1微信；=2QQ；=3微博
            String thirdType=(m.get("ThirdType")==null?null:m.get("ThirdType")+"");
            if (StringUtils.isNullOrEmptyOrSpace(thirdType)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获得第三方登录类型");
                return map;
            }
            //1.2-获取业务参数：用户的名称和Id
            String tuserId=(m.get("ThirdUserId")==null?null:m.get("ThirdUserId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(tuserId)) {
                map.put("ReturnType", "1000");
                map.put("Message", "无法获取第三方用户Id");
                return map;
            }
            String tuserName=(m.get("ThirdUserName")==null?null:m.get("ThirdUserName")+"");
            if (StringUtils.isNullOrEmptyOrSpace(tuserName)) {
                map.put("ReturnType", "1000");
                map.put("Message", "无法获取第三方用户名称");
                return map;
            }
            //1.3-获取业务参数：头像Url
            String tuserImg=(m.get("ThirdUserImg")==null?null:m.get("ThirdUserImg")+"");
            //1.4-获取业务参数：详细数据
            @SuppressWarnings("unchecked")
            Map<String, Object> tuserData=(Map<String, Object>)m.get("ThirdUserInfo");

            //2第三方登录
            Map<String, Object> rm=userService.thirdLogin(thirdType, tuserId, tuserName, tuserImg, tuserData);

            //3-成功后，自动登陆，处理Redis
            String _userId=((UserPo)rm.get("userInfo")).getUserId();
            mUdk.setUserId(_userId);
            RedisUserDeviceKey redisUdk=new RedisUserDeviceKey(mUdk);
            RedisOperService roService=new RedisOperService(redisConn, 4);
            ExpirableBlockKey rLock=RedisBlockLock.lock(redisUdk.getKey_Lock(), roService,
                    new BlockLockConfig(5, 2, 0, 50));
            try {
                sessionService.registUser(mUdk, (UserPo)rm.get("userInfo"));
                //3.2-保存使用情况
                MobileUsedPo mu=new MobileUsedPo();
                mu.setImei(mUdk.getDeviceId());
                mu.setStatus(1);
                mu.setUserId(_userId);
                mu.setPCDType(mUdk.getPCDType());
                muService.saveMobileUsed(mu);
            } finally {
                rLock.unlock();
                roService.close();
                roService=null;
            }

            //4设置返回值
            map.put("IsNew", "True");
            if ((Integer)rm.get("count")>1) map.put("IsNew", "False");
            map.put("UserInfo", ((UserPo)rm.get("userInfo")).toHashMap4Mobile());
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
     * 用户注销
     * @throws IOException
     */
    @RequestMapping(value="user/mlogout.do")
    @ResponseBody
    public Map<String,Object> mlogout(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.3-passport/user/mlogout");
        alPo.setObjType("003");//设置为用户
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
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/user/mlogout");
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

            //2-注销
            RedisUserDeviceKey redisUdk=new RedisUserDeviceKey(mUdk);
            RedisOperService roService=new RedisOperService(redisConn, 4);
            ExpirableBlockKey rLock=RedisBlockLock.lock(redisUdk.getKey_Lock(), roService, new BlockLockConfig(5, 2, 0, 50));
            try {
                sessionService.logoutSession(mUdk);
                //保存使用情况
                MobileUsedPo mu=new MobileUsedPo();
                mu.setImei(mUdk.getDeviceId());
                mu.setStatus(2);
                mu.setUserId(mUdk.getUserId());
                mu.setPCDType(mUdk.getPCDType());
                muService.saveMobileUsed(mu);
            } finally {
                rLock.unlock();
                roService.close();
                roService=null;
            }
            //3-返回成功，不管后台处理情况，总返回成功
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
     * 修改密码
     */
    @RequestMapping(value="user/updatePwd.do")
    @ResponseBody
    public Map<String,Object> updatePwd(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.4-passport/user/updatePwd");
        alPo.setObjType("003");//设置为用户
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
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/user/updatePwd");
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

            //2-获取其他参数
            String oldPwd=(m.get("OldPassword")==null?null:m.get("OldPassword")+"");
            String newPwd=(m.get("NewPassword")==null?null:m.get("NewPassword")+"");
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
            UserPo u=(UserPo)userService.getUserById(userId);
            if (u.getPassword().equals(oldPwd)) {
                Map<String, Object> updateInfo=new HashMap<String, Object>();
                updateInfo.put("userId",  userId);
                updateInfo.put("password",  newPwd);
                updateInfo=userService.updateUser(updateInfo);
                if (updateInfo.get("ReturnType").equals("1001")) map.put("ReturnType", "1001");
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
     * 修改密码，在通过手机号码找回密码时
     */
    @RequestMapping(value="user/updatePwd_AfterCheckPhoneOK.do")
    @ResponseBody
    public Map<String,Object> updatePwd_AfterCheckPhoneOK(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.5-passport/user/updatePwd_AfterCheckPhoneOK");
        alPo.setObjType("003");//设置为用户
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
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
                Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/user/updatePwd_AfterCheckPhoneOK");
                if (retM==null) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取会话信息");
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

            //1-获取其他参数
            String newPwd=(m.get("NewPassword")==null?null:m.get("NewPassword")+"");
            String errMsg="";
            if (StringUtils.isNullOrEmptyOrSpace(newPwd)) errMsg+=",新密码为空";
            if (!StringUtils.isNullOrEmptyOrSpace(errMsg)) {
                errMsg=errMsg.substring(1);
                map.put("ReturnType", "1003");
                map.put("Message", errMsg+",无法修改密码");
                return map;
            }
            UserPo up=userService.getUserById(m.get("RetrieveUserId")==null?null:(m.get("RetrieveUserId")+""));
            String info=null;
            RedisUserDeviceKey redisUdk=new RedisUserDeviceKey(mUdk);
            RedisOperService roService=new RedisOperService(redisConn, 4);
            try {
                String getValue=redisUdk.getKey_UserPhoneCheck();
                info=getValue==null?"":roService.get(getValue);
            } finally {
                roService.close();
                roService=null;
            }
            if (info.startsWith("OK")) {
                Map<String, Object> updateInfo=new HashMap<String, Object>();
                updateInfo.put("userId",  up.getUserId());
                updateInfo.put("password",  newPwd);
                updateInfo=userService.updateUser(updateInfo);
                if (updateInfo.get("ReturnType").equals("1001")) map.put("ReturnType", "1001");
                else {
                    map.put("ReturnType", "1004");
                    map.put("Message", "存储新密码失败");
                }
            } else {
                map.put("ReturnType", "1005");
                map.put("Message", "状态错误");
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
     * 获得用户详细信息
     */
    @RequestMapping(value="user/getUserInfo.do")
    @ResponseBody
    public Map<String,Object> getUserInfo(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.6-/passport/user/getUserInfo");
        alPo.setObjType("003");//设置为好友
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
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/user/getUserInfo");
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
            if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获得用户Id");
                return map;
            }
            //1-获得用户Id
            String _userId=(m.get("UserId")==null?null:m.get("UserId")+"");
            if (_userId!=null&&!userId.equals(_userId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "给定用户号和系统记录账号不匹配");
            } else {
                UserPo up=userService.getUserById(userId);
                if (up!=null) {
                    Map<String, Object> um=up.toDetailInfo();
                    List<DictRefRes> dictRefList=dictService.getDictRefs("plat_User", userId);
                    if (dictRefList!=null&&!dictRefList.isEmpty()) {
                        for (DictRefRes drr: dictRefList) {
                            if (drr.getDm().getId().equals("8")) {//性别
                                um.put("Sex", drr.getDd().getNodeName());
                            } else
                            if (drr.getDm().getId().equals("2")&&drr.getRefName().equals("地区")) {
                                um.put("Region", drr.getDd().getTreePathName());
                            }
                        }
                    }
                    if (up.getBirthday()!=null) um.put("Age", getAge(up.getBirthday().getTime())+"");
                    map.put("ReturnType", "1001");
                    map.put("UserInfo", um);
                } else {
                    map.put("ReturnType", "1011");
                    map.put("Message", "无对应的用户信息");
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
     * 随机获得可用的用户号码
     */
    @RequestMapping(value="user/getRandomUserNum.do")
    @ResponseBody
    public Map<String,Object> getRandomUserNum(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.7-passport/user/getRandomUserNum");
        alPo.setObjType("003");//设置为用户
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
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/user/getRandomUserNum");
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
     * 用手机号注册
     */
    @RequestMapping(value="user/registerByPhoneNum.do")
    @ResponseBody
    public Map<String,Object> registerByPhoneNum(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.8-passport/user/registerByPhoneNum");
        alPo.setObjType("003");//设置为用户
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

            //1-获取电话号码
            String phoneNum=(m.get("PhoneNum")==null?null:m.get("PhoneNum")+"");
            if (StringUtils.isNullOrEmptyOrSpace(phoneNum)) {
                map.put("ReturnType", "1000");
                map.put("Message", "无法获取手机号");
            }
            if (map.get("ReturnType")!=null) return map;

            //验证重复的手机号
            UserPo u=userService.getUserByPhoneNum(phoneNum);
            if (u==null) { //正确
                map.put("ReturnType", "1001");
                int random=SpiritRandom.getRandom(new Random(), 1000000, 1999999);
                String checkNum=(random+"").substring(1);
                String smsRetNum=SendSMS.sendSms(phoneNum, checkNum, "通过手机号注册用户");
                //向Session中加入验证信息
                RedisOperService roService=new RedisOperService(redisConn, 4);
                RedisUserDeviceKey redisUdk=new RedisUserDeviceKey(mUdk);
                try {
                    roService.set(redisUdk.getKey_UserPhoneCheck(), System.currentTimeMillis()+"::"+phoneNum+"::"+checkNum, "", 100*1000);
                } finally {
                    roService.close();
                    roService=null;
                }
                map.put("SmsRetNum", smsRetNum);
            } else {
                map.put("ReturnType", "1002");
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
     * 通过手机号找回密码
     */
    @RequestMapping(value="user/retrieveByPhoneNum.do")
    @ResponseBody
    public Map<String,Object> retrieveByPhoneNum(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.9-passport/user/retrieveByPhoneNum");
        alPo.setObjType("003");//设置为用户
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
                Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/user/retrieveByPhoneNum");
                if (retM==null) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取会话信息");
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

            //1-获取电话号码
            String phoneNum=(m.get("PhoneNum")==null?null:m.get("PhoneNum")+"");
            if (StringUtils.isNullOrEmptyOrSpace(phoneNum)) {
                map.put("ReturnType", "1000");
                map.put("Message", "无法获取手机号");
            }
            if (map.get("ReturnType")!=null) return map;

            //验证重复的手机号
            UserPo u=userService.getUserByPhoneNum(phoneNum);
            if (u!=null) { //正确
                map.put("ReturnType", "1001");
                int random=SpiritRandom.getRandom(new Random(), 1000000, 1999999);
                String checkNum=(random+"").substring(1);
                String smsRetNum=SendSMS.sendSms(phoneNum, checkNum, "通过绑定手机号找回密码");
                //向Session中加入验证信息
                RedisUserDeviceKey redisUdk=new RedisUserDeviceKey(mUdk);
                RedisOperService roService=new RedisOperService(redisConn, 4);
                try {
                    roService.set(redisUdk.getKey_UserPhoneCheck(), System.currentTimeMillis()+"::"+phoneNum+"::"+checkNum, "", 100*1000);
                } finally {
                    roService.close();
                    roService=null;
                }
                map.put("SmsRetNum", smsRetNum);
            } else {
                map.put("ReturnType", "1002");//该手机未绑定任何账户
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
     * 根据手机号码发送验证码
     */
    @RequestMapping(value="user/reSendPhoneCheckCode.do")
    @ResponseBody
    public Map<String,Object> reSendPhoneCheckCode(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.10-passport/user/reSendPhoneCheckCode");
        alPo.setObjType("003");//设置为用户
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
                Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/user/reSendPhoneCheckCode");
                if (retM==null) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取会话信息");
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

            //1-获取电话号码
            String phoneNum=(m.get("PhoneNum")==null?null:m.get("PhoneNum")+"");
            if (StringUtils.isNullOrEmptyOrSpace(phoneNum)) {
                map.put("ReturnType", "1000");
                map.put("Message", "无法获取手机号");
            }
            if (map.get("ReturnType")!=null) return map;
            //2-获取过程码
            int operType=-1;
            try {operType=Integer.parseInt(m.get("OperType")+"");} catch(Exception e) {}
            if (operType==-1) {
                map.put("ReturnType", "1000");
                map.put("Message", "无法获取过程码");
            }
            if (map.get("ReturnType")!=null) return map;

            RedisUserDeviceKey redisUdk=new RedisUserDeviceKey(mUdk);
            RedisOperService roService=new RedisOperService(redisConn, 4);
            String getValue=redisUdk.getKey_UserPhoneCheck();
            String info=(getValue==null?null:roService.get(getValue));

            if (info==null||info.equals("null")||info.startsWith("OK")) {//错误
                map.put("ReturnType", "1002");
                map.put("Message", "状态错误，未有之前的发送数据，无法重发");
            } else {//正确
                String[] _info=info.split("::");
                if (_info.length!=3) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "状态错误，数据格式不正确");
                } else {
                    String _phoneNum=_info[1];
                    if (!_phoneNum.equals(phoneNum)) {
                        map.put("ReturnType", "1002");
                        map.put("Message", "状态错误，手机号不匹配");
                    } else {
                        int random=SpiritRandom.getRandom(new Random(), 1000000, 1999999);
                        String checkNum=(random+"").substring(1);
                        String smsRetNum=SendSMS.sendSms(phoneNum, checkNum, operType==1?"通过手机号注册用户":"通过绑定手机号找回密码");
                        //向Session中加入验证信息
                        try {
                            roService.set(redisUdk.getKey_UserPhoneCheck(), System.currentTimeMillis()+"::"+phoneNum+"::"+checkNum, "", 100*1000);
                        } finally {
                            roService.close();
                            roService=null;
                        }
                        map.put("ReturnType", "1001");
                        map.put("SmsRetNum", smsRetNum);
                    }
                }
            }
            if (roService!=null) roService.close();
            roService=null;
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
     * 验证验证码
     */
    @RequestMapping(value="user/checkPhoneCheckCode.do")
    @ResponseBody
    public Map<String,Object> checkPhoneCheckCode(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.11-passport/user/checkPhoneCheckCode");
        alPo.setObjType("003");//设置为用户
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
                Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/user/checkPhoneCheckCode");
                if (retM==null) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取会话信息");
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

            //1-获取电话号码
            String phoneNum=(m.get("PhoneNum")==null?null:m.get("PhoneNum")+"");
            if (StringUtils.isNullOrEmptyOrSpace(phoneNum)) {
                map.put("ReturnType", "1000");
                map.put("Message", "无法获取手机号");
            }
            if (map.get("ReturnType")!=null) return map;
            //2-获取验证码
            String checkNum=(m.get("CheckCode")==null?null:m.get("CheckCode")+"");
            if (StringUtils.isNullOrEmptyOrSpace(checkNum)) {
                map.put("ReturnType", "1000");
                map.put("Message", "无法获取手机验证码");
            }
            if (map.get("ReturnType")!=null) return map;
            //3-获取是否需要得到用户id，只有在找回密码时才有用
            boolean needUserId=false;
            try {needUserId=Boolean.parseBoolean((m.get("NeedUserId")==null?"false":m.get("NeedUserId")+""));} catch(Exception e) {}

            //验证验证码
            RedisOperService roService=new RedisOperService(redisConn, 4);
            RedisUserDeviceKey redisUdk=new RedisUserDeviceKey(mUdk);
            String info=roService.get(redisUdk.getKey_UserPhoneCheck());
            if (info==null||info.equals("null")) {
                map.put("ReturnType", "1005");
                map.put("Message", "状态错误");
            } else {
                String[] _info=info.split("::");
                if (_info.length!=3) {
                    map.put("ReturnType", "1005");
                    map.put("Message", "状态错误，数据格式不正确");
                } else {
                    long time=Long.parseLong(_info[0]);
                    String _phoneNum=_info[1];
                    String _checkNum=_info[2];
                    if (System.currentTimeMillis()-time>(100*1000)) {
                        map.put("ReturnType", "1004");
                        map.put("Message", "超时");
                    } else if (!_phoneNum.equals(phoneNum)) {
                        map.put("ReturnType", "1003");
                        map.put("Message", "手机号不匹配");
                    } else if (!_checkNum.equals(checkNum)) {
                        map.put("ReturnType", "1002");
                        map.put("Message", "验证码不匹配");
                    } else {
                        try {
                            roService.set(redisUdk.getKey_UserPhoneCheck(), ("OK::"+_phoneNum), "", 100*1000);
                        } finally {
                            roService.close();
                            roService=null;
                        }
                        if (needUserId) {
                            UserPo u=userService.getUserByPhoneNum(phoneNum);
                            if (u!=null) map.put("UserId", u.getUserId());
                        }
                        map.put("ReturnType", "1001");
                    }
                }
            }
            if (roService!=null) roService.close();
            roService=null;
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
     * 得到所在用户组和我的好友列表
     */
    @RequestMapping(value="getGroupsAndFriends.do")
    @ResponseBody
    public Map<String,Object> getGroupsAndFriends(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.4.1-passport/getGroupsAndFriends");
        alPo.setObjType("005|009");//设置为好友
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
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/getGroupsAndFriends");
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
                    gm.put("GroupCount", g.get("groupCount"));
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
                    Map<String, Object> userViewM=u.toHashMap4Mobile();
                    //加入别名信息
                    UserAliasKey uak=new UserAliasKey("FRIEND", userId, u.getUserId());
                    UserAliasPo uap=uamm.getOneUserAlias(uak);
                    if (uap!=null) {
                        userViewM.put("UserAliasName", StringUtils.isNullOrEmptyOrSpace(uap.getAliasName())?u.getLoginName():uap.getAliasName());
                        userViewM.put("UserAliasDescn", uap.getAliasDescn());
                    }
                    if (!u.getUserId().equals(userId)) rul.add(userViewM);
                }
                topItem.put("Friends", rul);
            }
            map.put("FriendList", topItem);

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
     * 修改用户详细信息
     */
    @RequestMapping(value="user/updateUserInfo.do")
    @ResponseBody
    public Map<String,Object> updateUserInfo(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.13-/passport/user/updateUserInfo");
        alPo.setObjType("003");//设置为好友
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
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/user/updateUserInfo");
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
            if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获得用户Id");
                return map;
            }
            String _userId=(m.get("UserId")==null?null:m.get("UserId")+"");
            if (_userId!=null&&!userId.equals(_userId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "给定用户号和系统记录账号不匹配");
                return map;
            }
            //1-获得用户Id
            Map<String, Object> updateInfo=new HashMap<String, Object>();
            String nickName=(m.get("NickName")==null?null:m.get("NickName")+"");
            String userSign=(m.get("UserSign")==null?null:m.get("UserSign")+"");
            String sex=(m.get("SexDictId")==null?null:m.get("SexDictId")+"");
            String region=(m.get("RegionDictId")==null?null:m.get("RegionDictId")+"");
            String birthday=(m.get("Birthday")==null?null:m.get("Birthday")+"");
            String starSign=(m.get("StarSign")==null?null:m.get("StarSign")+"");
            String email=(m.get("MailAddr")==null?null:m.get("MailAddr")+"");
            String userNum=(m.get("UserNum")==null?null:m.get("UserNum")+"");
            String phoneNum=(m.get("PhoneNum")==null?null:m.get("PhoneNum")+"");
            updateInfo.put("userId", userId);
            if (!StringUtils.isNullOrEmptyOrSpace(nickName)) updateInfo.put("nickName", nickName);
            if (!StringUtils.isNullOrEmptyOrSpace(userSign)) updateInfo.put("userSign", userSign);
            if (!StringUtils.isNullOrEmptyOrSpace(sex)) updateInfo.put("sex", sex);
            if (!StringUtils.isNullOrEmptyOrSpace(region)) updateInfo.put("region", region);
            if (!StringUtils.isNullOrEmptyOrSpace(birthday)) {
                Timestamp t=null;
                try {t=new Timestamp(Long.parseLong(birthday));} catch(Exception e) {}
                if (t!=null) updateInfo.put("birthday", t);
            }
            if (!StringUtils.isNullOrEmptyOrSpace(starSign)) updateInfo.put("starSign", starSign);
            if (!StringUtils.isNullOrEmptyOrSpace(email)) updateInfo.put("email", email);
            if (!StringUtils.isNullOrEmptyOrSpace(userNum)) updateInfo.put("userNum", userNum);
            if (!StringUtils.isNullOrEmptyOrSpace(phoneNum)) updateInfo.put("phoneNum", phoneNum);

            updateInfo=userService.updateUser(updateInfo);
            if (updateInfo==null) {
                map.put("ReturnType", "1004");
                map.put("Message", "找不到对应的用户");
            } else {
                map.putAll(updateInfo);
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
     * 判断用户号
     */
    @RequestMapping(value="user/decideUserNum.do")
    @ResponseBody
    public Map<String,Object> decideUserNum(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("2.1.14-/passport/user/decideUserNum");
        alPo.setObjType("003");//设置为好友
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
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "passport/user/decideUserNum");
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
            if (StringUtils.isNullOrEmptyOrSpace(userId)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获得用户Id");
                return map;
            }
            String _userId=(m.get("UserId")==null?null:m.get("UserId")+"");
            if (_userId!=null&&!userId.equals(_userId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "给定用户号和系统记录账号不匹配");
                return map;
            }
            //1-获得用户号
            String userNum=(m.get("UserNum")==null?null:m.get("UserNum")+"");
            if (StringUtils.isNullOrEmptyOrSpace(userNum)) {
                map.put("ReturnType", "1004");
                map.put("Message", "无法获得用户号");
            } else {
                int flag=userService.decideUserNum(userId, userNum);
                if (flag==1) {
                    map.put("ReturnType", "1001");
                } else if (flag==2) {
                    map.put("ReturnType", "1005");
                    map.put("Message", "该用户已存在用户号，不能再编辑");
                } else if (flag==3) {
                    map.put("ReturnType", "1006");
                    map.put("Message", "该用户号重复");
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

    /*
     * 获得新的用户编码
     * @return
     */
    private int getNewUserNumber() {
        return SpiritRandom.getRandom(new Random(), 0, 999999);
    }

    private int getAge(long timestamp) {
        int age=0;
        Calendar born=Calendar.getInstance();
        Calendar now=Calendar.getInstance();
        now.setTime(new Date());
        born.setTimeInMillis(timestamp);
        if (born.after(now)) throw new IllegalArgumentException("生日不能大于今日");
        age=now.get(Calendar.YEAR) - born.get(Calendar.YEAR);
        if (now.get(Calendar.DAY_OF_YEAR) < born.get(Calendar.DAY_OF_YEAR)) age -= 1;
        return age;
    } 
}