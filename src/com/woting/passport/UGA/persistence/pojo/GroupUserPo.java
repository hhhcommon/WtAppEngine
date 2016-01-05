package com.woting.passport.UGA.persistence.pojo;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class GroupUserPo extends BaseObject {
    private static final long serialVersionUID = 5582172023664319397L;

    private String id;
    private String groupId;
    private String userId;
    private String inviter;
    private Timestamp CTime; //记录创建时间
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getGroupId() {
        return groupId;
    }
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getInviter() {
        return inviter;
    }
    public void setInviter(String inviter) {
        this.inviter = inviter;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime = cTime;
    }
}