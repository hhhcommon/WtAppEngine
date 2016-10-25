package com.woting.appengine.appopinion.web;

import java.sql.Timestamp;
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
import com.woting.appengine.appopinion.model.AppOpinion;
import com.woting.appengine.appopinion.persis.pojo.AppOpinionPo;
import com.woting.appengine.appopinion.persis.pojo.AppReOpinionPo;
import com.woting.appengine.appopinion.service.AppOpinionService;
import com.woting.dataanal.gather.API.ApiGatherUtils;
import com.woting.dataanal.gather.API.mem.ApiGatherMemory;
import com.woting.dataanal.gather.API.persis.pojo.ApiLogPo;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.RequestUtils;
import com.woting.passport.mobile.MobileParam;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.DeviceType;
import com.woting.passport.session.SessionService;

@Controller
@RequestMapping(value="/opinion/app/")
public class OpinionController {
    @Resource
    private AppOpinionService opinionsService;
    @Resource(name="redisSessionService")
    private SessionService sessionService;

    /**
     * 提交所提意见
     * @param request
     * @return
     */
    @RequestMapping(value="commit.do")
    @ResponseBody
    public Map<String,Object> commit(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("3.1.1-opinion/app/commit");
        alPo.setObjType("013");//设置为意见
        alPo.setDealFlag(1);//处理成功

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
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "opinion/app/commit");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");                    
                    } else if (!(retM.get("ReturnType")+"").equals("1001")) {
                        map.putAll(retM);
                    } else {
                        map.remove("ReturnType");
                    }
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
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
            if (map.get("ReturnType")!=null) return map;

            //2-获取意见
            String opinion=(m.get("Opinion")==null?null:m.get("Opinion")+"");
            if (StringUtils.isNullOrEmptyOrSpace(opinion)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取意见");
                return map;
            }
            //3-存储意见
            try {
                AppOpinionPo po=new AppOpinionPo();
                po.setImei(mUdk.getDeviceId());
                po.setUserId(userId);
                po.setOpinion(opinion);
                //是否重复提交意见
                List<AppOpinionPo> duplicates=opinionsService.getDuplicates(po);
                if (duplicates!=null&&duplicates.size()>0) {
                    map.put("ReturnType", "1005");
                    map.put("Message", "该意见已经提交");
                    return map;
                };
                opinionsService.insertOpinion(po);
            } catch(Exception ei) {
                map.put("ReturnType", "1004");
                map.put("Message", ei.getMessage());
                return map;
            }
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

    @RequestMapping(value="getList.do")
    @ResponseBody
    public Map<String,Object> getList(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("3.1.2-opinion/app/getList");
        alPo.setObjType("013");//设置为意见
        alPo.setDealFlag(1);//处理成功

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
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "opinion/app/getList");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");                    
                    } else if (!(retM.get("ReturnType")+"").equals("1001")) {
                        map.putAll(retM);
                    } else {
                        map.remove("ReturnType");
                    }
                } else {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取需要的参数");
                }
            }
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
            if (map.get("ReturnType")!=null) return map;

            List<AppOpinion> ol=opinionsService.getOpinionsByOnwerId(userId, mUdk.getDeviceId());
            if (ol!=null&&ol.size()>0) {
                map.put("ReturnType", "1001");
                map.put("OpinionList", convertAppOpinon4View(ol));
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无意见及反馈信息");
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

    private List<Map<String, Object>> convertAppOpinon4View(List<AppOpinion> ol) {
        List<Map<String, Object>> ret=new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> rel=null;
        List<AppReOpinionPo> reApl=null;
        Map<String, Object> selfOpinion=null, reOpinion=null;
        for (AppOpinion ap: ol) {
            selfOpinion=new HashMap<String, Object>();
            selfOpinion.put("OpinionId", ap.getId());
            selfOpinion.put("Opinion", ap.getOpinion());
            selfOpinion.put("OpinionTime", ap.getCTime().getTime());
            reApl=ap.getReList();
            if (reApl!=null&&reApl.size()>0) {
                rel=new ArrayList<Map<String, Object>>();
                for (AppReOpinionPo aro: reApl) {
                    reOpinion=new HashMap<String, Object>();
                    reOpinion.put("OpinionReId", aro.getId());
                    reOpinion.put("ReOpinion", aro.getReOpinion());
                    reOpinion.put("ReOpinionTime", aro.getCTime().getTime());
                    rel.add(reOpinion);
                }
                selfOpinion.put("ReList", rel);
            }
            ret.add(selfOpinion);
        }
        return ret;
    }
}