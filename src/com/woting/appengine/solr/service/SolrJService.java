package com.woting.appengine.solr.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.FieldAnalysisRequest;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.AnalysisPhase;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.TokenInfo;
import org.apache.solr.client.solrj.response.FieldAnalysisResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.woting.appengine.solr.persis.po.SolrInputPo;
import com.woting.appengine.solr.persis.po.SolrSearchResult;
import com.woting.appengine.solr.utils.SolrUtils;

@Service
public class SolrJService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	@Resource
	private HttpSolrServer httpSolrServer;
	
	public void addSolrIndex(Object media , String pid, long playcount) {
		if (media!=null) {
			SolrInputPo sPo = SolrUtils.convert2SolrInput(media, pid, playcount);
			SolrInputDocument document = SolrUtils.convert2SolrDocument(sPo);
			try {
				httpSolrServer.add(document);
				httpSolrServer.commit();
			} catch (SolrServerException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public List<SolrInputPo> getAudioListByAlbumId(String id) {
		//创建一个查询对象
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("item_pid:"+id);
		solrQuery.setSort("item_columnnum", SolrQuery.ORDER.desc);
		solrQuery.addFilterQuery("item_type:AUDIO");
		//根据查询条件搜索索引库
	    try {
			QueryResponse response = httpSolrServer.query(solrQuery);
			//获取内容列表
			SolrDocumentList documentList = response.getResults();
			//内容列表
			List<SolrInputPo> solrs = new ArrayList<>();
			if (documentList!=null && documentList.size()>0) {
				for (SolrDocument solrDocument : documentList) {
					SolrInputPo sPo = new SolrInputPo();
					sPo.setId(solrDocument.get("id").toString());
					sPo.setItem_id((String) solrDocument.get("item_id"));
					sPo.setItem_title((String) solrDocument.get("item_title"));
					sPo.setItem_type((String) solrDocument.get("item_type"));
					sPo.setItem_publisher((String) solrDocument.get("item_publisher"));
					if (solrDocument.containsKey("item_meidasize")) {
						sPo.setItem_mediasize((int) solrDocument.get("item_meidasize"));
					}
					if (solrDocument.containsKey("item_timelong")) {
						sPo.setItem_timelong((long) solrDocument.get("item_timelong"));
					}
					solrs.add(sPo);
				}
				return solrs;
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public SolrSearchResult solrSearch(String querystr, List<SortClause> sorts, String flstr, int page, int pageSize, String... fqstr) throws Exception {
		//创建一个查询对象
		SolrQuery solrQuery = new SolrQuery();
		//查询条件
		if (querystr==null || querystr.isEmpty()) {
			solrQuery.setQuery("*:*");
		} else {
			querystr = SolrUtils.makeQueryStr(querystr, true);
			solrQuery.setQuery("item_title:"+querystr+"^5 item_persons:"+querystr+"^3 item_channel:"+querystr+"^1 item_descn:"+querystr+"^2");
		}
		solrQuery.setStart((page -1) * pageSize);
		solrQuery.setRows(pageSize);
		//设置默认搜索域
//		solrQuery.set("df", "item_keywords");
//		solrQuery.set("defType", "edismax");
//		solrQuery.set("mm","80%");
		if (sorts!=null && sorts.size()>0) {
			solrQuery.setSorts(sorts);
		}
		if (flstr!=null && flstr.length()>0) {
			solrQuery.set("fl", flstr);
		}
		if (fqstr!=null && fqstr.length>0) {
			solrQuery.addFilterQuery(fqstr);
		}
		//根据查询条件搜索索引库
		QueryResponse response = httpSolrServer.query(solrQuery);
		//获取内容列表
		SolrDocumentList documentList = response.getResults();
		//内容列表
		List<SolrInputPo> solrs = new ArrayList<>();
		for (SolrDocument solrDocument : documentList) {
			SolrInputPo sPo = new SolrInputPo();
			sPo.setId(solrDocument.get("id").toString());
			sPo.setItem_id((String) solrDocument.get("item_id"));
			sPo.setItem_title((String) solrDocument.get("item_title"));
			sPo.setItem_type((String) solrDocument.get("item_type"));
			sPo.setItem_publisher((String) solrDocument.get("item_publisher"));
			if (solrDocument.containsKey("item_meidasize")) {
				sPo.setItem_mediasize((long) solrDocument.get("item_meidasize"));
			}
			if (solrDocument.containsKey("item_timelong")) {
				sPo.setItem_timelong((long) solrDocument.get("item_timelong"));
			}
			solrs.add(sPo);
		}
		SolrSearchResult result = new SolrSearchResult();
		//列表
		result.setSolrInputPos(solrs);
		//总记录数据
		result.setRecordCount(documentList.getNumFound());
		
		//计算分页
		Long recordCount = result.getRecordCount();
		int pageCount = (int) (recordCount / pageSize);
		if (recordCount % pageSize > 0) {
			pageCount++;
		}
		result.setPageCount(pageCount);
		result.setCurPage(page);
		return result;
	}
	
	public List<String> getAnalysis(String sentence) {
		if (sentence!=null) {
			sentence = SolrUtils.makeQueryStr(sentence, false);
			FieldAnalysisRequest request = new FieldAnalysisRequest("/analysis/field");
			request.addFieldName("text_ik");// 字段名，随便指定一个支持中文分词的字段
	        request.setFieldValue("");// 字段值，可以为空字符串，但是需要显式指定此参数
	        request.setQuery(sentence);
	        FieldAnalysisResponse response = null;
	        try {
	            response = request.process(httpSolrServer);
	        } catch (Exception e) {
	            logger.error("获取查询语句的分词时遇到错误", e);
	        }
	        List<String> results = new ArrayList<String>();
	        Iterator<AnalysisPhase> it = response.getFieldNameAnalysis("text_ik").getQueryPhases().iterator();
	        while(it.hasNext()) {
	          AnalysisPhase pharse = (AnalysisPhase)it.next();
	          List<TokenInfo> list = pharse.getTokens();
	          for (TokenInfo info : list) {
	              results.add(info.getText());
	          }
	        }
	        return results;
		}
		return null;
	}
	
	public void deleteAll() {
		try {
			httpSolrServer.deleteByQuery("*:*");
			httpSolrServer.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
