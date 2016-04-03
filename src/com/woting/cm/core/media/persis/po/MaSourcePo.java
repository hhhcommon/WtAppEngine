package com.woting.cm.core.media.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class MaSourcePo extends BaseObject {
    private static final long serialVersionUID = 2082780036836138143L;

    private String id; //uuid(主键)
    private String maId; //单体节目Id
    private int maSrcType; //来源，类型：1-组织表；2-文本
    private String maSrcId; //当maSrcType=1,来源Id，对应表wt_Organize内容；当maSrcType=2,来源名称
    private String maSource; //来源描述或名称
    private int smType; //来源媒体分类:1-文件;2-直播流
    private String playURI; //直播流URL
    private int isMain; //是否主播放地址；1是主播放
    private String descn; //说明
    private Timestamp CTime; //记录创建时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getMaId() {
        return maId;
    }
    public void setMaId(String maId) {
        this.maId = maId;
    }
    public int getMaSrcType() {
        return maSrcType;
    }
    public void setMaSrcType(int maSrcType) {
        this.maSrcType = maSrcType;
    }
    public String getMaSrcId() {
        return maSrcId;
    }
    public void setMaSrcId(String maSrcId) {
        this.maSrcId = maSrcId;
    }
    public String getMaSource() {
        return maSource;
    }
    public void setMaSource(String maSource) {
        this.maSource = maSource;
    }
    public int getSmType() {
        return smType;
    }
    public void setSmType(int smType) {
        this.smType = smType;
    }
    public String getPlayURI() {
        return playURI;
    }
    public void setPlayURI(String playURI) {
        this.playURI = playURI;
    }
    public int getIsMain() {
        return isMain;
    }
    public void setIsMain(int isMain) {
        this.isMain = isMain;
    }
    public String getDescn() {
        return descn;
    }
    public void setDescn(String descn) {
        this.descn = descn;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime = cTime;
    }
}