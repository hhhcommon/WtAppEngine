package com.woting.version.mobile.web;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spiritdata.framework.util.StringUtils;
import com.woting.dataanal.gather.API.ApiGatherUtils;
import com.woting.dataanal.gather.API.mem.ApiGatherMemory;
import com.woting.dataanal.gather.API.persis.pojo.ApiLogPo;
import com.woting.passport.mobile.MobileParam;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.DeviceType;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.RequestUtils;
import com.woting.version.core.model.Version;
import com.woting.version.core.service.VersionService;

@Lazy(true)
@Controller
public class VersionController {
    @Resource
    private VersionService verService;

    @RequestMapping(value="/common/getVersion.do")
    @ResponseBody
    public Map<String,Object> getVersion(HttpServletRequest request) {//不需要登录
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("1.1.3-common/getVersion");
        alPo.setObjType("P002");//设置为版本
        alPo.setDealFlag(1);//处理成功

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m!=null&&m.size()>0) {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                MobileUDKey mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) map.putAll(mUdk.toHashMapAsBean());
                //数据收集处理==2
                alPo.setOwnerType(201);
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
                //1-获取版本号
                String version=m.get("Version")==null?null:m.get("Version")+"";
                if (StringUtils.isNullOrEmptyOrSpace(version)) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "无法获得版本号信息");
                    return map;
                }

                //2-获取版本
                Version v=verService.getVersion(version);
                if (v==null) {
                    map.put("ReturnType", "1011");
                    map.put("Message", "该版本号无对应版本信息");
                } else {
                    map.put("ReturnType", "1001");
                    map.put("VersionInfo", v.toViewMap4App());
                }
            }
            map.put("ServerStatus", "1");
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

    @RequestMapping(value="/common/judgeVersion.do")
    @ResponseBody
    public Map<String,Object> judgeVersion(HttpServletRequest request) {//不需要登录
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("1.1.2-common/judgeVersion");
        alPo.setObjType("P002");//设置为版本
        alPo.setDealFlag(1);//处理成功

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            if (m!=null&&m.size()>0) {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                MobileUDKey mUdk=mp.getUserDeviceKey();
                if (mUdk!=null) map.putAll(mUdk.toHashMapAsBean());
                //数据收集处理==2
                alPo.setOwnerType(201);
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

                //1-获取App版本号
                String version=m.get("Version")==null?null:(m.get("Version")+"");
                if (StringUtils.isNullOrEmptyOrSpace(version)) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "无法获得版本号信息");
                    return map;
                }

                //2-判断并获取版本
                m=verService.judgeVersion(version);
                if (m==null) {
                    map.put("ReturnType", "1003");
                    map.put("Message", "无法获得当前发布版本");
                } else {
                    map.put("ReturnType", "1001");
                    map.putAll(m);
                }
            }
            map.put("ServerStatus", "1");
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
}