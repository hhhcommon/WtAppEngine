package com.woting.appengine.searchcrawler.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;
import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class XiMaLaYaService implements Callable<Map<String, Object>> {

	private int S_S_NUM = 2; // 搜索频道的数目
	private int S_F_NUM = 2; // 搜索频道内节目的数目
	private int F_NUM = 2; // 搜索节目的数目 以上排列顺序按照搜索到的排列顺序
	SearchUtils utils = new SearchUtils();
	private String constr;

	public XiMaLaYaService(String constr) {
		this.constr = constr;
	}

	public XiMaLaYaService() {
	}

	public Map<String, Object> XiMaLaYaService(String content) {
		Map<String, Object> map = new HashMap<String, Object>();
		List<Station> liststations = stationService(content);
		List<Festival> listfestivals = festivalService(content);
		if (liststations.isEmpty()) {
			map.put("XMLY_S", null);
		} else {
			map.put("XMLY_S", liststations);
		}
		if (listfestivals.isEmpty()) {
			map.put("XMLY_F", null);
		} else {
			map.put("XMLY_F", listfestivals);
		}
		return map;
	}

	public List<Station> stationService(String content) {
		String url = "http://www.ximalaya.com/search/" + utils.utf8TOurl(content) + "/t3";
		Document doc = null;
		List<Station> liststation = new ArrayList<Station>();
		try {
			doc = Jsoup.connect(url).timeout(3000).get();
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
				liststation.add(station);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return liststation;
	}

	/**
	 * 专辑下的节目搜索
	 * 
	 * @param contentid
	 * @return
	 */
	public Festival[] stationfestiavlS(String contentid) {
		String url = "http://www.ximalaya.com" + contentid;
		Festival[] festivals = new Festival[S_F_NUM];
		Document doc = null;
		try {
			doc = Jsoup.connect(url).timeout(5000).get();
			Elements elements = doc.select("li[sound_id]");
			for (int i = 0; i < (elements.size() > S_F_NUM ? S_F_NUM : elements.size()); i++) {
				Festival festival = new Festival();
				Element element = elements.get(i).select("a[class=forwardBtn]").get(0);
				festival.setAudioId(element.attr("track_id")); // 节目id
				festival.setAudioName(element.attr("track_title")); // 节目名称
				Elements elementsspan = elements.select("span");
				festival.setUpdateTime(elementsspan.get(0).html()); // 节目创建时间
				festival.setPlaynum(elementsspan.get(1).html()); // 节目播放次数
				festivals[i] = festivalS(festival.getAudioId(), festival);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return festivals;
	}

	public Festival festivalS(String contendid, Festival festival) {
		String url = "http://www.ximalaya.com/tracks/" + contendid + ".json";
		String jsonstr = utils.jsoupTOstr(url);
		Map<String, Object> map = utils.jsonTOmap(jsonstr);
		if (map == null) {
			return null;
		} else {
			festival.setPlayUrl(map.get("play_path") + ""); // 节目音频地址
			festival.setDuration(map.get("duration") + "000"); // 音频时长 ms
			festival.setAudioPic(map.get("cover_url_142") + ""); // 节目图片
			festival.setCategory(map.get("category_name") + ""); // 节目分类
			festival.setContentPub("喜马拉雅FM");
			return festival;
		}

	}

	public List<Festival> festivalService(String content) {
		String url = "http://www.ximalaya.com/search/" + utils.utf8TOurl(content) + "/t2";
		Document doc = null;
		List<Festival> listfestival = new ArrayList<Festival>();
		try {
			doc = Jsoup.connect(url).timeout(3000).get();
			Elements elements = doc.select("div[class=row soundReport]");
			if (elements.size() > 0) {
				elements.remove(0);
				for (int i = 0; i < (elements.size() > F_NUM ? F_NUM : elements.size()); i++) {
					Festival festival = new Festival();
					Element elf = elements.get(i);
					String name = elf.select("a[class=soundReport_soundname]").get(0).html();
					String id = elf.select("a[class=soundReport_soundname]").get(0).attr("href").split("/")[3];
					String desc = elf.select("a[class=soundReport_tag]").isEmpty() ? null
							: elf.select("a[class=soundReport_tag]").get(0).html();
					String host = elf.select("div[class=col soundReport_author]").isEmpty() ? null
							: elf.select("div[class=col soundReport_author]").get(0).select("a").get(0).html();
					String playnum = elf.select("div[class=col soundReport_playCount]").get(0).select("span").get(0)
							.html();
					festival.setAudioName(name);
					festival.setAudioId(id);
					festival.setAudioDes(desc);
					festival.setHost(host == null ? null : host.split(" ")[0]);
					festival.setPlaynum(playnum);
					listfestival.add(festivalS(festival.getAudioId(), festival));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return listfestival;
	}

	@Override
	public Map<String, Object> call() throws Exception {
		Map<String, Object> map = XiMaLaYaService(constr) == null ? null : XiMaLaYaService(constr);
		return map;
	}
}
