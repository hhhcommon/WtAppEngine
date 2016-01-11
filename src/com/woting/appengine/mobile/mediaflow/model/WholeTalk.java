package com.woting.appengine.mobile.mediaflow.model;

import java.util.HashMap;
import java.util.Map;

import com.woting.appengine.mobile.model.MobileKey;

/**
 * 一次完整的通话
 * @author wanghui
 */
public class WholeTalk {
    public WholeTalk() {
        super();
        this.talkData = new HashMap<Integer, TalkSegment>();
    }

    private String talkId; //本段通话的Id
    private String groupId; //本通话的组Id
    private String talkerId; //讲话人Id
    private MobileKey talkerMk; //讲话人Id
    private int MaxNum; //当前通话的最大包数
    private Map<Integer, TalkSegment> talkData; //通话的完整数据

    public String getTalkId() {
        return talkId;
    }
    public void setTalkId(String talkId) {
        this.talkId = talkId;
    }
    public String getGroupId() {
        return groupId;
    }
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    public String getTalkerId() {
        return talkerId;
    }
    public void setTalkerId(String talkerId) {
        this.talkerId = talkerId;
    }
    public int getMaxNum() {
        return MaxNum;
    }
    public void setMaxNum(int maxNum) {
        MaxNum = maxNum;
    }
    public Map<Integer, TalkSegment> getTalkData() {
        return talkData;
    }
    public void setTalkData(Map<Integer, TalkSegment> talkData) {
        this.talkData = talkData;
    }
    public MobileKey getTalkerMk() {
        return talkerMk;
    }
    public void setTalkerMk(MobileKey talkerMk) {
        this.talkerMk = talkerMk;
        this.talkerId=talkerMk.getUserId();
    }

    public void addSegment(TalkSegment ts) {
        if (ts.getWt()==null||!ts.getWt().getTalkId().equals(this.talkId)) throw new IllegalArgumentException("对话段的主对话与当前主对话不匹配");
        this.talkData.put(ts.getSeqNum(), ts);
        if (ts.getSeqNum()>this.MaxNum) this.MaxNum=ts.getSeqNum();
    }

    public boolean isCompleted() {
        TalkSegment ts = talkData.get(-1);
        if (ts==null) return false;
        for (String k: ts.getSendFlags().keySet()) {
            if (!ts.getSendFlags().get(k).equals("2")) return false;
        }
        for (int i=0; i<this.MaxNum; i++) {
            ts = talkData.get(i);
            if (ts==null) return false;
            for (String k: ts.getSendFlags().keySet()) {
                if (!ts.getSendFlags().get(k).equals("2")) return false;
            }
        }
        return true;
    }
}