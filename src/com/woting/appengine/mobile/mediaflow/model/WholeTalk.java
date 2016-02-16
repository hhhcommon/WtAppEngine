package com.woting.appengine.mobile.mediaflow.model;

import java.util.HashMap;
import java.util.Map;

import com.woting.appengine.mobile.model.MobileKey;

/**
 * 一次完整的通话
 * @author wanghui
 */
public class WholeTalk {
    protected long cycleTime=500;//周期时间，0.5秒
    private int expiresT=5; //过期周期
    //以上是控制时间的周期

    public WholeTalk() {
        super();
        this.talkData = new HashMap<Integer, TalkSegment>();
    }

    private String talkId; //本段通话的Id
    private String groupId; //本通话的组Id
    private MobileKey talkerMk; //讲话人Id
    private int MaxNum=0; //当前通话的最大包数
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
    }
    public String getTalkerId() {
        return this.talkerMk.getUserId();
    }

    /**
     * 加入一段音音频传输
     * @param ts
     */
    public void addSegment(TalkSegment ts) {
        if (ts.getWt()==null||!ts.getWt().getTalkId().equals(this.talkId)) throw new IllegalArgumentException("对话段的主对话与当前对话段不匹配");
        this.talkData.put(ts.getSeqNum(), ts);
        if (ts.getSeqNum()>this.MaxNum) this.MaxNum=ts.getSeqNum();
    }

    /**
     * 是否传输完成
     * @return
     */
    public boolean isCompleted() {
        TalkSegment ts=talkData.get(new Integer(-1));
        if (ts==null) return false;
        for (String k: ts.getSendFlags().keySet()) {
            if (!ts.getSendFlags().get(k).equals("2")) return false;
        }
        int lowIndex=this.MaxNum-expiresT;
        for (int i=(lowIndex>=0?lowIndex:0); i<this.MaxNum; i++) {
            ts = talkData.get(i);
            if (ts==null) return false;
            for (String k: ts.getSendFlags().keySet()) {
                if (!ts.getSendFlags().get(k).equals("2")) return false;
            }
        }
        return true;
    }

    /**
     * 是否接收到了全部的包
     * @return
     */
    public boolean isReceiveCompleted() {
        TalkSegment ts=talkData.get(new Integer(-1));
        if (ts==null) return false;
        int lowIndex=this.MaxNum-expiresT;
        for (int i=(lowIndex>=0?lowIndex:0); i<this.MaxNum; i++) {//注意，若5个周期前的数据没有收到，则认为还没有全部收到
            ts = talkData.get(i);
            if (ts==null) return false;
        }
        return true;
    }
}