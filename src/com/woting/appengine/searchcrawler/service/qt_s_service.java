package com.woting.appengine.searchcrawler.service;

import java.io.IOException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class qt_s_service {
	
	SearchUtils utils = new SearchUtils();
	
	@Test
	public void test(){
		String content = utils.utf8TOurl("周");
		StationS(content);
		
	}
	
	
	//电台搜索链接请求
		public void StationS(String url){
			String station_url = "http://www.qingting.fm/s/search/" + url;
			Document doc = null;
			try {
				doc = Jsoup.connect(station_url).ignoreContentType(true).timeout(3000).get();
				//获取频道json数据
				Elements elements = doc.select("ul[class=nav]");
				Elements elements_festivals = elements.get(0).select("li[jump-to=search-virtualprograms]");
				Elements elements_stations = elements.get(0).select("li[jump-to=search-virtualchannels]");
				Elements elements_radios = elements.get(0).select("li[jump-to=search-channels]");
				String festival_num = elements_festivals.select("a[href]").html();
				String station_num = elements_stations.select("a[href]").html();
				String radio_num = elements_radios.select("a[href]").html();
				int f_num = utils.findint(festival_num);
				int s_num = utils.findint(station_num);
				int r_num = utils.findint(radio_num);
				System.out.println(f_num+"##"+s_num+"##"+r_num);
			/*	Elements elements = doc.select("li[class=playable clearfix]");
				for (int i = 0;i<4;i++ ) {
					String title = elements.get(i).select("a[href]").get(0).select("span").html();
					String href = elements.get(i).select("a[href]").get(0).attr("data-switch-url");
					System.out.println(title+"##"+href);
				}*/
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
}
