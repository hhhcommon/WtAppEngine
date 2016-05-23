package com.woting.appengine.searchcrawler.service;

import java.io.IOException;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class SearchNews extends Thread {

	private  Map<String, Object> map;
	private  String constr;
	
	public SearchNews(String constr, Map<String, Object> map) {
		this.constr = constr;
		this.map = map;
	}
	
	private static String getContentInfo(String url) {
		Document doc = null;
		String contentinfo = "";
		try {
			doc = Jsoup.connect(url).ignoreContentType(true)
					.header("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2")
					.timeout(5000).get();
			Elements elements = doc.getElementsByTag("p");
			if(elements!=null){
				for (Element element : elements) {
					if (!element.hasAttr("class")){//element.attr("class").equals("p15") || !element.hasAttr("class")) { // p15  工业和信息化部
						String constr = SearchUtils.cleanTag(element.toString());
						if (!SearchUtils.isOrNORemove(constr)) {
							if (!constr.equals("") && constr.length() > 9) {
								contentinfo += constr ;// + "\n";
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contentinfo;
	}
	
	@Override
	public void run() {
		String url = (String) map.get("ContentURL");
		if (!StringUtils.isNullOrEmptyOrSpace(url)) {
			String contenturi = getContentInfo(url);
			if(!StringUtils.isNullOrEmptyOrSpace(contenturi) && contenturi.length()>30){ // contenturi为抓取到的新闻内容
				map.put("ContentURI", contenturi);
				map.remove("ContentURL");
				SearchUtils.addListInfo(constr, map);
			}
		}
	}
}
