package com.woting.appengine.solr.persis.po;

import com.spiritdata.framework.core.model.BaseObject;

public class SolrInputPo extends BaseObject {
	private static final long serialVersionUID = 3120554548607403867L;
	
	private String id;
	private String item_id;
	private String item_title;
	private String item_descn;
	private String item_imghash;
	private String item_publisher;
	private String item_type;
	private String item_pid;
	private int item_cloumnum;
	private long item_mediasize;
	private long item_playcount;
	private long item_timelong;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getItem_id() {
		return item_id;
	}
	public void setItem_id(String item_id) {
		this.item_id = item_id;
	}
	public String getItem_title() {
		return item_title;
	}
	public void setItem_title(String item_title) {
		this.item_title = item_title;
	}
	public String getItem_descn() {
		return item_descn;
	}
	public void setItem_descn(String item_descn) {
		this.item_descn = item_descn;
	}
	public String getItem_imghash() {
		return item_imghash;
	}
	public void setItem_imghash(String item_imghash) {
		this.item_imghash = item_imghash;
	}
	public String getItem_publisher() {
		return item_publisher;
	}
	public void setItem_publisher(String item_publisher) {
		this.item_publisher = item_publisher;
	}
	public String getItem_type() {
		return item_type;
	}
	public void setItem_type(String item_type) {
		this.item_type = item_type;
	}
	public String getItem_pid() {
		return item_pid;
	}
	public void setItem_pid(String item_pid) {
		this.item_pid = item_pid;
	}
	public int getItem_cloumnum() {
		return item_cloumnum;
	}
	public void setItem_cloumnum(int item_cloumnum) {
		this.item_cloumnum = item_cloumnum;
	}
	public long getItem_mediasize() {
		return item_mediasize;
	}
	public void setItem_mediasize(long item_mediasize) {
		this.item_mediasize = item_mediasize;
	}
	public long getItem_playcount() {
		return item_playcount;
	}
	public void setItem_playcount(long item_playcount) {
		this.item_playcount = item_playcount;
	}
	public long getItem_timelong() {
		return item_timelong;
	}
	public void setItem_timelong(long item_timelong) {
		this.item_timelong = item_timelong;
	}
}
