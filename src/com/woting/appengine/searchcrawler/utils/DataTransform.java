package com.woting.appengine.searchcrawler.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;

/**
 * 数据转换
 * 
 * @author wbq
 *
 */
public class DataTransform {

	/**
	 * 多festival转audio
	 * 
	 * @param list_Festival
	 *            搜索到的节目信息
	 * @param list_Station
	 *            搜索到的频道信息
	 * @param PageType
	 *            如果PageType=0,把专辑的第一个节目提取出来，如果等于1，频道信息在此方法不处理
	 * @return 返回处理好的audio信息
	 */
	public List<Map<String, Object>> datas2Audio(List<Festival> list_Festival, List<Station> list_Station,
			int PageType) {
		if (list_Festival == null || list_Station == null) {
			return null;
		}
		List<Map<String, Object>> list_AudioData = new ArrayList<Map<String, Object>>();
		if (list_Festival.size() > 0) {
			for (Festival festival : list_Festival) {
				// ContentCatalogs内容分类、ContentKeyWord关键词、ContentSubjectWord主题词和PlayCount播放次数未定义参数
				Map<String, Object> map = new HashMap<String, Object>();
				map = festival2Audio(festival);
				list_AudioData.add(map);
			}
		}
		if (PageType == 0) {
			if (list_Station.size() > 0) {
				for (int i = 0; i < list_Station.size(); i++) {
					Map<String, Object> map = datas2Sequ_Audio(list_Station.get(i));
					list_AudioData.add(map);
				}
			}
		}
		return list_AudioData;
	}

	/**
	 * 多专辑数据转换
	 * 
	 * @param list_Stations
	 * @return
	 */
	public List<Map<String, Object>> datas2Sequ(List<Station>... list_Stations) {
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
	public Map<String, Object> datas2Sequ_Audio(Station station) {
		if (station.getFestival() == null) {
			return null;
		}
		Festival festival = station.getFestival()[0];
		Map<String, Object> map = festival2Audio(festival);
		map.put("SeqInfo", station2Sequ(station));
		return map;
	}

	/**
	 * 单festival转audio
	 * 
	 * @param festival
	 * @return
	 */
	public Map<String, Object> festival2Audio(Festival festival) {
		// ContentCatalogs内容分类、ContentKeyWord关键词、ContentSubjectWord主题词和PlayCount播放次数未定义参数
		if (festival == null) {
			return null;
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("ContentId", festival.getAudioId());
		map.put("ContentName", festival.getAudioName());
		map.put("ContentURI", "content/getContentInfo.do?MediaType=AUDIO&ContentId=" + festival.getAudioId());
		map.put("ContentImg", festival.getAudioPic());
		map.put("ContentPlay", festival.getPlayUrl());
		map.put("ContentImg", festival.getAudioPic());
		map.put("ContentPersons", festival.getHost());
		map.put("ContentTimes", festival.getDuration());// 以ms为计量单位
		map.put("ContentPubTime", festival.getUpdateTime());
		map.put("ContentPub", festival.getContentPub());
		map.put("ContentDesc", festival.getAudioDes());
		map.put("CTime", null);
		map.put("MediaType", festival.getMediaType());
		map.put("ContentCatalogs", null);
		map.put("ContentKeyWord", null);
		map.put("ContentSubjectWord", null);
		map.put("PlayCount", null);
		return map;
	}

	/**
	 * 单station转sequ
	 * 
	 * @param station
	 * @return
	 */
	public Map<String, Object> station2Sequ(Station station) {
		// ContentCatalogs内容分类、ContentKeyWord关键词、ContentSubjectWord主题词和PlayCount播放次数
		if (station == null) {
			return null;
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("ContentSubCount", (station.getFestival().length) + "");
		map.put("ContentURI", "content/getContentInfo.do?MediaType=SEQU&ContentId=" + station.getId());
		map.put("ContentPersons", station.getHost());
		map.put("CTime", station.getCTime());
		map.put("ContentName", station.getName());
		map.put("ContentPub", station.getContentPub());
		map.put("MediaType", station.getMediaType());
		map.put("ContentId", station.getId());
		map.put("ContentDesc", station.getDesc());
		map.put("ContentImg", station.getPic());
		map.put("ContentCatalogs", null);
		map.put("ContentKeyWord", null);
		map.put("ContentSubjectWord", null);
		map.put("PlayCount", null);
		return map;
	}
}
