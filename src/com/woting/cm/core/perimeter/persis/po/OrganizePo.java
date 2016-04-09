package com.woting.cm.core.perimeter.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class OrganizePo extends BaseObject {
    private static final long serialVersionUID = -6043023448646954544L;

    private String id;  //uuid(用户id)
    private String orgName; //名称，组织名称
    private String orgTypeId; //组织分类Id，可分为：电台、网站等，是字典项迷城
    private String orgTypeName; //组织分类名称
    private String webPageUrl; //官网地址
    private String orgImg; //组织Logo
    private String descn; //对组织的描述
    private Timestamp CTime; //记录创建时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getOrgName() {
        return orgName;
    }
    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }
    public String getOrgTypeId() {
        return orgTypeId;
    }
    public void setOrgTypeId(String orgTypeId) {
        this.orgTypeId = orgTypeId;
    }
    public String getOrgTypeName() {
        return orgTypeName;
    }
    public void setOrgTypeName(String orgTypeName) {
        this.orgTypeName = orgTypeName;
    }
    public String getWebPageUrl() {
        return webPageUrl;
    }
    public void setWebPageUrl(String webPageUrl) {
        this.webPageUrl = webPageUrl;
    }
    public String getOrgImg() {
        return orgImg;
    }
    public void setOrgImg(String orgImg) {
        this.orgImg = orgImg;
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