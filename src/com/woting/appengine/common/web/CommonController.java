package com.woting.appengine.common.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spiritdata.framework.util.SpiritRandom;
import com.spiritdata.framework.util.StringUtils;
import com.spiritdata.framework.util.TreeUtils;
import com.woting.WtAppEngineConstants;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.common.util.RequestUtils;
import com.woting.appengine.content.service.ContentService;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.session.model.MobileSession;
import com.woting.appengine.searchcrawler.service.SearchCrawlerService;
import com.woting.cm.core.channel.mem._CacheChannel;
import com.woting.cm.core.common.model.Owner;
import com.woting.cm.core.dict.mem._CacheDictionary;
import com.woting.cm.core.dict.model.DictModel;
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.woting.passport.UGA.service.UserService;
import com.woting.passport.login.service.MobileUsedService;
import com.woting.searchword.service.WordService;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.core.cache.CacheEle;

@Lazy(true)
@Controller
public class CommonController {
    @Resource
    private UserService userService;
    @Resource
    private MobileUsedService muService;
    @Resource
    private ContentService contentService;
    @Resource
    private WordService wordService;
    @Resource
    private SearchCrawlerService scs;
    
    private _CacheDictionary _cd=null;
    private _CacheChannel _cc=null;

    @PostConstruct
    public void initParam() {
        _cd=(SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)==null?null:((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)).getContent());
        _cc=(SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)==null?null:((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)).getContent());
    }

    /**
     * 进入App
     * @param request 请求对象。数据包含在Data流中，以json格式存储，其中必须包括手机串号。如：{"imei":"123456789023456789"}
     * @return 分为如下情况<br/>
     *   若有异常：{ReturnType:T, TClass:exception.class, Message: e.getMessage()}
     *   已经登录：{ReturnType:1001, sessionId:sid, userInfo:{userName:un, mphone:138XXXX2345, email:a@b.c, realName:实名, headImg:hiUrl}}
     *     其中用户信息若没有相关内容，则相关的key:value对就不存在
     *   还未登录：{ReturnType:1002, sessionId:sid}
     */
    @RequestMapping(value="/common/entryApp.do")
    @ResponseBody
    public Map<String,Object> entryApp(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
                return map;
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 1);
                if ((retM.get("ReturnType")+"").equals("2001")) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    MobileSession ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                    if ((retM.get("ReturnType")+"").equals("1002")) {
                        map.put("ReturnType", "1002");
                    } if ((retM.get("ReturnType")+"").equals("2002")) {
                        map.put("ReturnType", "2002");
                        map.put("Message", "无法找到相应的用户");
                    }else {
                        map.put("ReturnType", "1001");
                        if ((UserPo)ms.getAttribute("user")!=null) map.put("UserInfo", ((UserPo)ms.getAttribute("user")).toHashMap4Mobile());
                    }
                }
                map.put("ServerStatus", "1"); //服务器状态
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

    @RequestMapping(value="/mainPage.do")
    @ResponseBody
    public Map<String,Object> mainPage(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            MobileSession ms=null;
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 0);
                if ((retM.get("ReturnType")+"").equals("2001")) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //获得页面类型
            int pageType=1;
            try {pageType=Integer.parseInt(m.get("PageType")+"");} catch(Exception e) {};
            //得到每页条数
            int pageSize=10;
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};
            //得到页数
            int page=1;
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};

            MobileKey mk=MobileUtils.getMobileKey(m);
            Map<String, Object> cl=contentService.getMainPage(ms.getKey().getUserId(), pageType, pageSize, page, mk);

            if (cl!=null&&cl.size()>0) {
                map.put("ResultList", cl);
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
            map.put("Message", e.getMessage());
            return map;
        }
    }

    /**
     * 得到当前的活跃的热词
     * @param request
     * @return
     */
    @RequestMapping(value="/getHotKeys.do")
    @ResponseBody
    public Map<String,Object> getHotKeys(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 0);
                if ((retM.get("ReturnType")+"").equals("2001")) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    MobileSession ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //1-获取功能类型，目前只有1内容搜索
            int funType=1;
            try {funType=Integer.parseInt(m.get("FunType")+"");} catch(Exception e) {}
            //2-检索词数量
            int wordSize=10;
            try {wordSize=Integer.parseInt(m.get("WordSize")+"");} catch(Exception e) {}
            //3-返回类型
            int returnType=1;
            try {returnType=Integer.parseInt(m.get("ReturnType")+"");} catch(Exception e) {}

            Owner o=new Owner(201, map.get("SessionId")+"");
            List<String>[] retls=wordService.getHotWords(o, returnType, wordSize);
            if (retls==null||retls.length==0) map.put("ReturnType", "1011");
            else {
                if (retls.length==1&&(retls[0]==null||retls[0].size()==0)) map.put("ReturnType", "1011");
                else 
                if (retls.length==2&&(retls[0]==null||retls[0].size()==0)&&(retls[1]==null||retls[1].size()==0)) map.put("ReturnType", "1011");
                else 
                if (retls.length>2)  map.put("ReturnType", "1011");
            }
            if (map.get("ReturnType")!=null) return map;

            String tempStr="";
            List<String> tempWords=null;
            if (retls.length==1) {
                tempWords=retls[0];
                for (String word: tempWords) tempStr+=","+word;
                map.put("KeyList", tempStr.substring(1));
            } else if (retls.length==2) {
                tempStr="";
                if ((retls[0]!=null&&retls[0].size()>0)) {
                    tempWords=retls[0];
                    for (String word: tempWords) tempStr+=","+word;
                    map.put("SysKeyList", tempStr.substring(1));
                }
                tempStr="";
                if ((retls[1]!=null&&retls[1].size()>0)) {
                    tempWords=retls[1];
                    for (String word: tempWords) tempStr+=","+word;
                    map.put("MyKeyList", tempStr.substring(1));
                }
            }
            map.put("ReturnType", "1001");
