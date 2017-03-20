package com.woting.appengine.solr.persis.po;

import java.util.List;

import com.spiritdata.framework.core.model.BaseObject;

public class SolrSearchResult extends BaseObject {

	private static final long serialVersionUID = -6676794423440913433L;
	private List<SolrInputPo> solrInputPos;
	private long recordCount;
	private int pageCount;
	private int curPage;
	
	public List<SolrInputPo> getSolrInputPos() {
		return solrInputPos;
	}
	public void setSolrInputPos(List<SolrInputPo> solrInputPos) {
		this.solrInputPos = solrInputPos;
	}
	public long getRecordCount() {
		return recordCount;
	}
	public void setRecordCount(long recordCount) {
		this.recordCount = recordCount;
	}
	public int getPageCount() {
		return pageCount;
	}
	public void setPageCount(int pageCount) {
		this.pageCount = pageCount;
	}
	public int getCurPage() {
		return curPage;
	}
	public void setCurPage(int curPage) {
		this.curPage = curPage;
	}
}
