package com.woting.appengine.searchcrawler.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.spiritdata.framework.util.JsonUtils;
import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;
import com.woting.appengine.searchcrawler.utils.SearchUtils;

@Service
public class QingTingService extends Thread {

	private final int S_S_NUM = 2; // 搜索频道的数目
	private final int F_NUM = 2; // 搜索节目的数目 以上排列顺序按照搜索到的排列顺序
	private final int T = 5000;
	private static String constr = "";
	Map<String, Object> map = new HashMap<String,Object>();

	public static void begin(String constr){
		QingTingService.constr = constr;
		QingTingService qts= new QingTingService();
		qts.start();
	}

	// 电台搜索链接请求
	public Map<String, Object> qingtingService(String content) {
		content = SearchUtils.utf8TOurl(content);
		Map<String, Object> map = new HashMap<String, Object>();
		String station_url = "http://www.qingting.fm/s/search/" + content;
		List<Festival> list_festival = new ArrayList<Festival>();
		List<Station> list_station = new ArrayList<Station>();
		Document doc = null;
		try {
			doc = Jsoup.connect(station_url).ignoreContentType(true).timeout(T).get();
			// 获取频道json数据
			Elements elements = doc.select("ul[class=nav]");
			Elements elements_festivals = elements.get(0).select("li[jump-to=search-virtualprograms]");
			Elements elements_stations = elements.get(0).select("li[jump-to=search-virtualchannels]");
			Elements elements_radios = elements.get(0).select("li[jump-to=search-channels]");
			String festival_num = elements_festivals.select("a[href]").html();
			String station_num = elements_stations.select("a[href]").html();
			String radio_num = elements_radios.select("a[href]").html();
			int r_num = SearchUtils.findint(radio_num); // 电台数量
			int s_num = SearchUtils.findint(station_num); // 频道数量
			int f_num = SearchUtils.findint(festival_num);// 节目数量
			elements = doc.select("li[class=playable clearfix]");
			for (int i = r_num; i < r_num + s_num + F_NUM; i++) {
				String title = elements.get(i).select("a[href]").get(0).select("span").get(0).html();
				String href = "http://www.qingting.fm" + elements.get(i).select("a[href]").get(0).attr("data-switch-url");
				if (i >= r_num + s_num) {
					Festival festival = new Festival();
					festival = festivalS(href);
					festival.setAudioName(title);
					list_festival.add(festival);
				}
				if (i < r_num + s_num) {
					Station station = new Station();
					station = stationS(href);
					station.setId(href.replaceAll("http://www.qingting.fm/s/vchannels/", ""));
					station.setName(title);
					station.setContentPub("蜻蜓FM");
					if(station!=null)SearchUtils.updateListInfo(constr, station); // 保存到在redis里key为constr的list里
					list_station.add(station);
					if (i == r_num + (s_num > (S_S_NUM - 1) ? (S_S_NUM - 1) : s_num) && (r_num + s_num) > 0) {
						i = r_num + s_num - 1;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		map.put("QT_S", list_station);
		map.put("QT_F", list_festival);
		return map;
	}

	/**
	 * 节目信息转换
	 * 
	 * @param url
	 * @return
	 */
	public Festival festivalS(String url) {
		Document doc = null;
		Festival festival = new Festival();
		try {
			doc = Jsoup.connect(url).ignoreContentType(true).timeout(3000).get();
			if (doc.select("li[class=playable auto-play clearfix]").size() > 0) {
				Element element = doc.select("li[class=playable auto-play clearfix]").get(0);
				String jsonstr = element.attr("data-play-info").replaceAll("&quot;", "\"");
				Map<String, Object> testmap = (Map<String, Object>) JsonUtils.jsonToObj(jsonstr, Map.class);
				festival.setAudioName(testmap.get("name").toString());
				festival.setAudioId(testmap.get("id").toString());
				festival.setMediaType("AUDIO");
				festival.setContentPub("蜻蜓FM");
				festival.setAudioPic(testmap.get("thumb").toString());
				festival.setDuration(testmap.get("duration") + "000");
				List<String> list_urls = (List<String>) testmap.get("urls");
				String m4aurl = "http://od.qingting.fm" + list_urls.get(0).toString();
				festival.setPlayUrl(m4aurl);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(festival!=null)SearchUtils.updateListInfo(constr, festival); // 保存到在redis里key为constr的list里
		return festival;
	}

	/**
	 * 频道信息转换
	 * 
	 * @param url
	 * @return
	 */
	public Station stationS(String url) {
		Document doc = null;
		Station station = new Station();
		Festival[] festivals = new Festival[1];
		Festival festival = new Festival();
		try {
			doc = Jsoup.connect(url).timeout(T).get();
			Element element = doc.select("li[class=playable clearfix]").get(0);
			String descstr = doc.select("div[class=channel-info clearfix]").get(0).select("div[class=content]").get(0)
					.html();
			station.setDesc(descstr);
			String jsonstr = element.attr("data-play-info").replaceAll("&quot;", "\"");
			Map<String, Object> testmap = (Map<String, Object>) JsonUtils.jsonToObj(jsonstr, Map.class);
			festival.setAudioName(testmap.get("name").toString());
			festival.setAudioId(testmap.get("id").toString());
			festival.setMediaType("AUDIO");
			festival.setContentPub("蜻蜓FM");
			festival.setAudioPic(testmap.get("thumb").toString());
			festival.setDuration(testmap.get("duration") + "000");
			List<String> list_urls = (List<String>) testmap.get("urls");
			String m4aurl = "http://od.qingting.fm" + list_urls.get(0).toString();
			festival.setPlayUrl(m4aurl);
			festivals[0] = festival;
			station.setFestival(festivals);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return station;
	}
	
	@Override
	public void run() {
		System.out.println("蜻蜓开始搜索");
		qingtingService(constr);
		System.out.println("蜻蜓结束搜索");
	}
}
