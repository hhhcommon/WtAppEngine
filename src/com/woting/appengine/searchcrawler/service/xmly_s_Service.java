package com.woting.appengine.searchcrawler.service;

import java.io.IOException;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class xmly_s_Service {
	
	SearchUtils utils = new SearchUtils();
	
	public Map<String, Object> xmlyService(String content){
		String url = "http://www.ximalaya.com/search/"+utils.utf8TOurl(content)+"/t3";
		Document doc = null;
		try {
			doc = Jsoup.connect(url).timeout(3000).get();
			Elements elements = doc.select("li[class=item]").select("a[class=albumface100]");
			System.out.println(elements);
			for (Element element : elements) {
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
