package com.woting.appengine.searchcrawler.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.ext.spring.redis.RedisOperService;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class BaiDuNewsSearch extends Thread {

	private static int searchnum = 20;
	private String constr;
	
	public BaiDuNewsSearch(String constr) {
		this.constr = constr;
	}

	private List<Map<String, Object>> baiduNewsSearch(String content) {
		content = SearchUtils.utf8TOurl(content);
		String url = "http://news.baidu.com/ns?word=" + content + "&tn=news&from=news&cl=2&rn=20&ct=1";
		Document doc = null;
		String contentname = "";
		String contenturl = "";
		String contentid = "";
		String contentimg = "";
		String contentdesc = "";
		String contentpubstr = "";
		String contentpub = "";
		String contentpubtime = "";
		List<Map<String, Object>> newlist = new ArrayList<Map<String, Object>>();
		try {
			doc = Jsoup.connect(url).ignoreContentType(true).timeout(5000).get();
			Elements elements = doc.select("div[class=result]");
			int num = (elements.size() > searchnum ? searchnum : elements.size());
			for (int i = 0; i < num; i++) {
				Map<String, Object> newmap = new HashMap<String, Object>();
				Element element = elements.get(i);
				if (element.getElementsByTag("h3") != null) {
					Element ele = element.getElementsByTag("h3").get(0);
					contenturl = ele.getElementsByTag("a").get(0).attr("href");
					contentname = ele.getElementsByTag("a").get(0).html();
					contentid = SequenceUUID.getPureUUID();
				}
				
				if (element.select("p[class=c-author]").size() > 0) 
					contentpubstr = element.getElementsByTag("p").get(0).html();
				if (element.select("img[class=c-img c-img6]").size() > 0) 
					contentimg = element.select("img[class=c-img c-img6]").get(0).attr("src");
				if (element.select("div[class=c-summary c-row ]").size() > 0) 
					contentdesc = element.select("div[class=c-summary c-row ]").get(0).html();
				if (element.select("div[class=c-span18 c-span-last]").size() > 0) 
					contentdesc = element.select("div[class=c-span18 c-span-last]").get(0).html();
				
				contentname = SearchUtils.cleanTag(contentname);
				contentpubstr = SearchUtils.cleanTag(contentpubstr);
				contentdesc = SearchUtils.cleanTag(contentdesc);
				String[] pubinfo = SearchUtils.getPubInfo(contentpubstr);
				contentpub = pubinfo[0];
				contentpubtime = pubinfo[1];
				
				newmap.put("ContentId", contentid);
				newmap.put("ContentName", contentname);
				newmap.put("ContentURL", contenturl);
				newmap.put("ContentImg", contentimg);
				newmap.put("ContentDesc", contentdesc);
				newmap.put("ContentPub", contentpub);
				newmap.put("ContentPubTime", contentpubtime);
				newmap.put("MediaType", "TTS");
				newlist.add(newmap); 
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return newlist;
	}
	
	@Override
	public void run() {
		try {
			System.out.println("百度搜索开始");
		    List<Map<String, Object>> listnews =  baiduNewsSearch(constr);
		    for (Map<String, Object> newmap : listnews) {
			    new NewsSearch(constr, newmap).start();
		    }
		} catch (Exception e) {
			System.out.println("百度搜索异常");
		}finally {
	        ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
	        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
	            JedisConnectionFactory conn=(JedisConnectionFactory)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("connectionFactory");
	            RedisOperService roService=new RedisOperService(conn);
	            SearchUtils.updateSearchFinish(constr, roService); // 暂定开启新闻搜索所有线程后，新闻搜索完成
	        }
		    System.out.println("百度搜索完成");
		}
	}
}
