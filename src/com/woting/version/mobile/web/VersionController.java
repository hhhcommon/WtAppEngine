package com.woting.version.mobile.web;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spiritdata.framework.util.StringUtils;
import com.woting.passport.mobile.MobileParam;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.SessionService;
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
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m!=null&&m.size()>0) {
                MobileUDKey mUdk=MobileParam.build(m).getUserDeviceKey();
                if (mUdk!=null) map.putAll(mUdk.toHashMapAsBean());

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
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="/common/judgeVersion.do")
    @ResponseBody
    public Map<String,Object> judgeVersion(HttpServletRequest request) {//不需要登录
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m!=null&&m.size()>0) {
                MobileUDKey mUdk=MobileParam.build(m).getUserDeviceKey();
                if (mUdk!=null) map.putAll(mUdk.toHashMapAsBean());

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
            map.put("Message", e.getMessage());
            return map;
        }
    }
}