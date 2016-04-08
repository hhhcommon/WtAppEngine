package com.woting.appengine.search.model;


/**
 * 考拉FM搜索节目信息
 * @author wbq
 *
 */
public class Festival {

	private String audioName;  	//节目名称
	private String audioPic;	//节目图片链接
	private String albumName;	//专辑名称
	private String albumPic;	//专辑图片
	private String mp3PlayUrl;	//节目mp3格式音频链接地址
	private String fileSize;	//音频文件大小
	private String updateTime;	//更新时间
	private String listenNum;	//听众人数

	
	@Override
	public String toString() {
		return "Festival [audioName=" + audioName + ", audioPic=" + audioPic + ", albumName=" + albumName
				+ ", albumPic=" + albumPic + ", mp3PlayUrl=" + mp3PlayUrl + ", fileSize=" + fileSize + ", updateTime="
				+ updateTime + ", listenNum=" + listenNum + "]";
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
		this.audioName = audioName;
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
		this.albumName = albumName;
	}

	public String getMp3PlayUrl() {
		return mp3PlayUrl;
	}
	public void setMp3PlayUrl(String mp3PlayUrl) {
		this.mp3PlayUrl = mp3PlayUrl;
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
