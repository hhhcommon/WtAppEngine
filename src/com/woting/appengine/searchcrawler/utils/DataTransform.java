package com.woting.appengine.searchcrawler.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.woting.appengine.searchcrawler.model.AudioData;
import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.SequData;
import com.woting.appengine.searchcrawler.model.Station;


/**
 * 数据转换
 * @author wbq
 *
 */
public class DataTransform {

	public Map<String, Object> dataTransform(String RPTypestr,Map<String, Object> map){
		switch(RPTypestr){
		case "0::0":break;
		case "0::1":break;
		case "2::0":break;
		case "2::1":break;
		default:break;
		}
		return map;
	}
	
	/**
	 * 转换搜索到的节目数据
	 * @param list_Festivals 各平台搜索到的包含节目的list列表
	 * @return	转换为提交节目信息的数据格式
	 */
	public List<AudioData> data2Audio(List<Festival>... list_Festivals){
		List<AudioData> list_AudioData = new ArrayList<AudioData>();
		for (List<Festival> list_Festival : list_Festivals) {
			if(list_Festival.size()>0){
				for (Festival festival : list_Festival) {
					AudioData audioData = new AudioData();
					audioData.setContentId(festival.getAudioId());
					audioData.setContentName(festival.getAudioName());
					audioData.setContentURI("content/getContentInfo.do?MediaType=AUDIO&ContentId="+festival.getAudioId());
					audioData.setContentImg(festival.getAudioPic());
					audioData.setContentPlay(festival.getMp3PlayUrl());
					audioData.setContentPersons(festival.getHost());
					audioData.setContentTimes(festival.getDuration());//以ms为计量单位
					audioData.setContentPubTime(festival.getCreateTime());
					audioData.setContentPub(festival.getContentPub());
					audioData.setContentDesc(festival.getAudioDes());
					audioData.setCTime(festival.getCreateTime());
					audioData.setMediaType(festival.getMediaType());
					//ContentCatalogs内容分类、ContentKeyWord关键词、ContentSubjectWord主题词和PlayCount播放次数未定义参数
					list_AudioData.add(audioData);
				}
			}
		}
		return list_AudioData;	
	}
	
	public List<SequData> data2Sequ(List<Station>... list_Stations){
		List<SequData> list_SequData = new ArrayList<SequData>();
		for (List<Station> list_Station : list_Stations) {
			if(list_Station.size()>0){
				for (Station station : list_Station) {
					SequData sequData = new SequData();
					sequData.setContentId(station.getId());
					sequData.setContentName(station.getName());
					sequData.setContentURI("content/getContentInfo.do?MediaType=SEQU&ContentId="+station.getId());
					sequData.setContentImg(station.getPic());
					sequData.setContentSubCount((station.getFestival().length)+"");
					sequData.setContentPub(station.getContentPub());
					sequData.setContentDesc(station.getDesc());
				}
			}
		}
		
		
		return null;
		
	}
}
