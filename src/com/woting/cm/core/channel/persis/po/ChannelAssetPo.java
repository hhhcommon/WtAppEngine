package com.woting.cm.core.channel.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class ChannelAssetPo extends BaseObject {
    private static final long serialVersionUID=-2687200733571596339L;

    private String id; //表ID(UUID)
    private String channelId; //栏目Id
    private String assetType; //内容类型：1电台；2单体媒体资源；3专辑资源
    private String assetId; //内容Id
    private String publisherId; //发布者Id
    private String checkerId; //审核者Id，可以为空，若为1，则审核者为系统
    private int isValidate; //是否生效(1-生效,2-无效)
    private int sort; //栏目排序,从大到小排序，越大越靠前，既是置顶功能
    private int flowFlag; //流程状态：0入库；1在审核；2审核通过(既发布状态)；3审核未通过
    private String pubName; //发布名称，可为空，若为空，则取资源的名称
    private String pubImg; //发布图片，可为空，若为空，则取资源的Img
    private String inRuleIds; //进入该栏目的规则，0为手工/人工创建，其他未系统规则Id
    private String checkRuleIds; //审核规则，0为手工/人工创建，其他为系统规则id
    private Timestamp CTime; //创建时间
    private Timestamp pubTime; //发布时间，发布时的时间，若多次发布，则是最新的发布时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id=id;
    }
    public String getChannelId() {
        return channelId;
    }
    public void setChannelId(String channelId) {
        this.channelId=channelId;
    }
    public String getAssetType() {
        return assetType;
    }
    public void setAssetType(String assetType) {
        this.assetType=assetType;
    }
    public String getAssetId() {
        return assetId;
    }
    public void setAssetId(String assetId) {
        this.assetId=assetId;
    }
    public String getPublisherId() {
        return publisherId;
    }
    public void setPublisherId(String publisherId) {
        this.publisherId=publisherId;
    }
    public String getCheckerId() {
        return checkerId;
    }
    public void setCheckerId(String checkerId) {
        this.checkerId=checkerId;
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
    public int getFlowFlag() {
        return flowFlag;
    }
    public void setFlowFlag(int flowFlag) {
        this.flowFlag=flowFlag;
    }
    public String getPubName() {
        return pubName;
    }
    public void setPubName(String pubName) {
        this.pubName=pubName;
    }
    public String getPubImg() {
        return pubImg;
    }
    public void setPubImg(String pubImg) {
        this.pubImg=pubImg;
    }
    public String getInRuleIds() {
        return inRuleIds;
    }
    public void setInRuleIds(String inRuleIds) {
        this.inRuleIds=inRuleIds;
    }
    public String getCheckRuleIds() {
        return checkRuleIds;
    }
    public void setCheckRuleIds(String checkRuleIds) {
        this.checkRuleIds=checkRuleIds;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime=cTime;
    }
    public Timestamp getPubTime() {
        return pubTime;
    }
    public void setPubTime(Timestamp pubTime) {
        this.pubTime=pubTime;
    }
}