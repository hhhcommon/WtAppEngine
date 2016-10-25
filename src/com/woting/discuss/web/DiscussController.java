package com.woting.discuss.web;

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
import com.woting.cm.core.media.MediaType;
import com.woting.dataanal.gather.API.ApiGatherUtils;
import com.woting.dataanal.gather.API.mem.ApiGatherMemory;
import com.woting.dataanal.gather.API.persis.pojo.ApiLogPo;
import com.woting.discuss.model.Discuss;
import com.woting.discuss.service.DiscussService;
import com.woting.passport.UGA.persis.pojo.UserPo;
import com.woting.passport.UGA.service.UserService;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.RequestUtils;
import com.woting.passport.mobile.MobileParam;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.DeviceType;
import com.woting.passport.session.SessionService;

@Controller
@RequestMapping(value="/discuss/")
public class DiscussController {
    @Resource
    private DiscussService discussService;
    @Resource
    private UserService userService;
    @Resource(name="redisSessionService")
    private SessionService sessionService;

    /**
     * 提交文章评论，注意这里不允许过客进行访问
     * @param request
     * @return
     */
    @RequestMapping(value="add.do")
    @ResponseBody
    public Map<String,Object> add(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.4.1-discuss/add");
        alPo.setObjType("028");//设置为评论
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
                mUdk=MobileParam.build(m).getUserDeviceKey();
                if (mUdk!=null) {
                    if (StringUtils.isNullOrEmptyOrSpace(mUdk.getDeviceId())&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) { //是PC端来的请求
                        mUdk.setDeviceId(request.getSession().getId());
                    }
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "discuss/add");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");                    
                    } else if (!(retM.get("ReturnType")+"").equals("1001")) {
                        map.putAll(retM);
                    } else {
                        map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
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
            
            //1-获取意见
            String opinion=(m.get("Opinion")==null?null:m.get("Opinion")+"");
            if (StringUtils.isNullOrEmptyOrSpace(opinion)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法评论内容");
                return map;
            }
            //2-获得内容分类
            String mediaType=(m.get("MediaType")==null?null:m.get("MediaType")+"");
            if (StringUtils.isNullOrEmptyOrSpace(mediaType)) {
                map.put("ReturnType", "1004");
                map.put("Message", "无法获取内容分类");
                return map;
            }
            if (MediaType.buildByTypeName(mediaType)==MediaType.ERR) {
                map.put("ReturnType", "1004");
                map.put("Message", "无法获取内容分类");
                return map;
            }
            //2-获取内容Id
            String contentId=(m.get("ContentId")==null?null:m.get("ContentId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(contentId)) {
                map.put("ReturnType", "1005");
                map.put("Message", "无法获取内容Id");
                return map;
            }
            //4-存储意见
            try {
                Discuss discuss=new Discuss();
                discuss.setUserId(userId);
                discuss.setOpinion(opinion);
                discuss.setResTableName((MediaType.buildByTypeName(mediaType)).getTabName());
                discuss.setResId(contentId);
                //是否重复提交意见
                List<Discuss> duplicates=discussService.getDuplicates(discuss);
                if (duplicates!=null&&duplicates.size()>0) {
                    map.put("ReturnType", "1006");
                    map.put("Message", "该评论已经提交");
                    return map;
                };
                int r=discussService.insertDiscuss(discuss);
                if (r!=1) {
                    map.put("ReturnType", "1007");
                    map.put("Message", "加入失败");
                    return map;
                }
            } catch(Exception ei) {
                map.put("ReturnType", "1007");
                map.put("Message", StringUtils.getAllMessage(ei));
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

    /**
     * 删除文章评论
     * @param request
     * @return
     */
    @RequestMapping(value="del.do")
    @ResponseBody
    public Map<String,Object> del(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.4.2-discuss/del");
        alPo.setObjType("028");//设置为评论
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
                mUdk=MobileParam.build(m).getUserDeviceKey();
                if (mUdk!=null) {
                    if (StringUtils.isNullOrEmptyOrSpace(mUdk.getDeviceId())&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) { //是PC端来的请求
                        mUdk.setDeviceId(request.getSession().getId());
                    }
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "discuss/add");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");                    
                    } else if (!(retM.get("ReturnType")+"").equals("1001")) {
                        map.putAll(retM);
                    } else {
                        map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
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

            //1-评论Id
            String discussId=(m.get("DiscussId")==null?null:m.get("DiscussId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(discussId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取评论Id");
                return map;
            }
            //2-删除意见
            try {
                Discuss discuss=new Discuss();
                discuss.setUserId(userId);
                discuss.setId(discussId);
                int flag=discussService.delDiscuss(discuss);
                if (flag==-1) {
                    map.put("ReturnType", "1004");
                    map.put("Message", "无对应评论，无法删除");
                } else if (flag==-2) {
                    map.put("ReturnType", "1005");
                    map.put("Message", "无权删除");
                } else if (flag==0) {
                    map.put("ReturnType", "1006");
                    map.put("Message", "删除失败");
                } else {
                    map.put("ReturnType", "1001");
                }
           } catch(Exception ei) {
                map.put("ReturnType", "1006");
                map.put("Message", StringUtils.getAllMessage(ei));
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

    @RequestMapping(value="article/getList.do")
    @ResponseBody
    public Map<String,Object> getArticleList(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.4.3-discuss/article/getList");
        alPo.setObjType("001");//设置为内容
        alPo.setDealFlag(1);//处理成功

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
                mUdk=MobileParam.build(m).getUserDeviceKey();
                if (mUdk!=null) {
                    if (StringUtils.isNullOrEmptyOrSpace(mUdk.getDeviceId())&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) { //是PC端来的请求
                        mUdk.setDeviceId(request.getSession().getId());
                    }
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "discuss/article/getList");
                    map.putAll(retM);
                    map.remove("ReturnType");
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

            //1-获得内容分类
            String mediaType=(m.get("MediaType")==null?null:m.get("MediaType")+"");
            if (StringUtils.isNullOrEmptyOrSpace(mediaType)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取内容分类");
                return map;
            }
            if (MediaType.buildByTypeName(mediaType)==MediaType.ERR) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取内容分类");
                return map;
            }
            //2-获取内容Id
            String contentId=(m.get("ContentId")==null?null:m.get("ContentId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(contentId)) {
                map.put("ReturnType", "1004");
                map.put("Message", "无法获取内容Id");
                return map;
            }
            //3-获取是否发布
            int isPub=1;
            try {
                isPub=Integer.parseInt(m.get("IsPub")==null?null:m.get("IsPub")+"");
            } catch(Exception e) {
            }
            //4-获取分页信息
            int page=-1;
            try {
                page=Integer.parseInt(m.get("Page")==null?null:m.get("Page")+"");
            } catch(Exception e) {
            }
            int pageSize=10;
            try {
                pageSize=Integer.parseInt(m.get("PageSize")==null?null:m.get("PageSize")+"");
            } catch(Exception e) {
            }

            Map<String, Object> ol=discussService.getArticleDiscusses(MediaType.buildByTypeName(mediaType), contentId, isPub, page, pageSize);
            if (ol!=null&&ol.size()>0) {
                map.put("ReturnType", "1001");
                map.put("AllCount", ol.get("AllCount"));
                map.put("OpinionList", convertDiscissView((List<Discuss>)(ol.get("List"))));
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无评论信息");
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
     * 获得返回的列表，包括用户的信息
     * @param ol
     * @return
     */
    private List<Map<String, Object>> convertDiscissView(List<Discuss> disList) {
        //得到用户列表
        List<String> userIds=new ArrayList<String>();
        Map<String, String> userMap=new HashMap<String, String>();
        for (Discuss d: disList) {
            if (d.getUserId()!=null||!d.getUserId().equals("0")) {
                userMap.put(d.getUserId(), d.getUserId());
            }
        }
        if (userMap!=null&&!userMap.isEmpty()) {
            for (String key: userMap.keySet()) {
                userIds.add(key);
            }
        }

        List<UserPo> ul=userService.getUserByIds(userIds);
        List<Map<String, Object>> ret=new ArrayList<Map<String, Object>>();

        Map<String, Object> oneDiscuss=null;
        for (Discuss d: disList) {
            oneDiscuss=d.toHashMap4Mobile();
            if (oneDiscuss!=null) {
                if (d.getUserId()!=null||!d.getUserId().equals("0")) {
                    for (UserPo up: ul) {
                        if (up.getUserId().equals(d.getUserId())) {
                            oneDiscuss.put("UserInfo", up.toHashMap4Mobile());
                            break;
                        }
                    }
                }
                ret.add(oneDiscuss);
            }
        }
        return ret==null||ret.isEmpty()?null:ret;
    }

    @RequestMapping(value="user/getList.do")
    @ResponseBody
    public Map<String,Object> getUserList(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.4.4-discuss/user/getList");
        alPo.setObjType("001");//设置为内容
        alPo.setDealFlag(1);//处理成功

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
                mUdk=MobileParam.build(m).getUserDeviceKey();
                if (mUdk!=null) {
                    if (StringUtils.isNullOrEmptyOrSpace(mUdk.getDeviceId())&&DeviceType.buildDtByPCDType(mUdk.getPCDType())==DeviceType.PC) { //是PC端来的请求
                        mUdk.setDeviceId(request.getSession().getId());
                    }
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "discuss/add");
                    if ((retM.get("ReturnType")+"").equals("2003")) {
                        map.put("ReturnType", "200");
                        map.put("Message", "需要登录");                    
                    } else if (!(retM.get("ReturnType")+"").equals("1001")) {
                        map.putAll(retM);
                    } else {
                        map.remove("ReturnType");
                    }
                    userId=retM.get("UserId")==null?null:retM.get("UserId")+"";
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

            //1-获得内容分类
            String mediaTypes=(m.get("MediaType")==null?null:m.get("MediaType")+"");
            if (StringUtils.isNullOrEmptyOrSpace(mediaTypes)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获取内容分类");
                return map;
            }
            String[] ms=mediaTypes.split(",");
            List<MediaType> ml=new ArrayList<MediaType>();
            for (String oneMt: ms) {
                if (MediaType.buildByTypeName(oneMt)!=MediaType.ERR) {
                    ml.add(MediaType.buildByTypeName(oneMt));
                }
            }
            if (ml.isEmpty()) ml=null;
            //2-获取内容Id
            int isPub=1;
            try {
                isPub=Integer.parseInt(m.get("IsPub")==null?null:m.get("IsPub")+"");
            } catch(Exception e) {
            }
            //3-获取分页信息
            int page=-1;
            try {
                page=Integer.parseInt(m.get("Page")==null?null:m.get("Page")+"");
            } catch(Exception e) {
            }
            int pageSize=10;
            try {
                pageSize=Integer.parseInt(m.get("PageSize")==null?null:m.get("PageSize")+"");
            } catch(Exception e) {
            }
            //4-获取分页信息
            int resultType=1;
            try {
                resultType=Integer.parseInt(m.get("ResultType")==null?null:m.get("ResultType")+"");
            } catch(Exception e) {
            }

            Map<String, Object> al=discussService.getUserDiscusses(userId, ml, isPub, page, pageSize, resultType);

            if (al!=null&&al.size()>0) {
                map.put("ReturnType", "1001");
                map.put("AllCount", al.get("AllCount"));
                map.put("ContentList", al.get("List"));
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无用户评论列表");
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
}