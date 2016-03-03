package com.woting.passport.groupinvite.persistence.pojo;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

/**
 * 群组用户邀请/申请信息
 * @author wh
 */
public class InviteGroupPo extends BaseObject {
    private static final long serialVersionUID = -1814048250735577973L;

    private String id; //主键
    private String aUserId; //主请用户Id
    private String bUserId; //被请用户Id
    private String groupId; //邀请的组Id
    private int inviteVector; //邀请次数，则增加1，直到邀请成功
    private String inviteMessage; //当前邀请说明文字
    private Timestamp firstInviteTime; //创建时间:首次邀请时间
    private Timestamp inviteTime; //创建时间:本次邀请时间
    private int acceptFlag; //邀请状态：0未处理;1邀请成功;2拒绝邀请
    private int managerFlag; //组管理员处理类型，只有审核组的邀请需要得到管理员的认可，0未处理,1通过,2拒绝
    private Timestamp acceptTime; //接受/拒绝邀请的时间
    private String refuseMessage; //当前邀请说明文字
    private String flag; //状态，1=正在用的组；2=组已被删除，这样的记录groupId在Group组中不必有关联主键

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
    public String getGroupId() {
        return groupId;
    }
    public void setGroupId(String groupId) {
        this.groupId = groupId;
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
    public int getManagerFlag() {
        return managerFlag;
    }
    public void setManagerFlag(int managerFlag) {
        this.managerFlag = managerFlag;
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
    public String getFlag() {
        return flag;
    }
    public void setFlag(String flag) {
        this.flag = flag;
    }
}