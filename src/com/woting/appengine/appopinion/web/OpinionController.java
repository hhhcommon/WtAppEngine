package com.woting.appengine.appopinion.web;

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
import com.woting.appengine.appopinion.persistence.pojo.AppOpinionPo;
import com.woting.appengine.appopinion.persistence.pojo.AppReOpinionPo;
import com.woting.appengine.appopinion.service.AppOpinionService;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.mobile.session.model.MobileSession;

@Controller
@RequestMapping(value="/opinion/app/")
public class OpinionController {
    @Resource
    private AppOpinionService opinionsService;

    /**
     * 提交所提意见
     * @param request
     * @return
     */
    @RequestMapping(value="commit.do")
    @ResponseBody
    public Map<String,Object> commit(HttpServletRequest request) {
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
                if (map.get("ReturnType")==null&&StringUtils.isNullOrEmptyOrSpace(userId)) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "无法获取用户Id");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //2-获取意见
            String opinion=(String)m.get("Opinion");
            if (StringUtils.isNullOrEmptyOrSpace(opinion)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取意见");
                return map;
            }
            //3-存储意见
            try {
                AppOpinionPo po=new AppOpinionPo();
                po.setImei(ms.getKey().getMobileId());
                po.setUserId(userId);
                po.setOpinion(opinion);
                //是否重复提交意见
                List<AppOpinionPo> duplicates = opinionsService.getDuplicates(po);
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
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="getList.do")
    @ResponseBody
    public Map<String,Object> getList(HttpServletRequest request) {
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
                    map.put("Message", "还未登录");
                } else {
                    ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                    if (ms.getKey().isUser()) userId=ms.getKey().getUserId();
                }
                if (map.get("ReturnType")==null&&StringUtils.isNullOrEmptyOrSpace(userId)) {
                    map.put("ReturnType", "1002");
                    map.put("Message", "无法获取用户Id");
                }
            }
            if (map.get("ReturnType")!=null) return map;

            List<AppOpinion> ol = opinionsService.getOpinionsByOnwerId(userId, ms.getKey().getMobileId());
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
            map.put("Message", e.getMessage());
            return map;
        }
    }

    private List<Map<String, Object>> convertAppOpinon4View(List<AppOpinion> ol) {
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
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
                rel = new ArrayList<Map<String, Object>>();
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