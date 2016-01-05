package com.woting.passport.friend.persistence.pojo;

import java.sql.Timestamp;
import com.spiritdata.framework.core.model.BaseObject;

/**
 * 
 * @author wh
 */
public class InviteFriendPo extends BaseObject {
    private static final long serialVersionUID = -1814048250735577973L;

    private String id; //主键
    private String aUserId; //第一用户Id
    private String bUserId; //第二用户Id
    private int inviteVector; //邀请方向，总是第一用户邀请第二用户，且是正整数，邀请一次，则增加1，直到邀请成功
    private String inviteMessage; //当前邀请说明文字
    private Timestamp firstInviteTime; //创建时间:首次邀请时间
    private Timestamp inviteTime; //创建时间:本次邀请时间
    private int acceptFlag; //邀请状态：0未处理;1邀请成功;2拒绝邀请
    private Timestamp acceptTime; //接受/拒绝邀请的时间
    private String refuseMessage; //当前邀请说明文字

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
    public String getInviteMessage() {
        return inviteMessage;
    }
    public void setInviteMessage(String inviteMessage) {
        this.inviteMessage = inviteMessage;
    }
    public Timestamp getFirstInviteTime() {
        return firstInviteTime;
    }
    public void setFirstInviteTime(Timestamp firstInviteTime) {
        this.firstInviteTime = firstInviteTime;
    }
    public Timestamp getInviteTime() {
        return inviteTime;
    }
    public void setInviteTime(Timestamp inviteTime) {
        this.inviteTime = inviteTime;
    }
    public int getAcceptFlag() {
        return acceptFlag;
    }
    public void setAcceptFlag(int acceptFlag) {
        this.acceptFlag = acceptFlag;
    }
    public Timestamp getAcceptTime() {
        return acceptTime;
    }
    public void setAcceptTime(Timestamp acceptTime) {
        this.acceptTime = acceptTime;
    }
    public String getRefuseMessage() {
        return refuseMessage;
    }
    public void setRefuseMessage(String refuseMessage) {
        this.refuseMessage = refuseMessage;
    }
}