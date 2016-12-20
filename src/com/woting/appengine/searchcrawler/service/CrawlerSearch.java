package com.woting.appengine.searchcrawler.service;

import javax.servlet.ServletContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.cm.core.searchword.persis.po.SearchWordPo;
import com.woting.cm.core.searchword.service.SearchWordService;
import com.woting.passport.mobile.MobileUDKey;

public class CrawlerSearch extends Thread {

	private String word;
	private MobileUDKey mobileUDKey;
	
	public CrawlerSearch(String word, MobileUDKey mobileUDKey) {
		this.word = word;
		this.mobileUDKey = mobileUDKey;
	}
	
	public void insertWord() {
		SearchWordPo searchWordPo = new SearchWordPo();
		searchWordPo.setId(SequenceUUID.getPureUUID());
		searchWordPo.setWord(word);
		if (mobileUDKey!=null) {
			searchWordPo.setDeviceId(mobileUDKey.getDeviceId());
		    searchWordPo.setUserId(mobileUDKey.getUserId());
		    searchWordPo.setPcdType(mobileUDKey.getPCDType()+"");
		}
		ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
		if (WebApplicationContextUtils.getWebApplicationContext(sc) != null) {
			SearchWordService searchWordService=(SearchWordService) WebApplicationContextUtils.getWebApplicationContext(sc).getBean("searchWordService");
			searchWordService.insertSearchWord(searchWordPo);
		}
	}
	
	@Override
	public void run() {
		insertWord();
	}
}
