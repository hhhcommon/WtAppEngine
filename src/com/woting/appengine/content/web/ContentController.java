package com.woting.appengine.content.web;

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
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.common.util.RequestUtils;
import com.woting.appengine.content.service.ContentService;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.session.model.MobileSession;
import com.woting.appengine.searchcrawler.utils.SearchUtils;
import com.woting.favorite.service.FavoriteService;

@Controller
@RequestMapping(value="/content/")
public class ContentController {
    @Resource
    private ContentService contentService;
    @Resource
    private FavoriteService favoriteService;

    /**
     * 获得标题图
     * @param request
     * @return 标题图格式
     */
    @RequestMapping(value="getLoopImgs.do")
    @ResponseBody
    public Map<String,Object> getLoopImgs(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
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
            bcItem.put("ImgDesc", "北京怀旧金曲");
            bcItem.put("ImgContentUrl", "abc/content.do?contentId=1");
            sl.add(bcItem);
            bcItem.put("ImgUrl", "asset/contents/imgs/1303967876491.jpg");
            bcItem.put("ImgIdx", "1");
            bcItem.put("ImgDesc", "故事新编");
            bcItem.put("ImgContentUrl", "abc/content.do?contentId=2");
            sl.add(bcItem);
            bcItem.put("ImgUrl", "asset/contents/imgs/m_1303967844788.jpg");
            bcItem.put("ImgIdx", "2");
            bcItem.put("ImgDesc", "新闻纵览");
            bcItem.put("ImgContentUrl", "abc/content.do?contentId=3");
            sl.add(bcItem);
            bcItem.put("ImgUrl", "asset/contents/imgs/m_1303967870670.jpg");
            bcItem.put("ImgIdx", "3");
            bcItem.put("ImgDesc", "体坛风云");
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
            map.put("Message", e.getMessage());
            return map;
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
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
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

            MobileKey mk=MobileUtils.getMobileKey(m);
            Map<String, Object> contents=contentService.getContents(catalogType, catalogId, resultType, mediaType, perSize, pageSize, page, beginCatalogId, pageType, mk, filter);
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
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="getContentInfo.do")
    @ResponseBody
    public Map<String,Object> getContentInfo(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
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
            MobileKey mk=MobileUtils.getMobileKey(m);
            if (mediaType.equals("SEQU")) contentInfo=contentService.getSeqMaInfo(contentId, pageSize, page, mk);
            else
            if (mediaType.equals("TTS"))  contentInfo=SearchUtils.getNewsInfo(contentId);

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
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="clickFavorite.do")
    @ResponseBody
    public Map<String,Object> clickFavorite(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
            }
            if (map.get("ReturnType")!=null) return map;

            MobileKey mk=MobileUtils.getMobileKey(m);
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

            flag=favoriteService.favorite(mediaType, contentId, flag, mk);
            
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
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="getFavoriteList.do")
    @ResponseBody
    public Map<String,Object> getFavoriteList(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
            }
            if (map.get("ReturnType")!=null) return map;

            MobileKey mk=MobileUtils.getMobileKey(m);
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


            Map<String, Object> result=favoriteService.getFavoriteList(resultType, pageType, mediaType, pageSize, page, perSize, beginCatalogId, mk);
            
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
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="getFavoriteMTypeDistri.do")
    @ResponseBody
    public Map<String,Object> getFavoriteMTypeDistri(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
            }
            if (map.get("ReturnType")!=null) return map;

            MobileKey mk=MobileUtils.getMobileKey(m);
            Map<String, Object> result=favoriteService.getFavoriteMTypeDistri(mk);

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
            map.put("Message", e.getMessage());
            return map;
        }
    }

    @RequestMapping(value="delFavorites.do")
    @ResponseBody
    public Map<String,Object> delFavorites(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=RequestUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM=MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
            }
            if (map.get("ReturnType")!=null) return map;

            MobileKey mk=MobileUtils.getMobileKey(m);
            //1-得到多个要删除的喜欢内容
            String delInfos=(m.get("DelInfos")==null?null:m.get("DelInfos")+"");
            if (StringUtils.isNullOrEmptyOrSpace(delInfos)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获得需要删除的内容信息");
                return map;
            }
            List<Map<String, Object>> result=favoriteService.delFavorites(delInfos, mk);

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
            map.put("Message", e.getMessage());
            return map;
        }
    }
}