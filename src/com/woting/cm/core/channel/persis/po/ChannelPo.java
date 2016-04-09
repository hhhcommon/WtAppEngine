package com.woting.cm.core.channel.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class ChannelPo extends BaseObject {
    private static final long serialVersionUID=2518950208774890558L;

    private String id;  //表ID(UUID)
    private String pcId; //父结点ID(UUID)，若是根为0
    private int ownerType;  //所有者类型(0-系统,1-主播)，目前为0
    private String ownerId;  //所有者Id，目前完全是系统维护的栏目，为1
    private String channelName;  //栏目名称
    private String NPy;  //名称拼音
    private int isValidate;  //是否生效(1-生效,2-无效)
    private int sort;  //栏目排序,从大到小排序，越大越靠前，根下同级别
    private String contentType;  //允许资源的类型，可以是多个，0所有；1电台；2单体媒体资源；3专辑资源；用逗号隔开，比如“1,2”，目前都是0
    private String channelImg;  //
    private String descn; //说明
    private Timestamp CTime; //记录创建时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id=id;
    }
    public String getPcId() {
        return pcId;
    }
    public void setPcId(String pcId) {
        this.pcId=pcId;
    }
    public int getOwnerType() {
        return ownerType;
    }
    public void setOwnerType(int ownerType) {
        this.ownerType=ownerType;
    }
    public String getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(String ownerId) {
        this.ownerId=ownerId;
    }
    public String getChannelName() {
        return channelName;
    }
    public void setChannelName(String channelName) {
        this.channelName=channelName;
    }
    public String getNPy() {
        return NPy;
    }
    public void setNPy(String nPy) {
        NPy=nPy;
    }
    public int getIsValidate() {
        return isValidate;
    }
    public void setIsValidate(int isValidate) {
        this.isValidate=isValidate;
    }
    public int getSort() {
        return sort;
    }
    public void setSort(int sort) {
        this.sort=sort;
    }
    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType=contentType;
    }
    public String getChannelImg() {
        return channelImg;
    }
    public void setChannelImg(String channelImg) {
        this.channelImg=channelImg;
    }
    public String getDescn() {
        return descn;
    }
    public void setDescn(String descn) {
        this.descn=descn;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime=cTime;
    }
}