package com.woting.appengine.searchcrawler.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;
import com.woting.appengine.searchcrawler.utils.SearchUtils;

public class XiMaLaYaService {
	
	private int S_S_NUM = 1;		//搜索频道的数目
	private int S_F_NUM = 2;	//搜索频道内节目的数目
	private int F_NUM = 2;		//搜索节目的数目     以上排列顺序按照搜索到的排列顺序
	SearchUtils utils = new SearchUtils();
	
	public Map<String, Object> xmlyService(String content){
		stationS(content);
		return null;
	}
	
	public List<Station> stationS(String content){
		String url = "http://www.ximalaya.com/search/"+utils.utf8TOurl(content)+"/t3";
		Document doc = null;
		List<Station> liststation = new ArrayList<Station>();
		try {
			doc = Jsoup.connect(url).timeout(3000).get();
			Elements elements = doc.select("li[class=item]");
			elements.remove(0);
		//	System.out.println(elements);
			for(int i=0;i<S_S_NUM;i++){
				Station station = new Station();
				station.setContentPub("喜马拉雅FM");
				Element element1 = elements.get(i).select("a[class=albumface100]").get(0);
		//		System.out.println(element1);
				String hrefstation = element1.attr("href");
				String stationpic = element1.select("span").select("img").attr("src");
		//		System.out.println(hrefstation);
				String[] strs = hrefstation.split("/");
				station.setId(strs[3]);			//专辑ID
				station.setPic(stationpic);		//专辑图片
				Element element2 = elements.get(i).select("div[class=info title]").select("a[href]").get(0);
				String stationname = element2.html();
				station.setName(stationname);	//专辑名称
		//		System.out.println(station.toString());
		//		System.out.println(Jsoup.connect("http://www.ximalaya.com"+hrefstation).get());
				stationfestiavlS(hrefstation);
				liststation.add(station);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return liststation;
	}
	
	/**
	 * 专辑下的节目搜索
	 * @param contentid
	 * @return
	 */
	public List<Festival> stationfestiavlS(String contentid){
		String url = "http://www.ximalaya.com"+contentid;
		Document doc = null;
		List<Festival> listfestival = new ArrayList<Festival>();
		try {
			doc = Jsoup.connect(url).timeout(3000).get();
			Elements elements = doc.select("li[sound_id]");
			for(int i=0;i<S_F_NUM;i++){
				Festival festival = new Festival();
				Element element = elements.get(i).select("a[class=forwardBtn]").get(0);
				festival.setAudioId(element.attr("track_id"));		//节目id
				festival.setAlbumName(element.attr("track_title")); //节目名称
				Elements elementsspan = elements.select("span");
				festival.setCreateTime(elementsspan.get(0).html());	//节目创建时间
				festival.setPlaynum(elementsspan.get(1).html());	//节目播放次数
				festivalS(festival.getAudioId(), festival);
				listfestival.add(festival);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return listfestival;
	}
	
	public Festival festivalS(String contendid,Festival festival){
		String url = "http://www.ximalaya.com/tracks/"+contendid+".json";
		String jsonstr = utils.jsoupTOstr(url);
		Map<String, Object> map = utils.jsonTOmap(jsonstr);
		festival.setPlayUrl(map.get("play_path")+"");		//节目音频地址
		festival.setDuration(map.get("duration")+"000");	//音频时长  ms
		festival.setAudioPic(map.get("cover_url_142")+"");	//节目图片
		festival.setCategory(map.get("category_name")+"");	//
		return festival;
		
	}
}
