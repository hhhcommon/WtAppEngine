package com.woting.appengine.searchcrawler.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.springframework.dao.support.DaoSupport;

import com.spiritdata.framework.util.JsonUtils;
import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class qt_s_service {
	
	SearchUtils utils = new SearchUtils();
	
	//电台搜索链接请求
		public void qt_S(String url){
			url = utils.utf8TOurl(url);
			String station_url = "http://www.qingting.fm/s/search/" + url;
			List<Festival> list_festival = new ArrayList<Festival>();
			Document doc = null;
			try {
				doc = Jsoup.connect(station_url).ignoreContentType(true).timeout(5000).get();
				//获取频道json数据
				Elements elements = doc.select("ul[class=nav]");
				Elements elements_festivals = elements.get(0).select("li[jump-to=search-virtualprograms]");
				Elements elements_stations = elements.get(0).select("li[jump-to=search-virtualchannels]");
				Elements elements_radios = elements.get(0).select("li[jump-to=search-channels]");
				String festival_num = elements_festivals.select("a[href]").html();
				String station_num = elements_stations.select("a[href]").html();
				String radio_num = elements_radios.select("a[href]").html();
				int r_num = utils.findint(radio_num);
				int s_num = utils.findint(station_num);
				int f_num = utils.findint(festival_num);
				System.out.println(f_num+"##"+s_num+"##"+r_num);
				elements = doc.select("li[class=playable clearfix]");
				for (int i = r_num;i<r_num+s_num+3;i++ ) {
					String title = elements.get(i).select("a[href]").get(0).select("span").html();
					String href = "";
			//		System.out.println(title+"##"+href);
					if (i==r_num+1) {
						i = r_num+s_num;
					}
					if(i<r_num+s_num){
						href = "http://www.qingting.fm"+elements.get(i).select("a[href]").get(0).attr("data-switch-url");
					}
					if (i>=r_num+s_num) {
						href = "http://www.qingting.fm"+elements.get(i).select("a[href]").get(0).attr("data-switch-url");
						Festival festival = new Festival();
						festival = festivalS(href);
						festival.setAudioName(title);
						list_festival.add(festival);
						System.out.println(festival.toString());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public Festival festivalS(String url){
			Document doc =null;
			Festival festival = new Festival();
			try {
				doc = Jsoup.connect(url).ignoreContentType(true).timeout(3000).get();
				Element element = doc.select("li[class=playable auto-play clearfix]").get(0);
				String jsonstr = element.attr("data-play-info").replaceAll("&quot;", "\"");
				Map<String, Object> testmap =  (Map<String, Object>) JsonUtils.jsonToObj(jsonstr, Map.class);
				festival.setAudioId(testmap.get("id").toString());
				festival.setAudioPic(testmap.get("thumb").toString());
				festival.setDuration(testmap.get("duration")+"000");
				List<String> list_urls = (List<String>) testmap.get("urls");
				String m4aurl = "http://od.qingting.fm"+list_urls.get(0).toString();
				festival.setPlayUrl(m4aurl);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return festival;
			
		}
}
