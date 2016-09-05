package com.woting.cm.core.media.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class MediaAssetPo extends BaseObject {
    private static final long serialVersionUID=-8041605008751208529L;

    private String id; //uuid(主键)
    private String maTitle; //媒体资源名称
    private int maPubType; //发布者记录类型：1-组织表,2-文本
    private String maPubId; //发布者，比如逻辑思维团队
    private String maPublisher; //发布者名称
    private Timestamp maPublishTime; //发布时间
    private String maImg; //媒体图Url
    private String maURL; //媒体主地址，可以是聚合的源，也可以是Wt平台中的文件URL
    private String subjectWords; //主题词
    private String keyWords; //关键词
    private String langDid; //语言字典项Id
    private String language; //语言名称
    private long timeLong; //时长，毫秒数
    private String descn; //说明
    private int pubCount; //发布状态：0未发布;>0被发布到多少个栏目中（系列节目的发布，这里的单曲也要被加1）
    private int maStatus; //资源状态：0草稿;1提交（包括发布和未发布）
    public int getMaStatus() {
		return maStatus;
	}
	public void setMaStatus(int maStatus) {
		this.maStatus = maStatus;
	}
	private Timestamp CTime; //记录创建时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id=id;
    }
    public String getMaTitle() {
        return maTitle;
    }
    public void setMaTitle(String maTitle) {
        this.maTitle=maTitle;
    }
    public int getMaPubType() {
        return maPubType;
    }
    public void setMaPubType(int maPubType) {
        this.maPubType=maPubType;
    }
    public String getMaPubId() {
        return maPubId;
    }
    public void setMaPubId(String maPubId) {
        this.maPubId=maPubId;
    }
    public String getMaPublisher() {
        return maPublisher;
    }
    public void setMaPublisher(String maPublisher) {
        this.maPublisher=maPublisher;
    }
    public Timestamp getMaPublishTime() {
        return maPublishTime;
    }
    public void setMaPublishTime(Timestamp maPublishTime) {
        this.maPublishTime=maPublishTime;
    }
    public String getMaImg() {
        return maImg;
    }
    public void setMaImg(String maImg) {
        this.maImg=maImg;
    }
    public String getMaURL() {
        return maURL;
    }
    public void setMaURL(String maURL) {
        this.maURL=maURL;
    }
    public String getSubjectWords() {
        return subjectWords;
    }
    public void setSubjectWords(String subjectWords) {
        this.subjectWords=subjectWords;
    }
    public String getKeyWords() {
        return keyWords;
    }
    public void setKeyWords(String keyWords) {
        this.keyWords=keyWords;
    }
    public String getLangDid() {
        return langDid;
    }
    public void setLangDid(String langDid) {
        this.langDid=langDid;
    }
    public String getLanguage() {
        return language;
    }
    public void setLanguage(String language) {
        this.language=language;
    }
    public long getTimeLong() {
        return timeLong;
    }
    public void setTimeLong(long timeLong) {
        this.timeLong=timeLong;
    }
    public String getDescn() {
        return descn;
    }
    public void setDescn(String descn) {
        this.descn=descn;
    }
    public int getPubCount() {
        return pubCount;
    }
    public void setPubCount(int pubCount) {
        this.pubCount=pubCount;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime=cTime;
    }
}