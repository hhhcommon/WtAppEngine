package com.woting.appengine.searchcrawler.model;

import java.util.Arrays;

/**
 * 
 * 考拉FM搜索专辑信息
 * @author wbq
 *
 */
public class Station {

	private String mediaType = "SEQU";//类型
	private String contentPub;	//发布者
	private String id;				//专辑ID
	private String name;			//专辑名称
	private String desc;			//专辑内容描述
	private String pic;				//专辑图片链接
	private String host;			//主播人
	private String createTime;
	private Festival[] festival;	//专辑节目信息
	
	public String getCreateTime() {
		return createTime;
	}


	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	@Override
	public String toString() {
		return "Station [mediaType=" + mediaType + ", contentPub=" + contentPub + ", id=" + id + ", name=" + name
				+ ", desc=" + desc + ", pic=" + pic + ", host=" + host + ", createTime=" + createTime + ", festival="
				+ Arrays.toString(festival) + "]";
	}

	public String getContentPub() {
		return contentPub;
	}


	public void setContentPub(String contentPub) {
		this.contentPub = contentPub;
	}


	public String getId() {
		return id;
	}
	

	public String getHost() {
		return host;
	}


	public void setHost(String host) {
		this.host = host;
	}

	public String getPic() {
		return pic;
	}


	public void setPic(String pic) {
		this.pic = pic;
	}


	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name.replaceAll("<em>|</em>", "");
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public Festival[] getFestival() {
		return festival;
	}
	public void setFestival(Festival[] festival) {
		this.festival = festival;
	}
}
