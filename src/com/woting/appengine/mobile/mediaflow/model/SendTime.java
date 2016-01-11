package com.woting.appengine.mobile.mediaflow.model;

import java.util.Date;

/**
 * 传送具体时间
 * @author wanghui
 */
public class SendTime {
    private String talkId; //本段通话的Id
    private int seqNum; //序列号：从而得到是那个segment
    private String userId; //对应的用户Id
    private Date time; //发送时间
    public String getTalkId() {
        return talkId;
    }
    public void setTalkId(String talkId) {
        this.talkId = talkId;
    }
    public int getSeqNum() {
        return seqNum;
    }
    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public Date getTime() {
        return time;
    }
    public void setTime(Date time) {
        this.time = time;
    }
}