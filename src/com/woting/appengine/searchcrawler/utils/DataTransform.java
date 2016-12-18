package com.woting.appengine.searchcrawler.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.spiritdata.framework.util.FileNameUtils;
import com.spiritdata.framework.util.JsonUtils;
import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;

/**
 * 数据转换
 * 
 * @author wbq
 *
 */
public abstract class DataTransform {

	/**
	 * 多festival转audio
	 * 
	 * @param list_Festival 搜索到的节目信息
	 * @param list_Station 搜索到的频道信息
	 * @param PageType 如果PageType=0,把专辑的第一个节目提取出来，如果等于1，频道信息在此方法不处理
	 * @return 返回处理好的audio信息
	 */
	public static List<Map<String, Object>> datas2Audio(List<Festival> list_Festival, List<Station> list_Station, int PageType) {
		if (list_Festival == null || list_Station == null) return null;
		List<Map<String, Object>> list_AudioData = new ArrayList<Map<String, Object>>();
		if (list_Festival.size() > 0) {
			for (Festival festival : list_Festival) {
				if (festival != null) {
					// ContentCatalogs内容分类、ContentKeyWord关键词、ContentSubjectWord主题词播放次数未定义参数
					Map<String, Object> map = new HashMap<String, Object>();
					map = festival2Audio(festival);
					list_AudioData.add(map);
				}
			}
		}
		if (PageType == 0) {
			if (list_Station.size() > 0) {
				for (int i = 0; i < list_Station.size(); i++) {
					if (list_Station.get(i).getFestival() != null) {
						if (list_Station.get(i).getFestival().length > 0) {
							Map<String, Object> map = datas2Sequ_Audio(list_Station.get(i));
							if (map != null)
								list_AudioData.add(map);
						}
					}
				}
			}
		}
		return list_AudioData;
	}
	
	public static List<Map<String, Object>> data2Audio(List<String> list){
		List<Map<String, Object>> listdata = new ArrayList<Map<String,Object>>();
		Map<String, Object> map = new HashMap<String,Object>();
		for (String string : list) {
			if(string.contains("AUDIO")) 
				map = festival2Audio((Festival)JsonUtils.jsonToObj(string, Festival.class));
			else {
				if(string.contains("SEQU"))
					map = station2Sequ((Station)JsonUtils.jsonToObj(string, Station.class));
			}
			if(!map.isEmpty()) listdata.add(map);
		}
		return listdata;
	}

	/**
	 * 多专辑数据转换
	 * 
	 * @param list_Stations
	 * @return
	 */
	public static List<Map<String, Object>> datas2Sequ(List<Station>... list_Stations) {
		if (list_Stations == null) {
			return null;
		}
		List<Map<String, Object>> list_SequData = new ArrayList<Map<String, Object>>();
		for (List<Station> list_Station : list_Stations) {
			if (list_Station.size() > 0) {
				for (Station station : list_Station) {
					// ContentCatalogs内容分类、ContentKeyWord关键词、ContentSubjectWord主题词和
					Map<String, Object> map = new HashMap<String, Object>();
					map = station2Sequ(station);
					list_SequData.add(map);
				}
			}
		}
		return list_SequData;
	}

	/**
	 * 提出专辑里的第一个节目
	 * 
	 * @param list_stations
	 * @return
	 */
	public static Map<String, Object> datas2Sequ_Audio(Station station) {
		if (station.getFestival() == null) return null;
		Map<String, Object> map = new HashMap<String, Object>();
		if (station.getFestival()[0] != null) {
			Festival festival = station.getFestival()[0];
			map = festival2Audio(festival);
			map.put("SeqInfo", station2Sequ(station));
			return map;
		} else return null;
	}

	/**
	 * 单festival转audio
	 * 
	 * @param festival
	 * @return
	 */
	public static Map<String, Object> festival2Audio(Festival festival) {
		// ContentCatalogs内容分类、ContentKeyWord关键词、ContentSubjectWord主题词和PlayCount播放次数未定义参数
		if (festival == null) return null;
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("ContentId", festival.getAudioId());
		map.put("ContentName", festival.getAudioName());
		map.put("ContentURI", "content/getContentInfo.do?MediaType=AUDIO&ContentId=" + festival.getAudioId());
		map.put("ContentImg", festival.getAudioPic());
		map.put("ContentPlay", festival.getPlayUrl());
		map.put("ContentImg", festival.getAudioPic());
		map.put("ContentPersons", festival.getPersonName());
		map.put("ContentTimes", festival.getDuration());// 以ms为计量单位
		map.put("ContentPubTime", festival.getUpdateTime());
		map.put("ContentPub", festival.getContentPub());
		map.put("ContentDescn", festival.getAudioDes());
		map.put("CTime", festival.getUpdateTime());
		map.put("MediaType", festival.getMediaType());
		map.put("ContentCatalogs", null);
		map.put("ContentKeyWord", null);
		map.put("ContentSubjectWord", null);
		map.put("PlayCount", "1234");
		
		String ext = FileNameUtils.getExt(map.containsKey("ContentPlay")?(map.get("ContentPlay")+""):null);
        if (ext.contains("?")) {
			ext = ext.replace(ext.substring(ext.indexOf("?"), ext.length()), ""); 
		}
        if (ext!=null) {
        	map.put("ContentPlayType", ext.contains("/flv")?"flv":ext.replace(".", ""));
		} else {
			map.put("ContentPlayType", null);
		}
		return map;
	}

	/**
	 * 单station转sequ
	 * 
	 * @param station
	 * @return
	 */
	public static Map<String, Object> station2Sequ(Station station) {
		// ContentCatalogs内容分类、ContentKeyWord关键词、ContentSubjectWord主题词和PlayCount播放次数
		if (station == null) return null;
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("ContentSubCount", (station.getFestival().length) + "");
		map.put("ContentURI", "content/getContentInfo.do?MediaType=SEQU&ContentId=" + station.getId());
		map.put("ContentPersons", station.getHost());
		map.put("CTime", station.getCTime());
		map.put("ContentName", station.getName());
		map.put("ContentPub", station.getContentPub());
		map.put("MediaType", station.getMediaType());
		map.put("ContentId", station.getId());
		map.put("ContentDescn", station.getDesc());
		map.put("ContentImg", station.getPic());
		map.put("ContentCatalogs", null);
		map.put("ContentKeyWord", null);
		map.put("ContentSubjectWord", null);
		map.put("PlayCount", "1234");
		return map;
	}
	
	public static int findInt(String str) {
		char[] s = str.toCharArray();
		String d = "";
		for (int i = 0; i < s.length; i++) {
			if (Character.isDigit(s[i])) {
				d += s[i];
			}
		}
		return Integer.valueOf(d);
	}
}
