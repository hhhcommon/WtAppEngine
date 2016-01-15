package com.woting.appengine.mobile.mediaflow.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.woting.passport.UGA.persistence.pojo.UserPo;

/**
 * 一段对话
 * @author wanghui
 */
public class TalkSegment {
    public TalkSegment() {
        super();
        this.sendUsers = new HashMap<String, UserPo>();
        this.sendFlags = new HashMap<String, String>();
        this.sendTime = new HashMap<String, List<Date>>();
    }

    private WholeTalk wt; //本段话对应的完整对话
    private byte[] data; //本段对话的实际
    private long begin; //开始时间点：离通话开始时间
    private long end; //本包结束的时间点：离通话开始时间
    private int seqNum; //序列号：从0开始
    private Map<String, UserPo> sendUsers; //需要传输的用户列表
    private Map<String/*userId*/, String/*状态：0未传送；1已传送；2传送成功；3传送失败*/> sendFlags; //为各用户传送数据的结果情况
    private Map<String/*userId*/, List<Date>> sendTime; //为各用户传送数据的结果情况

    public WholeTalk getWt() {
        return wt;
    }
    public void setWt(WholeTalk wt) {
        this.wt = wt;
    }
    public byte[] getData() {
        return data;
    }
    public void setData(byte[] data) {
        this.data = data;
    }
    public long getBegin() {
        return begin;
    }
    public void setBegin(long begin) {
        this.begin = begin;
    }
    public long getEnd() {
        return end;
    }
    public void setEnd(long end) {
        this.end = end;
    }
    public int getSeqNum() {
        return seqNum;
    }
    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }
    public Map<String, UserPo> getSendUsers() {
        return sendUsers;
    }
    public void setSendUsers(Map<String, UserPo> sendUsers) {
        this.sendUsers = sendUsers;
        for (String k: sendUsers.keySet()) {
            this.sendFlags.put(k, "0");
            this.sendTime.put(k, new ArrayList<Date>());
        }
    }
    public Map<String, String> getSendFlags() {
        return sendFlags;
    }
    public void setSendFlags(Map<String, String> sendFlags) {
        this.sendFlags = sendFlags;
    }
    public Map<String, List<Date>> getSendTime() {
        return sendTime;
    }
    public void addSendTime(String uk, Date d) {
        this.sendTime.get(uk).add(d);
    }
}