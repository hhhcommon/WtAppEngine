package com.woting.appengine.solr.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.common.SolrInputDocument;


import com.woting.appengine.solr.persis.po.SolrInputPo;
import com.woting.cm.core.media.persis.po.MediaAssetPo;
import com.woting.cm.core.media.persis.po.SeqMediaAssetPo;

/**
 * Solr工具
 * @author wbq
 *
 */
public abstract class SolrUtils {

	public static SolrInputPo convert2SolrInput(Object obj, String pid, long playcount) {
		if (obj instanceof SeqMediaAssetPo) {
			SeqMediaAssetPo sma = (SeqMediaAssetPo) obj;
			SolrInputPo sPo = new SolrInputPo();
			sPo.setId("SEQU_"+sma.getId());
			sPo.setItem_id(sma.getId());
			sPo.setItem_title(sma.getSmaTitle());
			sPo.setItem_type("SEQU");
			sPo.setItem_mediasize(sma.getSmaAllCount());
			sPo.setItem_publisher(sma.getSmaPublisher());
			sPo.setItem_descn(sma.getDescn());
			sPo.setItem_playcount(playcount);
			return sPo;
		} else {
			if (obj instanceof MediaAssetPo) {
				MediaAssetPo ma = (MediaAssetPo) obj;
				SolrInputPo sPo = new SolrInputPo();
				sPo.setId("AUDIO_"+ma.getId());
				sPo.setItem_id(ma.getId());
				if (pid!=null) {
					sPo.setItem_pid(pid);
				}
				sPo.setItem_title(ma.getMaTitle());
				sPo.setItem_publisher(ma.getMaPublisher());
				sPo.setItem_descn(ma.getDescn());
				sPo.setItem_timelong(ma.getTimeLong());
				sPo.setItem_playcount(playcount);
				sPo.setItem_type("AUDIO");
				return sPo;
			}
		}
		return null;
	}
	
	
	public static SolrInputDocument convert2SolrDocument(SolrInputPo solrInputPo) {
		if (solrInputPo!=null) {
			SolrInputDocument document = new SolrInputDocument();
			if (solrInputPo.getId()!=null) document.addField("id", solrInputPo.getId());
			if (solrInputPo.getItem_id()!=null) document.addField("item_id", solrInputPo.getItem_id());
			if (solrInputPo.getItem_title()!=null) document.addField("item_title", solrInputPo.getItem_title()); 
			if (solrInputPo.getItem_imghash()!=null) document.addField("item_imghash", solrInputPo.getItem_imghash());
			if (solrInputPo.getItem_publisher()!=null) document.addField("item_publisher", solrInputPo.getItem_publisher());
			if (solrInputPo.getItem_timelong()!=0) document.addField("item_timelong", solrInputPo.getItem_timelong());
			if (solrInputPo.getItem_mediasize()!=0) document.addField("item_meidasize", solrInputPo.getItem_mediasize());
			if (solrInputPo.getItem_type()!=null) document.addField("item_type", solrInputPo.getItem_type());
			if (solrInputPo.getItem_pid()!=null) document.addField("item_pid", solrInputPo.getItem_pid());
		    document.addField("item_playcount", solrInputPo.getItem_playcount());
			if (solrInputPo.getItem_descn()!=null) document.addField("item_descn", solrInputPo.getItem_descn());
			return document;
		}
		return null;
	}
	
	/**
	 * 去除solr查询过敏字段
	 * @param str
	 * @param searorans true:查询        false:分词
	 * @return
	 */
	public static String makeQueryStr(String str, boolean searorans) {
		String[] allergicField   = {"[","]",":","（","）","\"","“","”","/","{","}","-"," ","《","》"};
		String[] reallergicField = {"" ,"" ,"" ,"(",")" ,""  ,"" ,"" ,"" ,"" ,"" ,"" ,"" ,"" ,"" };
		String[] analysis = {"第","集"};
		String[] reanalysis = {"",""};
		if (searorans) { // 整理待查询字段
			if (str!=null && str.length()>0) {
				for (int i = 0; i < allergicField.length; i++) {
					str = str.replace(allergicField[i], reallergicField[i]);
				}
				if (str.contains("(") && !str.contains(")")) str = str.replace("(", "");
				if (str.contains(")") && !str.contains("(")) str = str.replace(")", ""); 
				return str;
			}
		} else { // 整理待搜索字段
			if (str!=null && str.length()>0) {
				for (int i = 0; i < allergicField.length; i++) {
					str = str.replace(allergicField[i], reallergicField[i]);
				}
				if (str.contains("(") && !str.contains(")")) str = str.replace("(", "");
				if (str.contains(")") && !str.contains("(")) str = str.replace(")", ""); 
				for (int i = 0; i < analysis.length; i++) {
					str = str.replace(analysis[i], reanalysis[i]);
				}
				return str;
			}
		}
		
		return null;
	}
	
	public static List<SortClause> makeSolrSort(String... flstr) {
		if (flstr!=null && flstr.length>0) {
			List<SortClause> sorts = new ArrayList<>();
			for (String sort : flstr) {
				String[] sortts = sort.split(" ");
				sorts.add(new SortClause(sortts[0], sortts[1]));
			}
			return sorts;
		}
		return null;
	}


	public static SolrInputPo convert2SolrInput(Object obj, String pid, String persons, String chstr, long playcount) {
		if (obj instanceof SeqMediaAssetPo) {
			SeqMediaAssetPo sma = (SeqMediaAssetPo) obj;
			SolrInputPo sPo = new SolrInputPo();
			sPo.setId("SEQU_"+sma.getId());
			sPo.setItem_id(sma.getId());
			sPo.setItem_title(sma.getSmaTitle());
			sPo.setItem_type("SEQU");
			sPo.setItem_mediasize(sma.getSmaAllCount());
			sPo.setItem_publisher(sma.getSmaPublisher());
			if (chstr!=null) {
				sPo.setItem_channel(chstr);
			}
			sPo.setItem_descn(sma.getDescn());
			if (persons!=null) {
				sPo.setItem_persons(persons);
			}
			sPo.setItem_playcount(playcount);
			return sPo;
		} else {
			if (obj instanceof MediaAssetPo) {
				MediaAssetPo ma = (MediaAssetPo) obj;
				SolrInputPo sPo = new SolrInputPo();
				sPo.setId("AUDIO_"+ma.getId());
				sPo.setItem_id(ma.getId());
				if (pid!=null) {
					sPo.setItem_pid(pid);
				}
				if (persons!=null) {
					sPo.setItem_persons(persons);
				}
				sPo.setItem_title(ma.getMaTitle());
				sPo.setItem_publisher(ma.getMaPublisher());
				sPo.setItem_descn(ma.getDescn());
				sPo.setItem_timelong(ma.getTimeLong());
				if (chstr!=null) {
					sPo.setItem_channel(chstr);
				}
				sPo.setItem_playcount(playcount);
				sPo.setItem_type("AUDIO");
				return sPo;
			}
		}
		return null;
	}
}
