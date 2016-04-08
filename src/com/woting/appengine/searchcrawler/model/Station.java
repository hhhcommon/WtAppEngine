package com.woting.appengine.searchcrawler.model;

import java.util.Arrays;

/**
 * 
 * 考拉FM搜索专辑信息
 * @author wbq
 *
 */
public class Station {

	private String id;				//专辑ID
	private String name;			//专辑名称
	private String desc;			//专辑内容描述
	private String pic;				//专辑图片链接
	private Festival[] festival;	//专辑节目信息
	
	
	public String getId() {
		return id;
	}
	

	@Override
	public String toString() {
		return "Station  [id=" + id + ", name=" + name + ", desc=" + desc + ", pic=" + pic + ", festival="
				+ Arrays.toString(festival) + "]";
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
		this.name = name;
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
