package com.woting.appengine.mobile.mediaflow.mem;

import java.util.HashMap;
import java.util.Map;

import com.spiritdata.framework.util.SequenceUUID;
//import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.intercom.model.GroupInterCom;
import com.woting.appengine.mobile.mediaflow.model.CompareGroupMsg;
import com.woting.appengine.mobile.mediaflow.model.WholeTalk;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
//import com.woting.push.core.message.Message;
import com.woting.push.core.message.MsgNormal;
import com.woting.push.core.message.content.MapContent;
import com.woting.passport.UGA.persis.pojo.UserPo;
import com.woting.passport.mobile.MobileUDKey;

public class TalkMemoryManage {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static TalkMemoryManage instance = new TalkMemoryManage();
    }
    public static TalkMemoryManage getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    //数据区
    protected TalkMemory tm;

    /*
     * 构造方法，初始化消息推送的内存结构
     */
    private TalkMemoryManage() {
        tm=TalkMemory.getInstance();
    }

    public WholeTalk getWholeTalk(String talkId) {
        return this.tm.talkMap.get(talkId);
    }

    public void removeWt(WholeTalk wt) {
//        this.tm.talkMap.remove(wt.getTalkerMk().toString());
        this.tm.talkMap.remove(wt.getTalkId());
    }

    /**
     * 加入内存
     * @param wt
     * @return 返回内存中与这个对讲对应的结构，若内存中已经存在，则返回内存中的结构，否则返回这个新结构
     */
    public void addWt(WholeTalk wt) {
        this.tm.talkMap.put(wt.getTalkId(), wt);
    }

    /**
     * 清除组对讲内存
     */
    public void clean() {
        if (this.tm.talkMap!=null&&!this.tm.talkMap.isEmpty()) {
            Map<String, Object> dataMap;
            MsgNormal exitPttMsg;
            MobileUDKey mUdk;

            PushMemoryManage pmm=PushMemoryManage.getInstance();
            for (String k: this.tm.talkMap.keySet()) {
                WholeTalk wt = this.tm.talkMap.get(k);
                if (wt.getTalkType()==1) {//对讲
                    GroupMemoryManage gmm=GroupMemoryManage.getInstance();
                    GroupInterCom gic = gmm.getGroupInterCom(wt.getObjId());
                    //判断对讲是否结束
                    boolean talkEnd=false;
                    talkEnd=wt.isSendCompleted()||gic==null||gic.getSpeaker()==null;
                    if (talkEnd) {
                        this.removeWt(wt);//清除语音内存
                        //发广播消息，推出PTT
                        if (gic.getSpeaker()!=null) {
                            //广播结束消息
                            exitPttMsg=new MsgNormal();
                            exitPttMsg.setFromType(1);
                            exitPttMsg.setToType(0);
                            exitPttMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                            exitPttMsg.setMsgType(0);
                            exitPttMsg.setBizType(1);
                            exitPttMsg.setCmdType(2);
                            exitPttMsg.setCommand(0x20);
                            dataMap=new HashMap<String, Object>();
                            dataMap.put("GroupId", wt.getObjId());
                            dataMap.put("TalkUserId", wt.getTalkerId());
                            MapContent mc=new MapContent(dataMap);
                            exitPttMsg.setMsgContent(mc);

                            //发送广播消息
                            Map<String, UserPo> entryGroupUsers=gic.getEntryGroupUserMap();
                            for (String _k: entryGroupUsers.keySet()) {
                                String _sp[] = _k.split("::");
                                mUdk=new MobileUDKey();
                                mUdk.setDeviceId(_sp[0]);
                                mUdk.setPCDType(Integer.parseInt(_sp[1]));
                                mUdk.setUserId(_sp[2]);
                                pmm.getSendMemory().addUniqueMsg2Queue(mUdk, exitPttMsg, new CompareGroupMsg());
                            }
                        }
                        gic.delSpeaker(gic.getSpeaker()==null?null:gic.getSpeaker().getUserId());
                        this.removeWt(wt);
                    }
                }
            }
        }
    }

    /**
     * 清除电话通话内容
     * @param callId 通话id
     */
    public void cleanCallData(String callId) {
        if (this.tm.talkMap!=null&&!this.tm.talkMap.isEmpty()) {
            for (String k: this.tm.talkMap.keySet()) {
                WholeTalk wt=this.tm.talkMap.get(k);
                if (wt.getTalkType()==2&&wt.getObjId().equals(callId)) {
                    this.removeWt(wt);
                }
            }
        }
    }
}