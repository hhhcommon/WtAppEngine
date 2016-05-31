package com.woting.appengine.searchcrawler.service;

import java.util.List;
import java.util.Map;
import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;
import com.woting.appengine.searchcrawler.utils.SearchUtils;

/**
 * 考拉搜索
 * 
 * @author wbq
 *
 */

public class KaoLaSearch extends Thread {

	private static int S_S_NUM = 4; // 搜索频道的数目
	private static int S_F_NUM = 2; // 搜索频道内节目的数目
	private static int F_NUM = 4; // 搜索节目的数目 以上排列顺序按照搜索到的排列顺序
	private String constr;

	public KaoLaSearch(String constr) {
		this.constr = constr;
	}
	
	private void kaolaService(String content){
		String str = SearchUtils.utf8TOurl(constr);
		StationS(str);
	    FestivalsS(str);
	}

	/**
	 * 频道信息及级下节目的搜索
	 * 
	 * @param content 搜索内容的url编码
	 * @return 返回搜索到的频道及级下节目
	 */
	private void StationS(String content) {
		String station_url = "http://www.kaolafm.com/webapi/resource/search?words=" + content
				+ "&rtype=20000&pagesize=20&pagenum=1";
		String station_id = new String();
		String jsonstr = SearchUtils.jsoupTOstr(station_url);
		List<Map<String, Object>> list_href = SearchUtils.jsonTOlist(jsonstr, "result", "dataList");
		if (!list_href.isEmpty()) {
			for (int i = 0; i < (list_href.size() > S_S_NUM ? S_S_NUM : list_href.size()); i++) {
				// 频道信息采集
				Festival[] festivals = new Festival[S_F_NUM];
				String S_host_name = ""; // 频道主播人名称
				Station station = new Station();
				station.setContentPub("考拉FM");
				station_id = list_href.get(i).get("id").toString();
				station.setId(list_href.get(i).get("id").toString());
				station.setName(list_href.get(i).get("name").toString());
				station.setPic(list_href.get(i).get("pic").toString());
				station.setDesc(list_href.get(i).get("desc").toString());
				List<Map<String, Object>> list_host = (List<Map<String, Object>>) list_href.get(i).get("host");
				for (Map<String, Object> map : list_host) {
					S_host_name += "," + map.get("name");
				}
				if (S_host_name.length() > 0) {
					S_host_name = S_host_name.substring(1);
				}
				station.setHost(S_host_name);
				String festival_url = "http://www.kaolafm.com/webapi/audios/list?id=" + station_id
						+ "&pagesize=20&pagenum=1&sorttype=1";
				jsonstr = SearchUtils.jsoupTOstr(festival_url);
				List<Map<String, Object>> list_href2 = SearchUtils.jsonTOlist(jsonstr, "result", "dataList");
				if (list_href2.size() > 0) {
					for (int j = 0; j < (list_href2.size() > S_F_NUM ? S_F_NUM : list_href2.size()); j++) {
						// 频道里节目信息采集
						String F_host_name = ""; // 节目主播人
						Festival festival = new Festival();
						festival.setContentPub("考拉FM");
						festival.setAudioId(list_href2.get(j).get("audioId").toString());
						festival.setAudioName(list_href2.get(j).get("audioName").toString());
						festival.setAudioPic(list_href2.get(j).get("audioPic")==null?null:list_href2.get(j).get("audioPic").toString()); // 频道里节目信息没有图片链接
						festival.setAudioDes(list_href2.get(j).get("audioDes").toString());
						festival.setAlbumName(list_href2.get(j).get("albumName")==null?null:list_href2.get(j).get("albumName").toString());
						festival.setAlbumPic(list_href2.get(j).get("albumPic")==null?null:list_href2.get(j).get("albumPic").toString());
						festival.setPlayUrl((list_href2.get(j).get("m3u8PlayUrl").toString()));
						festival.setFileSize((list_href2.get(j).get("fileSize").toString()));
						festival.setDuration(((list_href2.get(j).get("duration").toString())));
						festival.setUpdateTime(((list_href2.get(j).get("updateTime").toString())));
						festival.setListenNum(((list_href2.get(j).get("listenNum").toString())));
						List<Map<String, Object>> F_list_host = (List<Map<String, Object>>) list_href2.get(j).get("host");
						for (Map<String, Object> map : F_list_host)
							F_host_name += "," + map.get("name").toString();
						if (F_host_name.length() > 0)
							F_host_name = F_host_name.substring(1);
						festival.setHost(F_host_name);
						festivals[j] = festival;
					}
					station.setFestival(festivals);
				}
				if (station!=null) 
					SearchUtils.addListInfo(constr, station); // 保存到在redis里key为constr的list里
			}
		} 
	}

	/**
	 * 节目信息的搜索
	 * 
	 * @param content 搜索内容的url编码
	 * @return 返回搜索到的节目信息
	 */
	private void FestivalsS(String content) {
		String url = "http://www.kaolafm.com/webapi/resource/search?words=" + content
				+ "&rtype=30000&pagesize=20&pagenum=1";
		String jsonstr = SearchUtils.jsoupTOstr(url); // 获取网页链接的返回结果
		List<Map<String, Object>> list = SearchUtils.jsonTOlist(jsonstr, "result", "dataList"); // 获取json数据解析后的map对象
		if (!list.isEmpty()) {
			for (int i = 0; i < (list.size() > F_NUM ? F_NUM : list.size()); i++) {
				String festival_id = list.get(i).get("id").toString();
				// 节目音频查询
				url = "http://www.kaolafm.com/webapi/audiodetail/get?id=" + festival_id;
				jsonstr = SearchUtils.jsoupTOstr(url); // 获取网页链接的返回json结果
				Map<String, Object> map = SearchUtils.jsonTOmap(jsonstr, "result");
				List<Map<String, Object>> list_host = (List<Map<String, Object>>) map.get("host");
				String host_name = "";
				Festival festival = new Festival();
				festival.setContentPub("考拉FM");
				festival.setAudioId(map.get("audioId").toString());
				festival.setAudioName(map.get("audioName").toString());
				festival.setAudioPic(map.get("audioPic")==null?"":map.get("audioPic").toString());
				festival.setAudioDes(map.get("audioDes").toString());
				festival.setAlbumName(map.get("albumName").toString());
				festival.setAlbumPic(map.get("albumPic")==null?"":map.get("albumPic").toString());
				festival.setPlayUrl(map.get("m3u8PlayUrl").toString());
				festival.setFileSize(map.get("fileSize").toString());
				festival.setDuration(map.get("duration").toString());
				festival.setUpdateTime(map.get("updateTime").toString());
				festival.setListenNum(map.get("listenNum").toString());
				for (Map<String, Object> map2 : list_host)
					host_name += "," + map2.get("name").toString();
				if (host_name.length() > 0)
					host_name = host_name.substring(1);
				festival.setHost(host_name);
				if (festival!=null)
					SearchUtils.addListInfo(constr, festival); // 保存到在redis里key为constr的list里
			}
		}
	}

	@Override
	public void run() {
		System.out.println("考拉开始搜索");
		try {
			kaolaService(constr);
		} catch (Exception e) {
			System.out.println("考拉搜索异常");
		}
		finally {
			SearchUtils.updateSearchFinish(constr);
			System.out.println("考拉搜索结束");
		}
	}

}
