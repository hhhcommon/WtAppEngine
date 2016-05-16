package com.woting.appengine.common.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spiritdata.framework.util.StringUtils;
import com.spiritdata.framework.util.TreeUtils;
import com.woting.WtAppEngineConstants;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.content.service.ContentService;
import com.woting.appengine.mobile.session.model.MobileSession;
import com.woting.appengine.searchcrawler.service.BaiDuNewsService;
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
    @Resource
    private BaiDuNewsService baiduNewsService;
    
    private _CacheDictionary _cd=null;
    private _CacheChannel _cc=null;

    @PostConstruct
    public void initParam() {
        _cd=((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)).getContent();
        _cc=((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)).getContent();
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
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
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
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
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
            String _pageType=(String)m.get("PageType");
            if (StringUtils.isNullOrEmptyOrSpace(_pageType)) _pageType=request.getParameter("PageType");
            int pageType=1;
            if (!StringUtils.isNullOrEmptyOrSpace(_pageType)) try {pageType=Integer.parseInt(_pageType);} catch(Exception e) {};
            //得到每页条数
            String pageSize=(String)m.get("PageSize");
            if (StringUtils.isNullOrEmptyOrSpace(pageSize)) pageSize=request.getParameter("PageSize");
            if (StringUtils.isNullOrEmptyOrSpace(pageSize)) pageSize="10";
            int _pageSize=Integer.parseInt(pageSize);
            //得到页数
            String page=(String)m.get("Page");
            if (StringUtils.isNullOrEmptyOrSpace(page)) page=request.getParameter("Page");
            if (StringUtils.isNullOrEmptyOrSpace(page)) page="1";
            int _page=Integer.parseInt(page);

            Map<String, Object> cl=contentService.getMainPage(ms.getKey().getUserId(), pageType, _pageSize, _page);

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
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
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
            String funType=(String)m.get("FunType");
            if (StringUtils.isNullOrEmptyOrSpace(funType)) {
                funType=request.getParameter("FunType");
            }
            int _funType=1;
            if (!StringUtils.isNullOrEmptyOrSpace(funType)) {
                try {
                    _funType=Integer.parseInt(funType);
                } catch(Exception e) {}
            }
            //2-检索词数量
            String wordSize=(String)m.get("WordSize");
            if (StringUtils.isNullOrEmptyOrSpace(wordSize)) {
                wordSize=request.getParameter("WordSize");
            }
            int _wordSize=10;
            if (!StringUtils.isNullOrEmptyOrSpace(wordSize)) {
                try {
                    _wordSize=Integer.parseInt(wordSize);
                } catch(Exception e) {}
            }
            //3-返回类型
            String returnType=(String)m.get("ReturnType");
            if (StringUtils.isNullOrEmptyOrSpace(funType)) {
                returnType=request.getParameter("ReturnType");
            }
            int _returnType=1;
            if (!StringUtils.isNullOrEmptyOrSpace(returnType)) {
                try {
                    _returnType=Integer.parseInt(returnType);
                } catch(Exception e) {}
            }

            Owner o=new Owner(201, map.get("SessionId")+"");
            List<String>[] retls=wordService.getHotWords(o, _returnType, _wordSize);
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
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
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
            String searchStr=(String)m.get("KeyWord");
            if (StringUtils.isNullOrEmptyOrSpace(searchStr)) searchStr=request.getParameter("KeyWord");
            if (StringUtils.isNullOrEmptyOrSpace(searchStr)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法得到查询串");
                return map;
            }
            //1-获取功能类型，目前只有1内容搜索
            String funType=(String)m.get("FunType");
            if (StringUtils.isNullOrEmptyOrSpace(funType)) {
                funType=request.getParameter("FunType");
            }
            int _funType=1;
            if (!StringUtils.isNullOrEmptyOrSpace(funType)) {
                try {
                    _funType=Integer.parseInt(funType);
                } catch(Exception e) {}
            }
            //2-检索词数量
            String wordSize=(String)m.get("WordSize");
            if (StringUtils.isNullOrEmptyOrSpace(funType)) {
                wordSize=request.getParameter("WordSize");
            }
            int _wordSize=10;
            if (!StringUtils.isNullOrEmptyOrSpace(wordSize)) {
                try {
                    _wordSize=Integer.parseInt(wordSize);
                } catch(Exception e) {}
            }
            //3-返回类型
            String returnType=(String)m.get("ReturnType");
            if (StringUtils.isNullOrEmptyOrSpace(funType)) {
                returnType=request.getParameter("ReturnType");
            }
            int _returnType=1;
            if (!StringUtils.isNullOrEmptyOrSpace(returnType)) {
                try {
                    _returnType=Integer.parseInt(returnType);
                } catch(Exception e) {}
            }

            Owner o=new Owner(201, map.get("SessionId")+"");
            List<String>[] retls=wordService.searchHotWords(searchStr, o, _returnType, _wordSize);
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
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
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
            String catalogType=(String)m.get("CatalogType");
            if (StringUtils.isNullOrEmptyOrSpace(catalogType)) {
                catalogType=request.getParameter("CatalogType");
            }
            if (StringUtils.isNullOrEmptyOrSpace(catalogType)) {
                catalogType="-1";
            }
            //2-得到字典项Id或父栏目Id
            String catalogId=(String)m.get("CatalogId");
            if (StringUtils.isNullOrEmptyOrSpace(catalogId)) {
                catalogId=request.getParameter("CatalogId");
            }
            if (StringUtils.isNullOrEmptyOrSpace(catalogId)) {
                catalogId=null;
            }
            //3-得到返回类型
            String resultType=(String)m.get("ResultType");
            if (StringUtils.isNullOrEmptyOrSpace(resultType)) {
                resultType=request.getParameter("ResultType");
            }
            if (StringUtils.isNullOrEmptyOrSpace(resultType)) {
                resultType="2";
            }
            //4-得到相对层次
            String relLevel=(String)m.get("RelLevel");
            if (StringUtils.isNullOrEmptyOrSpace(relLevel)) {
                relLevel=request.getParameter("RelLevel");
            }
            if (StringUtils.isNullOrEmptyOrSpace(relLevel)) {
                relLevel="1";
            }

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
            int _relLevel=Integer.parseInt(relLevel);
            if (root!=null&&_relLevel>0) root=TreeUtils.cutLevelClone(root, _relLevel);

            if (root!=null) {
                Map<String, Object> CatalogData=new HashMap<String, Object>();
                //返回类型
                int _resultType=Integer.parseInt(resultType);
                if (_resultType==1) {//树结构
                    convert2Data(root, CatalogData, catalogType);
                    map.put("CatalogData", CatalogData);
                } else {//列表结构
                    if (_relLevel<=0) {//所有结点列表
                        map.put("CatalogData", getDeepList(root, catalogType));
                    } else { //某层级节点
                        map.put("CatalogData", getLevelNodeList(root, _relLevel, catalogType));
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
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
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
            String searchStr=(String)m.get("SearchStr");
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
            try {
                resultType=Integer.parseInt(m.get("ResultType")+"");
            } catch(Exception e) {}
            //获得页面类型
            String _pageType=(String)m.get("PageType");
            if (StringUtils.isNullOrEmptyOrSpace(_pageType)) _pageType=request.getParameter("PageType");
            int pageType=1;
            if (!StringUtils.isNullOrEmptyOrSpace(_pageType)) try {pageType=Integer.parseInt(_pageType);} catch(Exception e) {};

            String page = (String) m.get("Page");
            String pagesize = (String) m.get("PageSize");
            Map<String, Object> cl = new HashMap<String,Object>();
            long a=System.currentTimeMillis();
            if(!StringUtils.isNullOrEmptyOrSpace(page) && !StringUtils.isNullOrEmptyOrSpace(pagesize)){
                if(resultType==0 && pageType==0) cl=scs.searchCrawler(searchStr, resultType, pageType, Integer.valueOf(page), Integer.valueOf(pagesize));
                else cl=contentService.searchAll(searchStr, resultType, pageType);
                a=System.currentTimeMillis()-a;
            }
            
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
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
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
            String searchStr=(String)m.get("SearchStr");
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
            try {
                resultType=Integer.parseInt(m.get("ResultType")+"");
            } catch(Exception e) {}
            //获得页面类型
            String _pageType=(String)m.get("PageType");
            if (StringUtils.isNullOrEmptyOrSpace(_pageType)) _pageType=request.getParameter("PageType");
            int pageType=1;
            if (!StringUtils.isNullOrEmptyOrSpace(_pageType)) try {pageType=Integer.parseInt(_pageType);} catch(Exception e) {};
            
            int page = (int) m.get("Page");
            int pageSize = (int) m.get("PageSize");
            Map<String, Object> cl = new HashMap<String,Object>();
            long a=System.currentTimeMillis();
            if(resultType==0 && pageType==0){
            	cl = scs.searchCrawler(searchStr, resultType, pageType, page, pageSize);
            //	 cl=threadService.searchWebAndLocal(searchStr, resultType, pageType);
            }else{
            	cl=contentService.searchAll(searchStr, resultType, pageType);
            }
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
}