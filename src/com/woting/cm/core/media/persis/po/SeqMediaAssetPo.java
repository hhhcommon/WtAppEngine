package com.woting.cm.core.media.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class SeqMediaAssetPo extends BaseObject {
    private static final long serialVersionUID = -5566892762581978391L;

    private String id; //uuid(主键)
    private String smaTitle; //专辑名称
    private int smaPubType; //发布者类型：1-组织表,2-文本
    private String smaPubId; //发布所属组织Id,或名称
    private String smaPublisher; //发布者名称,或描述
    private Timestamp smaPublishTime; //发布时间
    private String smaImg; //媒体图
    private int smaAllCount; //总卷集号，可以为空，这个和总数不同，也可能一样
    private String subjectWords; //主题词
    private String keyWords; //关键词
    private String langDid; //语言字典项Id
    private String language; //语言名称
    private String descn; //说明
    private int pubCount; //发布状态：0未发布;>0被发布到多少个栏目中（系列节目的发布，这里的单曲也要被加1）
    private int smaStatus; //资源状态：0草稿;1提交（包括已发布和未发布）
    private Timestamp CTime; //记录创建时间

    public String getId() {
        return id;
    }
    public int getSmaStatus() {
		return smaStatus;
	}
	public void setSmaStatus(int smaStatus) {
		this.smaStatus = smaStatus;
	}
	public void setId(String id) {
        this.id = id;
    }
    public String getSmaTitle() {
        return smaTitle;
    }
    public void setSmaTitle(String smaTitle) {
        this.smaTitle = smaTitle;
    }
    public int getSmaPubType() {
        return smaPubType;
    }
    public void setSmaPubType(int smaPubType) {
        this.smaPubType = smaPubType;
    }
    public String getSmaPubId() {
        return smaPubId;
    }
    public void setSmaPubId(String smaPubId) {
        this.smaPubId = smaPubId;
    }
    public String getSmaPublisher() {
        return smaPublisher;
    }
    public void setSmaPublisher(String smaPublisher) {
        this.smaPublisher = smaPublisher;
    }
    public Timestamp getSmaPublishTime() {
        return smaPublishTime;
    }
    public void setSmaPublishTime(Timestamp smaPublishTime) {
        this.smaPublishTime = smaPublishTime;
    }
    public String getSmaImg() {
        return smaImg;
    }
    public void setSmaImg(String smaImg) {
        this.smaImg = smaImg;
    }
    public int getSmaAllCount() {
        return smaAllCount;
    }
    public void setSmaAllCount(int smaAllCount) {
        this.smaAllCount = smaAllCount;
    }
    public String getSubjectWords() {
        return subjectWords;
    }
    public void setSubjectWords(String subjectWords) {
        this.subjectWords = subjectWords;
    }
    public String getKeyWords() {
        return keyWords;
    }
    public void setKeyWords(String keyWords) {
        this.keyWords = keyWords;
    }
    public String getLangDid() {
        return langDid;
    }
    public void setLangDid(String langDid) {
        this.langDid = langDid;
    }
    public String getLanguage() {
        return language;
    }
    public void setLanguage(String language) {
        this.language = language;
    }
    public String getDescn() {
        return descn;
    }
    public void setDescn(String descn) {
        this.descn = descn;
    }
    public int getPubCount() {
        return pubCount;
    }
    public void setPubCount(int pubCount) {
        this.pubCount = pubCount;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime = cTime;
    }
}