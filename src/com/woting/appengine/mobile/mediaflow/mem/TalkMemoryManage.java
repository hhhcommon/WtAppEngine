package com.woting.appengine.mobile.mediaflow.mem;

import java.util.HashMap;
import java.util.Map;

import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.intercom.model.GroupInterCom;
import com.woting.appengine.mobile.mediaflow.model.WholeTalk;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.appengine.mobile.push.model.CompareMsg;
import com.woting.appengine.mobile.push.model.Message;
import com.woting.passport.UGA.persistence.pojo.UserPo;

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
            Message exitPttMsg;
            MobileKey mk;

            PushMemoryManage pmm=PushMemoryManage.getInstance();
            GroupMemoryManage gmm=GroupMemoryManage.getInstance();
            for (String k: this.tm.talkMap.keySet()) {
                WholeTalk wt = this.tm.talkMap.get(k);
                if (wt.getTalkType()==1) {
                    GroupInterCom gic = gmm.getGroupInterCom(wt.getObjId());
                    //判断对讲是否结束
                    boolean talkEnd=false;
                    talkEnd=wt.isCompleted()||gic==null||gic.getSpeaker()==null;
                    if (talkEnd) {
                        this.removeWt(wt);//清除语音内存
                        //发广播消息，推出PTT
                        if (gic.getSpeaker()!=null) {
                            //广播结束消息
                            exitPttMsg=new Message();
                            exitPttMsg.setFromAddr("{(intercom)@@(www.woting.fm||S)}");
                            exitPttMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                            exitPttMsg.setMsgType(1);
                            exitPttMsg.setMsgBizType("INTERCOM_CTL");
                            exitPttMsg.setCmdType("PTT");
                            exitPttMsg.setCommand("b2");
                            dataMap=new HashMap<String, Object>();
                            dataMap.put("GroupId", wt.getObjId());
                            dataMap.put("TalkUserId", wt.getTalkerId());
                            exitPttMsg.setMsgContent(dataMap);
                            //发送广播消息
                            Map<String, UserPo> entryGroupUsers=gic.getEntryGroupUserMap();
                            for (String _k: entryGroupUsers.keySet()) {
                                String _sp[] = _k.split("::");
                                mk=new MobileKey();
                                mk.setMobileId(_sp[0]);
                                mk.setPCDType(Integer.parseInt(_sp[1]));
                                mk.setUserId(_sp[2]);
                                exitPttMsg.setToAddr(MobileUtils.getAddr(mk));
                                pmm.getSendMemory().addUniqueMsg2Queue(mk, exitPttMsg, new CompareGroupMsg());
                            }
                        }
                        gic.delSpeaker(gic.getSpeaker()==null?null:gic.getSpeaker().getUserId());
                    }
                }
            }
        }
    }
    class CompareGroupMsg implements CompareMsg {
        @Override
        public boolean compare(Message msg1, Message msg2) {
            if (msg1.getFromAddr().equals(msg2.getFromAddr())
              &&msg1.getToAddr().equals(msg2.getToAddr())
              &&msg1.getMsgBizType().equals(msg2.getMsgBizType())
              &&msg1.getCmdType().equals(msg2.getCmdType())
              &&msg1.getCommand().equals(msg2.getCommand()) ) {
                if (msg1.getMsgContent()==null&&msg2.getMsgContent()==null) return true;
                if (((msg1.getMsgContent()!=null&&msg2.getMsgContent()!=null))
                  &&(((Map)msg1.getMsgContent()).get("GroupId").equals(((Map)msg2.getMsgContent()).get("GroupId")))) return true;
            }
            return false;
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