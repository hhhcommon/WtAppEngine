package com.woting.appengine.searchcrawler.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;
import com.woting.appengine.searchcrawler.utils.SearchUtils;


public class XiMaLaYaService extends Thread {

	private final int S_S_NUM = 4; // 搜索频道的数目
	private final int S_F_NUM = 2; // 搜索频道内节目的数目
	private final int F_NUM = 4; // 搜索节目的数目 以上排列顺序按照搜索到的排列顺序
	private final int T =5000;
	private static String constr;
	Map<String, Object> map = new HashMap<String,Object>();

	public static void begin(String constr){
		XiMaLaYaService.constr = constr;
		XiMaLaYaService xiMaLaYaService = new XiMaLaYaService();
		xiMaLaYaService.start();
	}
	
	private void ximalayaService(String content){
		stationS(content);
	    festivalsS(content);
	}

	private void stationS(String content) {
		String url = "http://www.ximalaya.com/search/" + SearchUtils.utf8TOurl(content) + "/t3";
		Document doc = null;
		try {
			doc = Jsoup.connect(url).timeout(T).get();
			Elements elements = doc.select("div[class=content_wrap2]");
			for (int i = 0; i < (elements.size() > S_S_NUM ? S_S_NUM : elements.size()); i++) {
				Station station = new Station();
				station.setContentPub("喜马拉雅FM");
				Element element1 = elements.get(i).select("a[class=albumface100]").get(0);
				String hrefstation = element1.attr("href");
				String stationpic = element1.select("span").select("img").attr("src");
				String[] strs = hrefstation.split("/");
				station.setId(strs[3]); // 专辑ID
				station.setPic(stationpic); // 专辑图片
				Element element2 = elements.get(i).select("div[class=info title]").select("a[href]").get(0);
				String stationname = element2.html();
				station.setName(stationname); // 专辑名称
				station.setContentPub("喜马拉雅FM");
				station.setFestival(stationfestiavlS(hrefstation));
				if(station!=null) SearchUtils.addListInfo(content, station); // 保存到在redis里key为constr的list里
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 专辑下的节目搜索
	 * 
	 * @param contentid
	 * @return
	 */
	private Festival[] stationfestiavlS(String contentid) {
		String url = "http://www.ximalaya.com" + contentid;
		Festival[] festivals = new Festival[S_F_NUM];
		Document doc = null;
		try {
			doc = Jsoup.connect(url).timeout(T).get();
			Elements elements = doc.select("li[sound_id]");
			for (int i = 0; i < (elements.size() > S_F_NUM ? S_F_NUM : elements.size()); i++) {
				Festival festival = new Festival();
				Element element = elements.get(i).select("a[class=forwardBtn]").get(0);
				festival.setAudioId(element.attr("track_id")); // 节目id
	//			festival.setAudioName(element.attr("track_title")); // 节目名称
				Elements elementsspan = elements.select("span");
				festival.setUpdateTime(elementsspan.get(0).html()); // 节目创建时间
	//			festival.setPlaynum(elementsspan.get(1).html()); // 节目播放次数
				festivals[i] = festivalS(festival.getAudioId(), festival);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return festivals;
	}

	private Festival festivalS(String contendid, Festival festival) {
		String url = "http://www.ximalaya.com/tracks/" + contendid + ".json";
		String jsonstr = SearchUtils.jsoupTOstr(url);
		Map<String, Object> map = SearchUtils.jsonTOmap(jsonstr);
		if (map == null) {
			return null;
		} else {
			festival.setAudioId(contendid);	//	节目id
			festival.setAudioName(map.get("title")+"");
			festival.setPlayUrl(map.get("play_path") + ""); // 节目音频地址
			festival.setDuration(map.get("duration") + "000"); // 音频时长 ms
			festival.setAudioPic(map.get("cover_url_142") + ""); // 节目图片
			festival.setCategory(map.get("category_name") + ""); // 节目分类
			festival.setPlaynum(map.get("play_count")+""); // 节目播放次数
			festival.setContentPub("喜马拉雅FM");
			return festival;
		}
	}

	private void festivalsS(String content) {
		String url = "http://www.ximalaya.com/search/" + SearchUtils.utf8TOurl(content) + "/t2";
		Document doc = null;
		try {
			doc = Jsoup.connect(url).timeout(T).get();
			Elements elements = doc.select("div[class=row soundReport]");
			if (elements.size() > 0) {
				elements.remove(0);
				for (int i = 0; i < (elements.size() > F_NUM ? F_NUM : elements.size()); i++) {
					Festival festival = new Festival();
					Element elf = elements.get(i);
//					String name = elf.select("a[class=soundReport_soundname]").get(0).html();
					String id = elf.select("a[class=soundReport_soundname]").get(0).attr("href").split("/")[3];
					String desc = elf.select("a[class=soundReport_tag]").isEmpty() ? null
							: elf.select("a[class=soundReport_tag]").get(0).html();
					String host = elf.select("div[class=col soundReport_author]").isEmpty() ? null
							: elf.select("div[class=col soundReport_author]").get(0).select("a").get(0).html();
					String playnum = elf.select("div[class=col soundReport_playCount]").get(0).select("span").get(0)
							.html();
		//			festival.setAudioName(name);
					festival.setAudioId(id);
					festival.setAudioDes(desc);
					festival.setHost(host == null ? null : host.split(" ")[0]);
					festival.setPlaynum(playnum);
					festival = festivalS(festival.getAudioId(), festival);
					if(festival!=null) SearchUtils.addListInfo(content, festival); // 保存到在redis里key为constr的list里
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		System.out.println("喜马拉雅搜索开始");
		try {
			ximalayaService(constr);
		} catch (Exception e) {
			System.out.println("喜马拉雅搜索异常");
		}finally {
			SearchUtils.updateSearchFinish(constr);
	     	System.err.println("喜马拉雅搜索结束");
		}
		
	}

}
