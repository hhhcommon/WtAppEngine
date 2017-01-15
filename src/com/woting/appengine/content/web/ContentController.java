package com.woting.appengine.content.web;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.util.StringUtils;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.ext.spring.redis.RedisOperService;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.RequestUtils;
import com.woting.appengine.content.service.ContentService;
import com.woting.appengine.content.utils.ContentRedisUtils;
import com.woting.appengine.searchcrawler.utils.SearchUtils;
import com.woting.dataanal.gather.API.ApiGatherUtils;
import com.woting.dataanal.gather.API.mem.ApiGatherMemory;
import com.woting.dataanal.gather.API.persis.pojo.ApiLogPo;
import com.woting.favorite.service.FavoriteService;
import com.woting.passport.mobile.MobileParam;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.DeviceType;
import com.woting.passport.session.SessionService;

@Controller
@RequestMapping(value="/content/")
public class ContentController {
    @Resource
    private ContentService contentService;
    @Resource
    private FavoriteService favoriteService;
    @Resource(name="redisSessionService")
    private SessionService sessionService;

    /**
     * 获得标题图
     * @param request
     * @return 标题图格式
     */
    @RequestMapping(value="getLoopImgs.do")
    @ResponseBody
    public Map<String,Object> getLoopImgs(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.2.3-content/getLoopImgs");
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

            //1-获取列表
            List<Map<String, Object>> sl=null;
            Map<String, Object> bcClass=null, bcItem=null;
            bcClass=new HashMap<String, Object>();//一个分类
            bcClass.put("CatalogType", "001");
            bcClass.put("CatalogId", "002");
            bcClass.put("CatalogName", "娱乐");
            bcClass.put("CatalogImg", "a.jpg");
            bcClass.put("PageSize", "3");//当前列表元素个数
            bcClass.put("AllListSize", "3");//本分类列表元素个数
            //------------------
            sl=new ArrayList<Map<String, Object>>();
            bcItem=new HashMap<String, Object>();
            bcItem.put("ImgUrl", "asset/contents/imgs/1280115949992.jpg");
            bcItem.put("ImgIdx", "0");
            bcItem.put("ImgDescn", "北京怀旧金曲");
            bcItem.put("ImgContentUrl", "abc/content.do?contentId=1");
            sl.add(bcItem);
            bcItem.put("ImgUrl", "asset/contents/imgs/1303967876491.jpg");
            bcItem.put("ImgIdx", "1");
            bcItem.put("ImgDescn", "故事新编");
            bcItem.put("ImgContentUrl", "abc/content.do?contentId=2");
            sl.add(bcItem);
            bcItem.put("ImgUrl", "asset/contents/imgs/m_1303967844788.jpg");
            bcItem.put("ImgIdx", "2");
            bcItem.put("ImgDescn", "新闻纵览");
            bcItem.put("ImgContentUrl", "abc/content.do?contentId=3");
            sl.add(bcItem);
            bcItem.put("ImgUrl", "asset/contents/imgs/m_1303967870670.jpg");
            bcItem.put("ImgIdx", "3");
            bcItem.put("ImgDescn", "体坛风云");
            bcItem.put("ImgContentUrl", "abc/content.do?contentId=3");
            sl.add(bcItem);
           bcClass.put("SubList", sl);
            //------------------
            map.put("ReturnType", "1001");
            map.put("ResultList", bcClass);
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
     * 获得内容（某栏目或分类下的内容）
     * @param request
     * @return 所获得的内容，包括分页
     */
    @RequestMapping(value="getContents.do")
    @ResponseBody
    public Map<String,Object> getContents(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.2.1-content/getContents");
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
                if (mUdk!=null) map.putAll(mUdk.toHashMapAsBean());
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

            //1-得到模式Id
            String catalogType=(m.get("CatalogType")==null?null:m.get("CatalogType")+"");
            if (StringUtils.isNullOrEmptyOrSpace(catalogType)) catalogType="-1";
            //2-得到字典项Id或父栏目Id
            String catalogId=(m.get("CatalogId")==null?null:m.get("CatalogId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(catalogId)) catalogId=null;
            //3-得到返回类型
            int resultType=3;
            try {resultType=Integer.parseInt(m.get("ResultType")+"");} catch(Exception e) {}
            //4-得到类型
            String mediaType=(m.get("MediaType")==null?null:m.get("MediaType")+"");
            if (StringUtils.isNullOrEmptyOrSpace(mediaType)) mediaType=null;
            //5-得到每分类条目数
            int perSize=3;
            try {perSize=Integer.parseInt(m.get("PerSize")+"");} catch(Exception e) {}
            //6-得到每页条数
            int pageSize=10;
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};
            //7-得到页数
            int page=1;
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};
            //8-得到开始分类Id
            String beginCatalogId=(m.get("BeginCatalogId")==null?null:m.get("BeginCatalogId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(beginCatalogId)) beginCatalogId=null;
            //9-获得页面类型
            int pageType=1;
            try {pageType=Integer.parseInt(m.get("PageType")+"");} catch(Exception e) {};
            //10-获得过滤条件
            Map<String, Object> filter=null;
            try {
                filter=(Map)m.get("FilterData");
            } catch(Exception e) {}

            Map<String, Object> contents=contentService.getContents(catalogType, catalogId, resultType, mediaType, perSize, pageSize, page, beginCatalogId, pageType, mUdk, filter);
            if (contents!=null&&contents.size()>0) {
                map.put("ResultList", contents);
                map.put("ReturnType", "1001");
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "没有查到任何内容");
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

    @RequestMapping(value="getContentInfo.do")
    @ResponseBody
    public Map<String,Object> getContentInfo(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.2.2-content/getContentInfo");
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

            //1-得到内容类别
            String mediaType=(m.get("MediaType")==null?null:m.get("MediaType")+"");
            if (StringUtils.isNullOrEmptyOrSpace(mediaType)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获得内容类别");
                return map;
            }
            //2-得到系列内容的Id
            String contentId=(m.get("ContentId")==null?null:m.get("ContentId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(contentId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获得内容呢Id");
                return map;
            }
            //3-得到每页记录数
            int pageSize=-1;
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};
            //4-得到当前页数
            int page=1;
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};
            
            Map<String, Object> contentInfo=null;
            //检测内容是否存在redis里
            map = ContentRedisUtils.isOrNoToLocal(m, 1);
            if (map!=null) {
				int isnum = (int) map.get("IsOrNoLocal");
				Object info = map.get("Info");
				if (isnum == 0) {
					contentInfo = (Map<String, Object>) info;
				} else if (isnum == 1) {
					contentId = (String) info;
				}
			}
            map.clear();
            
            if (contentInfo==null) {
				if (mediaType.equals("SEQU")) contentInfo=contentService.getSeqMaInfo(contentId, pageSize, page, mUdk);
	            else
	            if (mediaType.equals("TTS")) {
	                ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
	                if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
	                    JedisConnectionFactory conn=(JedisConnectionFactory)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("connectionFactory");
	                    RedisOperService roService=new RedisOperService(conn);
	                    contentInfo=SearchUtils.getNewsInfo(contentId, roService);
	                }
	            } else if (mediaType.equals("AUDIO"))  contentInfo=contentService.getMaInfo(contentId, mUdk);
	            else if (mediaType.equals("RADIO"))  contentInfo=contentService.getBcInfo(contentId, mUdk);
			}
            
            
            if (contentInfo!=null&&contentInfo.size()>0) {
                map.put("ResultInfo", contentInfo);
                map.put("ReturnType", "1001");
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "没有查到任何内容");
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

    @RequestMapping(value="clickFavorite.do")
    @ResponseBody
    public Map<String,Object> clickFavorite(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.3.1-content/clickFavorite");
        alPo.setObjType("DA002");//用户喜欢对象
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
                if (mUdk!=null) {
                    Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "content/");
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

            //1-得到内容类别
            String mediaType=(m.get("MediaType")==null?null:m.get("MediaType")+"");
            if (StringUtils.isNullOrEmptyOrSpace(mediaType)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获得内容类别");
                return map;
            }
            //2-得到系列内容的Id
            String contentId=(m.get("ContentId")==null?null:m.get("ContentId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(contentId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获得内容呢Id");
                return map;
            }
            //3-得到喜欢状态：=1喜欢；=0取消喜欢
            int flag=1;
            try {flag=Integer.parseInt(m.get("Flag")+"");} catch(Exception e) {};
            map.put("Flag", flag+"");

            m.put("MUDK", mUdk);
            boolean isok = false;
            map = ContentRedisUtils.isOrNoToLocal(m, 3);
            if (map!=null) {
				int isnum = (int) map.get("IsOrNoLocal");
				Object info = map.get("Info");
				if (isnum == 0) { //未入库
					isok = false;
				} else if (isnum == 1) { //已入库
					contentId = (String) info;
					isok = true;
				} else if (isnum == 2) {
					isok = false;
				}
			}
            map.clear();
            if (isok) {
				flag=favoriteService.favorite(mediaType, contentId, flag, mUdk);
			}
            
            if (flag==1) {
                map.put("ReturnType", "1001");
                map.put("FavoriteCount", flag+"");
            } else if (flag==0) {//在喜欢时
                map.put("ReturnType", "1004");
                map.put("Message", "所指定的节目不存在");
            } else if (flag==2) {//在喜欢时
                map.put("ReturnType", "1005");
                map.put("Message", "已经喜欢了此内容");
            } else if (flag==-1) {//在取消喜欢时
                map.put("ReturnType", "1006");
                map.put("Message", "还未喜欢此内容");
            } else if (flag==-100) {//参数错误
                map.put("ReturnType", "1002");
                map.put("Message", "内容类别不能识别");
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

    @RequestMapping(value="getFavoriteList.do")
    @ResponseBody
    public Map<String,Object> getFavoriteList(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.3.2-content/getFavoriteList");
        alPo.setObjType("DA002");//用户喜欢对象
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

            //1-得到返回类型
            int resultType=3;
            try {resultType=Integer.parseInt(m.get("ResultType")+"");} catch(Exception e) {}
            //2-获得页面类型
            int pageType=1;
            try {pageType=Integer.parseInt(m.get("PageType")+"");} catch(Exception e) {};
            //3-得到内容类别
            String mediaType=(m.get("MediaType")==null?null:m.get("MediaType")+"");
            //4-得到每页记录数，默认每页10条记录
            int pageSize=10;
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};
            //5-得到当前页数
            int page=1;
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};
            //6-得到每分类条目数
            int perSize=3;
            try {perSize=Integer.parseInt(m.get("PerSize")+"");} catch(Exception e) {}
            //7-得到开始分类Id
            String beginCatalogId=(m.get("BeginCatalogId")==null?null:m.get("BeginCatalogId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(beginCatalogId)) beginCatalogId=null;


            Map<String, Object> result=favoriteService.getFavoriteList(resultType, pageType, mediaType, pageSize, page, perSize, beginCatalogId, mUdk);
            
            if (result==null||result.isEmpty()) {
                map.put("ReturnType", "1011");
                map.put("Message", "无数据");
            } else {
                map.put("ReturnType", "1001");
                map.put("ResultList", result);
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

    @RequestMapping(value="getFavoriteMTypeDistri.do")
    @ResponseBody
    public Map<String,Object> getFavoriteMTypeDistri(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.3.3-content/getFavoriteMTypeDistri");
        alPo.setObjType("DA002");//用户喜欢对象
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

            Map<String, Object> result=favoriteService.getFavoriteMTypeDistri(mUdk);

            if (result==null||result.isEmpty()) {
                map.put("ReturnType", "1011");
                map.put("Message", "无数据");
            } else {
                map.put("ReturnType", "1001");
                map.put("MediaTypeDistri", result);
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

    @RequestMapping(value="delFavorites.do")
    @ResponseBody
    public Map<String,Object> delFavorites(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.3.4-content/delFavorites");
        alPo.setObjType("DA002");//用户喜欢对象
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

            //1-得到多个要删除的喜欢内容
            String delInfos=(m.get("DelInfos")==null?null:m.get("DelInfos")+"");
            if (StringUtils.isNullOrEmptyOrSpace(delInfos)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获得需要删除的内容信息");
                return map;
            }
            List<Map<String, Object>> result=favoriteService.delFavorites(delInfos, mUdk);

            if (result==null||result.isEmpty()) {
                map.put("ReturnType", "1011");
                map.put("Message", "无数据");
            } else {
                map.put("ReturnType", "1001");
                map.put("DelFeedbackList", result);
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
    
    @RequestMapping(value="getBCProgramme.do")
    @ResponseBody
    public Map<String,Object> getBCProgramme(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.2.4-content/getBCProgramme.do");
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
            String bcId=(m.get("BcId")==null?null:m.get("BcId")+"");
            if (StringUtils.isNullOrEmptyOrSpace(bcId)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获得电台Id");
                return map;
            }
            String requestTimesstr = m.get("RequestTimes")==null?null:m.get("RequestTimes")+"";
            if (StringUtils.isNullOrEmptyOrSpace(requestTimesstr) || requestTimesstr.equals("null")) {
            	requestTimesstr = System.currentTimeMillis()+"";
			}
            List<Map<String, Object>> bcps = contentService.BcProgrammes(bcId, requestTimesstr);
            if (bcps!=null&&bcps.size()>0) {
                map.put("ResultList", bcps);
                map.put("ReturnType", "1001");
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "暂无节目单");
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
    
    @RequestMapping(value="getIsPlayingBCProgramme.do")
    @ResponseBody
    public Map<String,Object> IsPlaying(HttpServletRequest request) {
        //数据收集处理==1
        ApiLogPo alPo=ApiGatherUtils.buildApiLogDataFromRequest(request);
        alPo.setApiName("4.2.5-content/getIsPlayingBCProgramme.do");
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
            String bcIds=(m.get("BcIds")==null?null:m.get("BcIds")+"");
            if (StringUtils.isNullOrEmptyOrSpace(bcIds)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获得电台Id");
                return map;
            }
            String requestTimesstr = m.get("RequestTimes")==null?null:m.get("RequestTimes")+"";
            if (StringUtils.isNullOrEmptyOrSpace(requestTimesstr) || requestTimesstr.equals("null")) {
            	requestTimesstr = System.currentTimeMillis()+"";
			}
            long requestTime = Long.valueOf(requestTimesstr);
            Map<String, Object> bcps = contentService.getBCIsPlayingProgramme(bcIds, requestTime);
            if (bcps!=null&&bcps.size()>0) {
                map.put("ResultList", bcps);
                map.put("ReturnType", "1001");
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "暂无节目单");
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