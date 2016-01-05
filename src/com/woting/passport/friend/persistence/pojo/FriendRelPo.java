package com.woting.passport.friend.persistence.pojo;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

/**
 * 好友关系<br/>
 * 对应持久化中数据库的表为wt_Friend_Rel/视图为vWt_Friend_Rel
 * @author wh
 */
public class FriendRelPo extends BaseObject {
    private static final long serialVersionUID = 5104152017388080405L;

    private String id; //主键
    private String aUserId; //第一用户Id
    private String bUserId; //第二用户Id
    private int inviteVector; //邀请方向，总是第一用户邀请第二用户，且是正整数，邀请一次，则增加1，直到邀请成功
    private Timestamp inviteTime; //创建时间:本次邀请时间
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getaUserId() {
        return aUserId;
    }
    public void setaUserId(String aUserId) {
        this.aUserId = aUserId;
    }
    public String getbUserId() {
        return bUserId;
    }
    public void setbUserId(String bUserId) {
        this.bUserId = bUserId;
    }
    public int getInviteVector() {
        return inviteVector;
    }
    public void setInviteVector(int inviteVector) {
        this.inviteVector = inviteVector;
    }
    public Timestamp getInviteTime() {
        return inviteTime;
    }
    public void setInviteTime(Timestamp inviteTime) {
        this.inviteTime = inviteTime;
    }
}