package com.woting.appengine.search.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.mysql.jdbc.StringUtils;
import com.woting.appengine.search.model.Festival;
import com.woting.appengine.search.model.Station;
import com.woting.appengine.search.utils.SearchUtils;

/**
 * 考拉搜索
 * @author wbq
 *
 */

public class kl_s_service {

	private int S_S_T = 2;		//搜索频道的数目
	private int S_S_F_T = 2;	//搜索频道内节目的数目
	private int S_F_T = 2;		//搜索节目的数目     以上排列顺序按照搜索到的排列顺序
	SearchUtils utils = new SearchUtils(5000);	//搜索工具
	
	@Test
	public void test(){
		String content = utils.utf8TOurl("周小儿");
		List<Station> list_station = StationS(content);
		List<Festival> list_festival = FestivalsS(content);
		if(list_station.isEmpty() || list_station.equals("")){
			System.out.println("抱歉，频道未搜索到结果！");
		}else{
			System.out.println("频道"+list_station.size());
		}
		if(list_festival.isEmpty() || list_festival.equals("")){
			System.out.println("抱歉，节目未搜索到结果！");
		}else {
			System.out.println("节目"+list_festival.size());
		}
	}
	
	
	/**
	 * 频道信息及级下节目的搜索
	 * @param content 搜索内容的url编码
	 * @return 返回搜索到的频道及级下节目
	 */
	public List<Station> StationS(String content){
		String station_url = "http://www.kaolafm.com/webapi/resource/search?words=" + content +
				"&rtype=20000&pagesize=20&pagenum=1";
		List<Station> list_station = new  ArrayList<Station>();
		Festival[] festivals = new Festival[S_S_F_T];
		String station_id = new String();
		String jsonstr = utils.jsoupTOstr(station_url);
		List<Map<String, Object>> list_href = utils.jsonTOlist(jsonstr, "result","dataList");
		if(!list_href.isEmpty()){
			for(int i = 0;i<(list_href.size()>S_S_T?S_S_T:list_href.size());i++){
				//频道信息采集
				Station station = new Station();
				station_id=list_href.get(i).get("id").toString();
				station.setId(list_href.get(i).get("id").toString());
				station.setName(list_href.get(i).get("name").toString());
				station.setPic(list_href.get(i).get("pic").toString());
				station.setDesc(list_href.get(i).get("desc").toString());
				String festival_url = "http://www.kaolafm.com/webapi/audios/list?id="+station_id+
						"&pagesize=20&pagenum=1&sorttype=1";
				jsonstr = utils.jsoupTOstr(festival_url);
				List<Map<String, Object>> list_href2=utils.jsonTOlist(jsonstr, "result","dataList");
				if(list_href2.size()>0){
					for(int j = 0;j<(list_href2.size()>S_S_F_T?S_S_F_T:list_href2.size());j++){
						//频道里节目信息采集
						
						Festival festival  = new Festival();
						festival.setAudioName(list_href2.get(j).get("audioName").toString());
						festival.setAudioPic(utils.ObjIsEmpty(list_href.get(j).get("audioPic")));  //频道里节目信息没有图片链接
						festival.setAlbumName(list_href2.get(j).get("albumName")==null?"":"".toString());
						festival.setAlbumPic(utils.ObjIsEmpty(list_href2.get(j).get("albumPic")));
						festival.setMp3PlayUrl((list_href2.get(j).get("mp3PlayUrl").toString()));
						festival.setFileSize((list_href2.get(j).get("fileSize").toString()));
						festival.setUpdateTime(((list_href2.get(j).get("updateTime").toString())));
						festival.setListenNum(((list_href2.get(j).get("listenNum").toString())));
						festivals[j] = festival;
					}
					station.setFestival(festivals);
					list_station.add(station);
				}
			}
		}else {
			list_station.clear();
		}
		return list_station;
	}
	
	/**
	 * 节目信息的搜索
	 * @param content 搜索内容的url编码
	 * @return 返回搜索到的节目信息
	 */
	public List<Festival> FestivalsS(String content){
		String url = "http://www.kaolafm.com/webapi/resource/search?words="+content+"&rtype=30000&pagesize=20&pagenum=1";
		List<Festival> list_festival = new ArrayList<Festival>();
		String jsonstr = utils.jsoupTOstr(url);	//获取网页链接的返回结果
		List<Map<String, Object>> list = utils.jsonTOlist(jsonstr, "result","dataList");  //获取json数据解析后的map对象
		if(!list.isEmpty()){
			for(int i = 0;i<(list.size()>S_F_T?S_F_T:list.size());i++){
				String festival_id = list.get(i).get("id").toString();
				//节目音频查询
				url = "http://www.kaolafm.com/webapi/audiodetail/get?id="+festival_id;
				jsonstr = utils.jsoupTOstr(url);	//获取网页链接的返回结果
				Map<String, Object> map = utils.jsonTOmap(jsonstr, "result");	
				Festival festival  = new Festival();
				festival.setAudioName(map.get("audioName").toString());
				festival.setAudioPic(utils.ObjIsEmpty(map.get("audioPic"))); 
				festival.setAlbumName(map.get("albumName").toString());
				festival.setAlbumPic(utils.ObjIsEmpty(map.get("albumPic")));
				festival.setMp3PlayUrl((map.get("mp3PlayUrl").toString()));
				festival.setFileSize((map.get("fileSize").toString()));
				festival.setUpdateTime(((map.get("updateTime").toString())));
				festival.setListenNum(((map.get("listenNum").toString())));
				list_festival.add(festival);
			}
		}else {
			list_festival.clear();
		}
		return list_festival;
	}
	
}
