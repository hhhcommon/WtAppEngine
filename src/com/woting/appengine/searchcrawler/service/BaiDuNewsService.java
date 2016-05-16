package com.woting.appengine.searchcrawler.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class BaiDuNewsService extends Thread {

	private static int searchnum = 20;
	private static String constr;
	
	public static void begin(String constr){
		BaiDuNewsService.constr = constr;
		BaiDuNewsService bd = new BaiDuNewsService();
		bd.run();
	}

	private static List<Map<String, Object>> baiduNewsService(String content) {
		content = SearchUtils.utf8TOurl(content);
		String url = "http://news.baidu.com/ns?word=" + constr + "&tn=news&from=news&cl=2&rn=20&ct=1";
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
					contentid = UUID.randomUUID().toString().replace("-", "");
				}
				if (element.select("p[class=c-author]").size() > 0) {
					contentpubstr = element.getElementsByTag("p").get(0).html();
				}
				if (element.select("img[class=c-img c-img6]").size() > 0) {
					contentimg = element.select("img[class=c-img c-img6]").get(0).attr("src");
				}
				if (element.select("div[class=c-summary c-row ]").size() > 0) {
					contentdesc = element.select("div[class=c-summary c-row ]").get(0).html();
				}
				if (element.select("div[class=c-span18 c-span-last]").size() > 0) {
					contentdesc = element.select("div[class=c-span18 c-span-last]").get(0).html();
				}
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
		List<Map<String, Object>> listnews =  baiduNewsService(constr);
		for (Map<String, Object> newmap : listnews) {
			System.out.println("##"+newmap);
			new SearchNews(constr, newmap).start();
		}
		SearchUtils.updateSearchFinish(constr); // 暂定开启新闻搜索所有线程后，新闻搜索完成
	}
}