//            map.put("KeyList", "逻辑思维,郭德纲,芈月传奇,数学,恐怖主义,鬼吹灯,盗墓笔记,老梁说事");
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
     * 目前不处理群组查找热词
     * 查找当前的活跃热词
     */
    @RequestMapping(value="/searchHotKeys.do")
    @ResponseBody
    public Map<String,Object> searchHotKeys(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 0);
                if ((retM.get("ReturnType")+"").equals("2001")) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    MobileSession ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //获得查找词
            String searchStr=(m.get("KeyWord")==null?null:m.get("KeyWord")+"");
            if (StringUtils.isNullOrEmptyOrSpace(searchStr)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法得到查询串");
                return map;
            }
            //1-获取功能类型，目前只有1内容搜索
            int funType=1;
            try {funType=Integer.parseInt(m.get("FunType")+"");} catch(Exception e) {}
            //2-检索词数量
            int wordSize=10;
            try {wordSize=Integer.parseInt(m.get("WordSize")+"");} catch(Exception e) {}
            //3-返回类型
            int returnType=1;
            try {returnType=Integer.parseInt(m.get("ReturnType")+"");} catch(Exception e) {}

            Owner o=new Owner(201, map.get("SessionId")+"");
            List<String>[] retls=wordService.searchHotWords(searchStr, o, returnType, wordSize);
            if (retls==null||retls.length==0) map.put("ReturnType", "1011");
            else {
                if (retls.length==1&&(retls[0]==null||retls[0].size()==0)) map.put("ReturnType", "1011");
                else 
                if (retls.length==2&&(retls[0]==null||retls[0].size()==0)&&(retls[1]==null||retls[1].size()==0)) map.put("ReturnType", "1011");
                else 
                if (retls.length>2)  map.put("ReturnType", "1011");
            }
            if (map.get("ReturnType")!=null) return map;

            String tempStr="";
            List<String> tempWords=null;
            if (retls.length==1) {
                tempWords=retls[0];
                for (String word: tempWords) tempStr+=","+word;
                map.put("KeyList", tempStr.substring(1));
            } else if (retls.length==2) {
                tempStr="";
                if ((retls[0]!=null&&retls[0].size()>0)) {
                    tempWords=retls[0];
                    for (String word: tempWords) tempStr+=","+word;
                    map.put("SysKeyList", tempStr.substring(1));
                }
                tempStr="";
                if ((retls[1]!=null&&retls[1].size()>0)) {
                    tempWords=retls[1];
                    for (String word: tempWords) tempStr+=","+word;
                    map.put("MyKeyList", tempStr.substring(1));
                }
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

    private void convert2Data(TreeNode<? extends TreeNodeBean> t, Map<String, Object> retData, String catalogType) {
        if (retData!=null&&t!=null) {
            retData.put("CatalogType", catalogType);
            retData.put("CatalogId", t.getId());
            retData.put("CatalogName", t.getNodeName());
            if (!t.isLeaf()) {
                List<Map<String, Object>> subCata=new ArrayList<Map<String, Object>>();
                for (TreeNode<? extends TreeNodeBean> _t: t.getChildren()) {
                    Map<String, Object> m=new HashMap<String, Object>();
                    convert2Data(_t, m, catalogType);
                    subCata.add(m);
                }
                retData.put("SubCata", subCata);
            }
        }
    }
    private List<Map<String, Object>> getDeepList(TreeNode<? extends TreeNodeBean> t, String catalogType) {
        if (t==null) return null;
        List<Map<String, Object>> ret=new ArrayList<Map<String, Object>>();
        if (!t.isLeaf()) {
            for (TreeNode<? extends TreeNodeBean> _t: t.getChildren()) {
                Map<String, Object> m=new HashMap<String, Object>();
                m.put("CatalogType", catalogType);
                m.put("CatalogId", _t.getId());
                m.put("CatalogName", _t.getNodeName());
                ret.add(m);
                List<Map<String, Object>> _r=getDeepList(_t, catalogType);
                if (_r!=null) ret.addAll(_r);
            }
            return ret;
        } else return null;
    }
    private List<Map<String, Object>> getLevelNodeList(TreeNode<? extends TreeNodeBean> t, int level, String catalogType) {
        if (t==null) return null;
        List<Map<String, Object>> ret=new ArrayList<Map<String, Object>>();
        if (!t.isLeaf()) {
            for (TreeNode<? extends TreeNodeBean> _t: t.getChildren()) {
                if (level==1) {
                    Map<String, Object> m=new HashMap<String, Object>();
                    m.put("CatalogType", catalogType);
                    m.put("CatalogId", _t.getId());
                    m.put("CatalogName", _t.getNodeName());
                    ret.add(m);
                } else {
                    List<Map<String, Object>> _r=getLevelNodeList(_t, level-1, catalogType);
                    if (_r!=null) ret.addAll(_r);
                }
            }
            return ret;
        } else return null;
    }

    @RequestMapping(value="/getCatalogInfo.do")
    @ResponseBody
    public Map<String,Object> getCatalogInfo(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 0);
                if ((retM.get("ReturnType")+"").equals("2001")) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    MobileSession ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
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
            int resultType=2;
            try {resultType=Integer.parseInt(m.get("ResultType")+"");} catch(Exception e) {}
            //4-得到相对层次
            int relLevel=1;
            try {relLevel=Integer.parseInt(m.get("RelLevel")+"");} catch(Exception e) {}

            //根据分类获得根
            TreeNode<? extends TreeNodeBean> root=null;
            if (catalogType.equals("-1")) {
                root=_cc.channelTree;
            } else {
                DictModel dm=_cd.getDictModelById(catalogType);
                if (dm!=null&&dm.dictTree!=null) root=dm.dictTree;
            }
            //获得相应的结点，通过查找
            if (root!=null) {
                if (catalogId!=null) root=root.findNode(catalogId);
            }
            //根据层级参数，对树进行截取
            if (root!=null&&relLevel>0) root=TreeUtils.cutLevelClone(root, relLevel);

            if (root!=null) {
                Map<String, Object> CatalogData=new HashMap<String, Object>();
                //返回类型
                if (resultType==1) {//树结构
                    convert2Data(root, CatalogData, catalogType);
                    map.put("CatalogData", CatalogData);
                } else {//列表结构
                    if (relLevel<=0) {//所有结点列表
                        map.put("CatalogData", getDeepList(root, catalogType));
                    } else { //某层级节点
                        map.put("CatalogData", getLevelNodeList(root, relLevel, catalogType));
                    }
                }
                map.put("ReturnType", "1001");
            } else {
                map.put("Message", "无符合条件的"+(catalogType.equals("-1")?"栏目":"分类")+"信息");
                map.put("ReturnType", "1011");
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

    @RequestMapping(value="/searchByText.do")
    @ResponseBody
    public Map<String,Object> searchByText(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 0);
                if ((retM.get("ReturnType")+"").equals("2001")) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    MobileSession ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //获得查询串
            String searchStr=(m.get("SearchStr")==null?null:m.get("SearchStr")+"");
            if (StringUtils.isNullOrEmptyOrSpace(searchStr)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法得到查询串");
                return map;
            }

            //敏感词处理
            Owner o=new Owner(201, map.get("SessionId")+"");
            String _s[]=searchStr.split(",");
            for (int i=0; i<_s.length; i++) wordService.addWord2Online(_s[i].trim(), o);

            //获得结果类型，0获得一个列表，1获得分类列表，这个列表根据content字段处理，这个字段目前没有用到
            int resultType=0;
            try {resultType=Integer.parseInt(m.get("ResultType")+"");} catch(Exception e) {}
            //获得页面类型
            int pageType=1;
            try {pageType=Integer.parseInt(m.get("PageType")+"");} catch(Exception e) {};
            //得到每页条数
            int pageSize=10;
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};
            //得到页数
            int page=1;
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};

            MobileKey mk=MobileUtils.getMobileKey(m);
            Map<String, Object> cl = new HashMap<String,Object>();
            long a=System.currentTimeMillis();
            if(page>0 && pageSize>0 && resultType==0 && pageType==0)cl=scs.searchCrawler(searchStr, resultType, pageType, page, pageSize, mk);
            else cl=contentService.searchAll(searchStr, resultType, pageType, mk);
            a=System.currentTimeMillis()-a;

            if (cl!=null&&cl.size()>0) {
                map.put("ResultType", cl.get("ResultType"));
                cl.remove("ResultType");
                map.put("ResultList", cl);
                map.put("ReturnType", "1001");
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "没有查到任何内容");
            }
            map.put("TestDuration", a);
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="/searchByVoice.do")
    @ResponseBody
    public Map<String,Object> searchByVoice(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 0);
                if ((retM.get("ReturnType")+"").equals("2001")) {
                    map.put("ReturnType", "0000");
                    map.put("Message", "无法获取设备Id(IMEI)");
                } else {
                    MobileSession ms=(MobileSession)retM.get("MobileSession");
                    map.put("SessionId", ms.getKey().getSessionId());
                }
            }
            if (map.get("ReturnType")!=null) return map;

            //获得查询串
            String searchStr=(m.get("SearchStr")==null?null:m.get("SearchStr")+"");
            if (StringUtils.isNullOrEmptyOrSpace(searchStr)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法得到查询串");
                return map;
            }
            //敏感词处理
            Owner o=new Owner(201, map.get("SessionId")+"");
            String _s[]=searchStr.split(",");
            for (int i=0; i<_s.length; i++) wordService.addWord2Online(_s[i].trim(), o);

            //获得结果类型，0获得一个列表，1获得分类列表，这个列表根据content字段处理，这个字段目前没有用到
            int resultType=0;
            try {resultType=Integer.parseInt(m.get("ResultType")+"");} catch(Exception e) {}
            //获得页面类型
            int pageType=1;
            try {pageType=Integer.parseInt(m.get("PageType")+"");} catch(Exception e) {};
            //得到每页条数
            int pageSize=10;
            try {pageSize=Integer.parseInt(m.get("PageSize")+"");} catch(Exception e) {};
            //得到页数
            int page=1;
            try {page=Integer.parseInt(m.get("Page")+"");} catch(Exception e) {};

            MobileKey mk=MobileUtils.getMobileKey(m);
            Map<String, Object> cl = new HashMap<String,Object>();
            long a=System.currentTimeMillis();
            if(resultType==0 && pageType==0) cl = scs.searchCrawler(searchStr, resultType, pageType, page, pageSize, mk);
            else cl=contentService.searchAll(searchStr, resultType, pageType, mk);
            a=System.currentTimeMillis()-a;

            if (cl!=null&&cl.size()>0) {
                map.put("ResultType", cl.get("ResultType"));
                cl.remove("ResultType");
                map.put("ResultList", cl);
                map.put("ReturnType", "1001");
            } else {
                map.put("ReturnType", "1011");
                map.put("Message", "没有查到任何内容");
            }
            map.put("TestDuration", a);
            return map;
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
    }
    
    @RequestMapping(value="/lkTTS.do")
    @ResponseBody
    public Map<String,Object> getlkTTSInfo(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            String[] str = {
            		"目前从四环内环上万泉寺路暂无匝道，如果开车从四环奔颐和园、圆明园等景区，需要先从万泉河出口出来，走一段四环辅路，经过3个红绿灯才能拐到万泉河路上。“我们计划修一条西向北的高架匝道，方便驾车直接从四环进入万泉河路。”",
            		"晚高峰期间，北京国贸桥附近路段车行缓慢",
            		"京哈高速京秦段北京、沈阳方向，已于2016年06月17日15时12分恢复正常通行。该路段曾于2016年06月17日12时56分因高速交警管制，榛子镇站双向禁止货车上道",
            		"G25长深高速沂水收费站-沂水北收费站K1510+700长春方向发生一货车侧翻事故，占用应急车道，请过往车辆减速慢行",
            		"S12滨德高速阳信收费站-庆云收费K28+000-K38+000滨州方向养护施工，占用应急车道，超车道和行车道可正常通行，请过往车辆谨慎慢行",
            		"S7602青岛前港湾区疏港高速K24+400-K25+900双向全封闭施工，工期约8个月，途经车辆可由辅路通行，并按照施工现场设置的引导标志指示减速慢行，注意安全",
            		"G22青兰高速K221+597-K222+297青岛方向因隧道养护施工，占用两条行车道，超车道可正常通行，请减速慢行",
            		"G20青银高速禹城南站因省道S101地方道路施工，施工日期至2017年4月17日，出入口无法通行",
            		"京哈高速京秦段北京、沈阳方向，已于2016年06月17日14时44分恢复正常通行。该路段曾于2016年06月17日14时10分因高速交警管制，玉田站双向禁止五轴（含）以上货车及危险品车辆上道",
            		"唐津高速唐山、天津方向，已于2016年06月17日14时45分恢复正常通行。该路段曾于2016年06月17日12时40分因高速交警管制，唐山东站双向禁止货车上道，唐港站双向关闭",
            		"长深高速承唐唐山段承德方向，已于2016年06月17日14时43分恢复正常通行。该路段曾于2016年06月17日14时08分因高速交警管制，南湖站附近K973处实行交通管制，禁止车辆通行",
            		"S1济聊高速茌平收费站-齐河西收费站K23+600济南方向养护施工，占用应急车道和行车道，超车道可正常通行",
            		"S12滨德高速阳信收费站-庆云收费K28+000-K38+000滨州方向养护施工，占用应急车道，超车道和行车道可正常通行，请过往车辆谨慎慢行",
            		"S29滨莱高速高青北收费站-和庄收费站K14+700-K108+579滨州方向因路面养护施工，四分之一幅封闭施工，四分之三幅可通行，请减速慢行或择路绕行",
            		"G2501南京绕城高速东北段由南京四桥往六合方向17K过新篁收费站1公里附近发生1起事故，现场占用第三、应急车道，暂不影响通行，事故正在处理中",
            		"张涿高速保定段涿州方向，已于2016年06月17日14时31分恢复正常通行。该路段曾于2016年06月17日10时00分北龙门隧道K82+714至K87+383处养护施工，占用行车道和应急车道",
            		"保沧高速沧州方向，该路段于2016年06月17日14时22分因与京港澳高速京石段互通附近K19至K17处养护施工，占用超车道和应急车道",
            		"青银高速青岛、银川方向，已于2016年06月17日13时52分恢复正常通行。该路段曾于2016年06月17日12时09分因车流量大，窦妪站下道口K614+932处车辆缓慢通行",
            		"宜昌长江公路大桥G50：2016年03月18日至2016年06月30日,G50沪渝高速（宜昌长江公路大桥）往宜昌方向虎牙互通匝道封闭施工，往伍家岗、宜昌、三峡大坝方向的车辆，请转G50汉宜高速武汉方向行驶3公里至猇亭收费站绕行。预计工期105天",
            		"荆宜高速公路G42：荆宜向虎牙互通匝道施工封闭，转G50汉宜高速往武汉方向车辆，请直行3.5公里绕至宜昌大桥桥北收费站"};
            int random=SpiritRandom.getRandom(new Random(), 0, 19);
            String uri = str[random];
            map.put("ContentURI", uri);
        } catch(Exception e) {
            e.printStackTrace();
            map.put("ReturnType", "T");
            map.put("TClass", e.getClass().getName());
            map.put("Message", e.getMessage());
            return map;
        }
        return map;
    }
}