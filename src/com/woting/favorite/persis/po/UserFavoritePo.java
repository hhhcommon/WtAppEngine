package com.woting.favorite.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class UserFavoritePo extends BaseObject {
    private static final long serialVersionUID=-8406881716350334538L;

    private String id; //用户词Id
    private int ownerType; //所有者类型
    private String ownerId; //所有者Id,可能是用户也可能是设备
    private String resTableName; //内容类型：1电台；2单体媒体资源；3专辑资源；4文本
    private String resId; //内容Id
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
    public String getResTableName() {
        return resTableName;
    }
    public void setResTableName(String resTableName) {
        this.resTableName = resTableName;
    }
    public String getResId() {
        return resId;
    }
    public void setResId(String resId) {
        this.resId = resId;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime=cTime;
    }
}