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
import com.woting.appengine.content.service.ContentService;
import com.woting.appengine.mobile.session.model.MobileSession;

@Controller
@RequestMapping(value="/content/")
public class ContentController {
    @Resource
    private ContentService contentService;

    @RequestMapping(value="mainPage.do")
    @ResponseBody
    public Map<String,Object> mainPage(HttpServletRequest request) {
        Map<String,Object> map=new HashMap<String, Object>();
        try {
            //0-获取参数
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
            }
            if (map.get("ReturnType")!=null) return map;

            List<Map<String, Object>> ml = new ArrayList<Map<String, Object>>();
            List<Map<String, Object>> sl=null;
            Map<String, Object> bcClass=null, bcItem=null;
            bcClass=new HashMap<String, Object>();//一个分类
            bcClass.put("CatalogType", "1");
            bcClass.put("CatalogId", "001");
            bcClass.put("CatalogName", "推荐");
            bcClass.put("CatalogImg", "a.jpg");
            bcClass.put("PageSize", "3");//当前列表元素个数
            bcClass.put("AllListSize", "7");//本分类列表元素个数
            //------------------
            sl = new ArrayList<Map<String, Object>>();
            bcItem=new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京新闻广播");
            bcItem.put("ContentId", "003");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentPlay", "mms://alive.rbc.cn/fm1006");
            bcItem.put("CurrentContent", "时政要闻");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "AUDIO"); //电台
            bcItem.put("ContentName", "天下大事");
            bcItem.put("ContentId", "002");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentPlay", "mms://alive.rbc.cn/fm1073");
            bcItem.put("Actor", "李界观");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "SEQU"); //系列节目
            bcItem.put("ContentName", "观复嘟嘟");
            bcItem.put("ContentId", "001");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentPlay", "/getSeqMaInfo.do?ContentId=001");
            bcItem.put("Actor", "马未都");//当前节目
            sl.add(bcItem);
            bcClass.put("SubList", sl);
            //------------------
            ml.add(bcClass);
            bcClass=new HashMap<String, Object>();//一个分类
            bcClass.put("CatalogType", "1");
            bcClass.put("CatalogId", "002");
            bcClass.put("CatalogName", "排行");
            bcClass.put("CatalogImg", "a.jpg");
            bcClass.put("PageSize", "3");//当前列表元素个数
            bcClass.put("AllListSize", "50");//本分类列表元素个数
            //------------------
            sl = new ArrayList<Map<String, Object>>();
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京体育广播");
            bcItem.put("ContentId", "002");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentPlay", "mms://alive.rbc.cn/fm1025");
            bcItem.put("CurrentContent", "经典回顾");//当前节目
            sl.add(bcItem);
            bcItem=new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京音乐广播");
            bcItem.put("ContentId", "004");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentPlay", "mms://alive.rbc.cn/fm974");
            bcItem.put("CurrentContent", "财经报道");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京外语广播");
            bcItem.put("ContentId", "001");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentPlay", "mms://alive.rbc.cn/am774");
            bcItem.put("CurrentContent", "路况信息");//当前节目
            sl.add(bcItem);
            bcClass.put("SubList", sl);
            //------------------
            ml.add(bcClass);
            bcClass=new HashMap<String, Object>();//一个分类
            bcClass.put("CatalogType", "1");
            bcClass.put("CatalogId", "003");
            bcClass.put("CatalogName", "历史");
            bcClass.put("CatalogImg", "a.jpg");
            bcClass.put("PageSize", "3");//当前列表元素个数
            bcClass.put("AllListSize", "8");//本分类列表元素个数
            //------------------
            sl = new ArrayList<Map<String, Object>>();
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京爱家广播");
            bcItem.put("ContentId", "005");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentPlay", "mms://alive.rbc.cn/am927");
            bcItem.put("CurrentContent", "评书联播");//当前节目
            sl.add(bcItem);
            bcItem=new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京古典音乐");
            bcItem.put("ContentId", "004");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentPlay", "mms://alive.rbc.cn/cfm986");
            bcItem.put("CurrentContent", "财经报道");//当前节目
            sl.add(bcItem);
            bcItem = new HashMap<String, Object>();
            bcItem.put("MediaType", "RADIO"); //电台
            bcItem.put("ContentName", "北京通俗音乐");
            bcItem.put("ContentId", "001");
            bcItem.put("ContentImg", "images/dft_broadcast.png");
            bcItem.put("ContentPlay", "mms://alive.rbc.cn/cfm970");
            bcItem.put("CurrentContent", "路况信息");//当前节目
            sl.add(bcItem);
            bcClass.put("SubList", sl);
            //------------------
            ml.add(bcClass);
            map.put("ReturnType", "1001");
            map.put("ResultList", ml);
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
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
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
            sl = new ArrayList<Map<String, Object>>();
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
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
            }
            if (map.get("ReturnType")!=null) return map;

            //1-得到模式Id
            String catalogType=(String)m.get("CatalogType");
            if (StringUtils.isNullOrEmptyOrSpace(catalogType)) catalogType=request.getParameter("CatalogType");
            if (StringUtils.isNullOrEmptyOrSpace(catalogType)) catalogType="-1";
            //2-得到字典项Id或父栏目Id
            String catalogId=(String)m.get("CatalogId");
            if (StringUtils.isNullOrEmptyOrSpace(catalogId)) catalogId=request.getParameter("CatalogId");
            if (StringUtils.isNullOrEmptyOrSpace(catalogId)) catalogId=null;
            //3-得到返回类型
            String resultType=(String)m.get("ResultType");
            if (StringUtils.isNullOrEmptyOrSpace(resultType)) resultType=request.getParameter("ResultType");
            if (StringUtils.isNullOrEmptyOrSpace(resultType)) resultType="3";
            int _resultType=Integer.parseInt(resultType);
            //4-得到类型
            String mediaType=(String)m.get("MediaType");
            if (StringUtils.isNullOrEmptyOrSpace(mediaType)) mediaType=request.getParameter("MediaType");
            if (StringUtils.isNullOrEmptyOrSpace(mediaType)) mediaType=null;
            //5-得到每分类条目数
            String perSize=(String)m.get("PerSize");
            if (StringUtils.isNullOrEmptyOrSpace(perSize)) perSize=request.getParameter("PerSize");
            if (StringUtils.isNullOrEmptyOrSpace(perSize)) perSize="3";
            int _perSize=Integer.parseInt(perSize);
            //6-得到每页条数
            String pageSize=(String)m.get("PageSize");
            if (StringUtils.isNullOrEmptyOrSpace(pageSize)) pageSize=request.getParameter("PageSize");
            if (StringUtils.isNullOrEmptyOrSpace(pageSize)) pageSize="10";
            int _pageSize=Integer.parseInt(pageSize);
            //7-得到页数
            String page=(String)m.get("Page");
            if (StringUtils.isNullOrEmptyOrSpace(page)) page=request.getParameter("Page");
            if (StringUtils.isNullOrEmptyOrSpace(page)) page="1";
            int _page=Integer.parseInt(page);
            //8-得到开始分类Id
            String beginCatalogId=(String)m.get("BeginCatalogId");
            if (StringUtils.isNullOrEmptyOrSpace(beginCatalogId)) beginCatalogId=request.getParameter("BeginCatalogId");
            if (StringUtils.isNullOrEmptyOrSpace(beginCatalogId)) beginCatalogId=null;
            //9-获得页面类型
            String _pageType=(String)m.get("PageType");
            if (StringUtils.isNullOrEmptyOrSpace(_pageType)) _pageType=request.getParameter("PageType");
            int pageType=1;
            if (!StringUtils.isNullOrEmptyOrSpace(_pageType)) try {pageType=Integer.parseInt(_pageType);} catch(Exception e) {};

            Map<String, Object> contents=contentService.getContents(catalogType, catalogId, _resultType, mediaType, _perSize, _pageSize, _page, beginCatalogId, pageType);
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
            Map<String, Object> m=MobileUtils.getDataFromRequest(request);
            if (m==null||m.size()==0) {
                map.put("ReturnType", "0000");
                map.put("Message", "无法获取需要的参数");
            } else {
                Map<String, Object> retM = MobileUtils.dealMobileLinked(m, 0);
                MobileSession ms=(MobileSession)retM.get("MobileSession");
                map.put("SessionId", ms.getKey().getSessionId());
            }
            if (map.get("ReturnType")!=null) return map;

            //1-得到内容类别
            String mediaType=(String)m.get("MediaType");
            if (StringUtils.isNullOrEmptyOrSpace(mediaType)) {
                mediaType=request.getParameter("MediaType");
            }
            if (StringUtils.isNullOrEmptyOrSpace(mediaType)) {
                map.put("ReturnType", "1002");
                map.put("Message", "无法获得内容类别");
                return map;
            }
            //2-得到系列内容的Id
            String contentId=(String)m.get("ContentId");
            if (StringUtils.isNullOrEmptyOrSpace(contentId)) {
                contentId=request.getParameter("ContentId");
            }
            if (StringUtils.isNullOrEmptyOrSpace(contentId)) {
                map.put("ReturnType", "1003");
                map.put("Message", "无法获得内容呢Id");
                return map;
            }
            //3-得到每页记录数
            String _pageSize=(String)m.get("PageSize");
            if (StringUtils.isNullOrEmptyOrSpace(_pageSize)) {
                _pageSize=request.getParameter("PageSize");
            }
            int pageSize=-1;
            if (!StringUtils.isNullOrEmptyOrSpace(_pageSize)) {
                try {pageSize=Integer.parseInt(_pageSize);} catch(Exception e) {}
            }
            //4-得到当前页数
            String _pageNum=(String)m.get("Page");
            if (StringUtils.isNullOrEmptyOrSpace(_pageNum)) {
                _pageNum=request.getParameter("Page");
            }
            int page=-1;
            if (!StringUtils.isNullOrEmptyOrSpace(_pageNum)) {
                try {page=Integer.parseInt(_pageNum);} catch(Exception e) {}
            }

            if (mediaType.equals("SEQU")) {
                Map<String, Object> smInfo=contentService.getSeqMaInfo(contentId, pageSize, page);
                if (smInfo!=null&&smInfo.size()>0) {
                    map.put("ResultList", smInfo);
                    map.put("ReturnType", "1001");
                } else {
                    map.put("ReturnType", "1011");
                    map.put("Message", "没有查到任何内容");
                }
                
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