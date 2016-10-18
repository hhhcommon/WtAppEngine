package com.woting.appengine.searchcrawler.service;

import java.io.IOException;
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
import com.spiritdata.framework.util.JsonUtils;
import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;
import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class QingTingSearch extends Thread {

	private static int S_S_NUM = 4; // 搜索频道的数目
	private static int F_NUM = 4; // 搜索节目的数目 以上排列顺序按照搜索到的排列顺序
	private static int T = 5000;
	private String constr = "";

	public QingTingSearch(String constr) {
		this.constr = constr;
	}

	// 电台搜索链接请求
	private void qingtingService(String content) {
		content = SearchUtils.utf8TOurl(content);
		String station_url = "http://www.qingting.fm/s/search/" + content;
		Document doc = null;
		try {
			doc = Jsoup.connect(station_url).ignoreContentType(true).timeout(T).get();
			// 获取频道json数据
			Elements elements = doc.select("ul[class=nav]");
			Elements elements_stations = elements.get(0).select("li[jump-to=search-virtualchannels]");
			Elements elements_radios = elements.get(0).select("li[jump-to=search-channels]");
			String station_num = elements_stations.select("a[href]").html();
			String radio_num = elements_radios.select("a[href]").html();
			int r_num = SearchUtils.findint(radio_num); // 电台数量
			int s_num = SearchUtils.findint(station_num); // 频道数量
			elements = doc.select("li[class=playable clearfix]");
			for (int i = r_num; i < r_num + s_num + F_NUM; i++) {
				String title = elements.get(i).select("a[href]").get(0).select("span").get(0).html();
				String href = "http://www.qingting.fm" + elements.get(i).select("a[href]").get(0).attr("data-switch-url");
				if (i >= r_num + s_num) {
					Festival festival = new Festival();
					festival = festivalS(href);
					festival.setAudioName(title);
					if(festival!=null) {
			            ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
			            if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
			                JedisConnectionFactory conn=(JedisConnectionFactory)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("connectionFactory");
			                RedisOperService roService=new RedisOperService(conn);
			                SearchUtils.addListInfo(constr, festival, roService);//保存到在redis里key为constr的list里
			            }
					}
				}
				if (i < r_num + s_num) {
					Station station = new Station();
					station = stationS(href);
					station.setId(href.replaceAll("http://www.qingting.fm/s/vchannels/", ""));
					station.setName(title);
					station.setContentPub("蜻蜓FM");
					if(station!=null) {
                        ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
                        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                            JedisConnectionFactory conn=(JedisConnectionFactory)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("connectionFactory");
                            RedisOperService roService=new RedisOperService(conn);
                            SearchUtils.addListInfo(constr, station, roService); // 保存到在redis里key为constr的list里
                        }
					}
					if (i == r_num + (s_num > (S_S_NUM - 1) ? (S_S_NUM - 1) : s_num) && (r_num + s_num) > 0) {
						i = r_num + s_num - 1;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 节目信息获取
	 * 
	 * @param url
	 * @return
	 */
	private Festival festivalS(String url) {
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
		return festival;
	}

	/**
	 * 频道信息转换
	 * 
	 * @param url
	 * @return
	 */
	private Station stationS(String url) {
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
		try {
			qingtingService(constr);
		} catch (Exception e) {
			System.out.println("蜻蜓搜索异常");
		}
		finally {
            ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
            if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                JedisConnectionFactory conn=(JedisConnectionFactory)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("connectionFactory");
                RedisOperService roService=new RedisOperService(conn);
                SearchUtils.updateSearchFinish(constr, roService);
            }
		    System.out.println("蜻蜓结束搜索");
		}
	}
}
