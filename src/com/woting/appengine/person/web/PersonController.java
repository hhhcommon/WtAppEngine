package com.woting.appengine.person.web;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.RequestUtils;
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.person.service.PersonProService;
import com.woting.dataanal.gather.API.ApiGatherUtils;
import com.woting.dataanal.gather.API.mem.ApiGatherMemory;
import com.woting.dataanal.gather.API.persis.pojo.ApiLogPo;
import com.woting.passport.mobile.MobileParam;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.DeviceType;

@Controller
@RequestMapping(value="/person/")
public class PersonController {
	@Resource
	private PersonProService personProService;

	@RequestMapping(value="getPersonInfo.do")
    @ResponseBody
    public Map<String,Object> getPersonInfo(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.6.1-person/getPersonInfo.do");
        alPo.setObjType("001");//内容对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            MobileUDKey mUdk=null;
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                map.putAll(mUdk.toHashMapAsBean());
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
   
            //1-得到系列内容的Id
            String personId=(m.get("PersonId")==null?null:m.get("PersonId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(personId)) {
                map.put("ReturnType", "1012");
                map.put("Message", "无法获得主播Id");
                return map;
            }
            String seqMediaSize = m.get("SeqMediaSize")==null?null:m.get("SeqMediaSize")+"";
            if (StringUtils.isNullOrEmptyOrSpace(seqMediaSize) || seqMediaSize.equals("null")) {
            	seqMediaSize = "3";
			}
            String mediaAssetSize = m.get("MediaAssetSize")==null?null:m.get("MediaAssetSize")+"";
            if (StringUtils.isNullOrEmptyOrSpace(mediaAssetSize) || mediaAssetSize.equals("null")) {
            	mediaAssetSize = "10";
			}
            
            map = personProService.getPersonInfo(personId, seqMediaSize, mediaAssetSize);//contentService.BcProgrammes(bcId, requestTimesstr);
            if (map!=null&&map.size()>0) {
                map.put("ReturnType", "1001");
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无主播信息");
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
	
	@RequestMapping(value="getPersonContents.do")
    @ResponseBody
    public Map<String,Object> getPersonContents(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.6.2-person/getPersonContents.do");
        alPo.setObjType("002");//内容对象
        alPo.setDealFlag(1);//处理成功
        alPo.setOwnerType(201);
        alPo.setOwnerId("--");

        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            alPo.setReqParam(JsonUtils.objToJson(m));
            MobileUDKey mUdk=null;
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                MobileParam mp=MobileParam.build(m);
                if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&DeviceType.buildDtByPCDType(StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())?-1:Integer.parseInt(mp.getPCDType()))==DeviceType.PC) { //是PC端来的请求
                    mp.setImei(request.getSession().getId());
                }
                mUdk=mp.getUserDeviceKey();
                map.putAll(mUdk.toHashMapAsBean());
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
   
            //1-得到系列内容的Id
            String personId=(m.get("PersonId")==null?null:m.get("PersonId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(personId)) {
                map.put("ReturnType", "1012");
                map.put("Message", "无法获得主播Id");
                return map;
            }
            String pageSize = m.get("PageSize")==null?null:m.get("PageSize")+"";
            if (StringUtils.isNullOrEmptyOrSpace(pageSize) || pageSize.equals("null")) {
            	pageSize = "10";
			}
            String page = m.get("Page")==null?null:m.get("Page")+"";
            if (StringUtils.isNullOrEmptyOrSpace(page) || page.equals("null")) {
            	page = "1";
			}
            String mediaType = m.get("MediaType")==null?null:m.get("MediaType")+"";
            if (StringUtils.isNullOrEmptyOrSpace(mediaType) || mediaType.equals("null")) {
            	mediaType = "SEQU";
			}
            String orderBy = m.get("OrderBy")==null?null:m.get("OrderBy")+"";
            if (StringUtils.isNullOrEmptyOrSpace(orderBy) || orderBy.equals("null")) {
            	orderBy = "1";
			}
            List<Map<String, Object>> bcps = personProService.getPersonContents(personId,mediaType,page,pageSize,orderBy);//contentService.BcProgrammes(bcId, requestTimesstr);
            if (bcps!=null&&bcps.size()>0) {
                map.put("ResultList", bcps);
                map.put("ReturnType", "1001");
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "无主播信息");
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
