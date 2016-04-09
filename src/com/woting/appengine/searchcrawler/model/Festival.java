package com.woting.appengine.searchcrawler.model;


/**
 * 考拉FM搜索节目信息
 * @author wbq
 *
 */
public class Festival {

	private String mediaType = "AUDIO";	//类型
	private String contentPub;	//发布者
	private String audioId;		//节目id
	private String audioName;  	//节目名称
	private String audioPic;	//内容图片
	private String audioDes;	//节目描述
	private String albumName;	//专辑名称
	private String albumPic;	//专辑图片
	private String mp3PlayUrl;	//节目mp3格式音频链接地址
	private String fileSize;	//音频文件大小
	private String duration;	//音频时长
	private String updateTime;	//发布时间
	private String listenNum;	//听众人数
	private String createTime;	//创建时间
	private String host;		//主播人
	
	@Override
	public String toString() {
		return "Festival [mediaType=" + mediaType + ", contentPub=" + contentPub + ", audioId=" + audioId
				+ ", audioName=" + audioName + ", audioPic=" + audioPic + ", audioDes=" + audioDes + ", albumName="
				+ albumName + ", albumPic=" + albumPic + ", mp3PlayUrl=" + mp3PlayUrl + ", fileSize=" + fileSize
				+ ", duration=" + duration + ", updateTime=" + updateTime + ", listenNum=" + listenNum + ", createTime="
				+ createTime + ", host=" + host + "]";
	}
	
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getCreateTime() {
		return createTime;
	}
	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}
	
	public String getAudioDes() {
		return audioDes;
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
		this.audioName = audioName.replaceAll("<em>|</em>", "");
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
		this.albumName = albumName.replaceAll("<em>|</em>", "");
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
