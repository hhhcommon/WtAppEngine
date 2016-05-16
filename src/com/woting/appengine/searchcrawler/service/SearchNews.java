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
			doc = Jsoup.connect(url).ignoreContentType(true).timeout(5000).get();
			Elements elements = doc.getElementsByTag("p");
			if(elements!=null){
				for (Element element : elements) {
					if (!element.hasAttr("class")) {
						String constr = SearchUtils.cleanTag(element.toString());
						if (!SearchUtils.isOrNORemove(constr)) {
							if (!constr.equals("") && constr.length() > 9) {
								contentinfo += constr + "\n";
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
			if(!StringUtils.isNullOrEmptyOrSpace(contenturi)){
				map.put("ContentURI", contenturi);
				map.remove("ContentURL");
				SearchUtils.addListInfo(constr, map);
			}
		}
	}
}
