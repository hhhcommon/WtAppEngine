package com.woting.appengine.searchcrawler.model;

import java.io.Serializable;

import com.spiritdata.framework.util.StringUtils;

/**
 * 考拉FM搜索节目信息
 * @author wbq
 *
 */
public class Festival implements Serializable {

	private String mediaType = "AUDIO";	//类型
	private String contentPub;	//发布者
	private String audioId;		//节目id
	private String audioName;  	//节目名称
	private String audioPic;	//内容图片
	private String audioDes;	//节目描述
	private String albumName;	//专辑名称
	private String albumPic;	//专辑图片
	private String PlayUrl;		//节目音频链接地址
	private String fileSize;	//音频文件大小
	private String duration;	//音频时长
	private String updateTime;	//发布时间
	private String listenNum;	//听众人数
	private String personName;  //上传者昵称
	private String personId;    //上传者id
	private String playnum;		//播放次数 
	private String category;	//分类
	


	@Override
	public String toString() {
		return "Festival [mediaType=" + mediaType + ", contentPub=" + contentPub + ", audioId=" + audioId
				+ ", audioName=" + audioName + ", audioPic=" + audioPic + ", audioDes=" + audioDes + ", albumName="
				+ albumName + ", albumPic=" + albumPic + ", PlayUrl=" + PlayUrl + ", fileSize=" + fileSize
				+ ", duration=" + duration + ", updateTime=" + updateTime + ", listenNum=" + listenNum + ", host="
				+  ", playnum=" + playnum + ", category=" + category + "]";
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getPlaynum() {
		return playnum;
	}

	public void setPlaynum(String playnum) {
		this.playnum = playnum;
	}
	public String getPersonName() {
		return personName;
	}

	public void setPersonName(String personName) {
		this.personName = personName;
	}

	public String getPersonId() {
		return personId;
	}

	public void setPersonId(String personId) {
		this.personId = personId;
	}

	public String getAudioDes() {
		return audioDes==null?null:audioDes.replaceAll("\n", "").replaceAll("\r", "");
	}
	public void setAudioDes(String audioDes) {
		this.audioDes = audioDes;
	}
	public String getAudioId() {
		return audioId;
	}
	public void setAudioId(String audioId) {
		this.audioId = audioId;
	}
	public String getContentPub() {
		return contentPub;
	}
	public void setContentPub(String contentPub) {
		this.contentPub = contentPub;
	}
	public String getMediaType() {
		return mediaType;
	}
	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}
	public String getDuration() {
		return duration;
	}
	public void setDuration(String duration) {
		this.duration = duration;
	}
	public String getAlbumPic() {
		return albumPic;
	}
	public void setAlbumPic(String albumPic) {
		this.albumPic = albumPic;
	}
	public String getAudioName() {
		return audioName;
	}
	public void setAudioName(String audioName) {
		if (!StringUtils.isNullOrEmptyOrSpace(audioName)) {
			this.audioName = audioName.replaceAll("<em>|</em>", "");
		}
	}
	public String getAudioPic() {
		return audioPic;
	}
	public void setAudioPic(String audioPic) {
		this.audioPic = audioPic;
	}
	public String getAlbumName() {
		return albumName;
	}
	public void setAlbumName(String albumName) {
		if (!StringUtils.isNullOrEmptyOrSpace(albumName)) {
			this.albumName = albumName.replaceAll("<em>|</em>", "");
		}
	}

	public String getPlayUrl() {
		return PlayUrl;
	}
	public void setPlayUrl(String PlayUrl) {
		this.PlayUrl = PlayUrl;
	}
	public String getFileSize() {
		return fileSize;
	}
	public void setFileSize(String fileSize) {
		this.fileSize = fileSize;
	}
	public String getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}
	public String getListenNum() {
		return listenNum;
	}
	public void setListenNum(String listenNum) {
		this.listenNum = listenNum;
	}
}
