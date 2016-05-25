package com.woting.favorite.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class UserFavoritePo extends BaseObject {
    private static final long serialVersionUID=-8406881716350334538L;

    private String id; //用户词Id
    private int ownerType; //所有者类型
    private String ownerId; //所有者Id,可能是用户也可能是设备
    private String assetType; //内容类型：1电台；2单体媒体资源；3专辑资源；4文本
    private String assetId; //内容Id
    private Timestamp CTime; //记录创建时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id=id;
    }
    public String getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(String ownerId) {
        this.ownerId=ownerId;
    }
    public int getOwnerType() {
        return ownerType;
    }
    public void setOwnerType(int ownerType) {
        this.ownerType=ownerType;
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
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime=cTime;
    }
}