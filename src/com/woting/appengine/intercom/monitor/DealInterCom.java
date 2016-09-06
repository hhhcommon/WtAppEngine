package com.woting.appengine.intercom.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.calling.mem.CallingMemoryManage;
import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.intercom.model.GroupInterCom;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.push.core.message.CompareMsg;
import com.woting.push.core.message.MessageUtils;
import com.woting.push.core.message.Message;
import com.woting.push.core.message.MsgMedia;
import com.woting.push.core.message.MsgNormal;
import com.woting.push.core.message.content.MapContent;
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.woting.passport.mobile.MobileUDKey;

public class DealInterCom extends Thread {
    private PushMemoryManage pmm=PushMemoryManage.getInstance();
    private GroupMemoryManage gmm=GroupMemoryManage.getInstance();

    /**
     * 给线程起一个名字的构造函数
     * @param name 线程名称
     */
    public DealInterCom(String name) {
        super("对讲过程监听处理线程"+((name==null||name.trim().length()==0)?"":"::"+name));
    }

    @Override
    public void run() {
        System.out.println(this.getName()+"开始执行");
        String tempStr="";
        while(true) {
            try {
                sleep(10);
                //读取Receive内存中的typeMsgMap中的内容
                MsgNormal m=(MsgNormal)pmm.getReceiveMemory().pollTypeQueue("1");
                if (m==null) continue;
                if (m.getCmdType()==1) {
                    if (m.getCommand()==1) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-用户进入组::(User="+MobileUDKey.buildFromMsg(m)+";Group="+((MapContent)m.getMsgContent()).get("GroupId")+")";
                        System.out.println(tempStr);
                        (new EntryGroup("{"+tempStr+"}处理线程", m)).start();
                    } else if (m.getCommand()==2) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-用户退出组::(User="+MobileUDKey.buildFromMsg(m)+";Group="+((MapContent)m.getMsgContent()).get("GroupId")+")";
                        System.out.println(tempStr);
                        (new ExitGroup("{"+tempStr+"}处理线程", m)).start();
                    }
                } else if (m.getCmdType()==2) {
                    if (m.getCommand()==1) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-开始对讲::(User="+MobileUDKey.buildFromMsg(m)+";Group="+((MapContent)m.getMsgContent()).get("GroupId")+")";
                        System.out.println(tempStr);
                        (new BeginPTT("{"+tempStr+"}处理线程", m)).start();
                    } else if (m.getCommand()==2) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-结束对讲::(User="+MobileUDKey.buildFromMsg(m)+";Group="+((MapContent)m.getMsgContent()).get("GroupId")+")";
                        System.out.println(tempStr);
                        (new EndPTT("{"+tempStr+"}处理线程", m)).start();
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    //进入组处理
    class EntryGroup extends Thread {
        private MsgNormal sourceMsg;//源消息
        protected EntryGroup(String name, MsgNormal sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
            MobileUDKey mUdk=MobileUDKey.buildFromMsg(sourceMsg);
            if (mUdk==null) return;

            String groupId="";
            try {
                groupId+=((MapContent)sourceMsg.getMsgContent()).get("GroupId");
            } catch(Exception e) {}
            if (groupId.length()==0) return;
            MsgNormal retMsg=MessageUtils.buildAckMsg(sourceMsg);
            retMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            retMsg.setBizType(sourceMsg.getBizType());
            retMsg.setCmdType(sourceMsg.getCmdType());
            retMsg.setCommand(9);
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("GroupId", groupId);
            MapContent mc=new MapContent(dataMap);
            retMsg.setMsgContent(mc);

            Map<String, Object> retM=null;
            GroupInterCom gic=gmm.getGroupInterCom(groupId);
            if (!mUdk.isUser()) retMsg.setReturnType(0x00);
            else if (gic==null) retMsg.setReturnType(0x02);
            else {
                retM=gic.insertEntryUser(mUdk);
                String rt = (String)retM.get("returnType");
                if (rt.equals("3")) retMsg.setReturnType(0x40);//该用户不在指定组
                else if (rt.equals("2")) retMsg.setReturnType(0x08);//该用户已经在指定组
                else retMsg.setReturnType(0x01);//正确加入组
            }
            pmm.getSendMemory().addMsg2Queue(mUdk, retMsg);

            //广播消息信息组织
            if (retM!=null&&retM.containsKey("needBroadCast")) {
                MsgNormal bMsg=getBroadCastMessage(retMsg);
                bMsg.setCommand(0x10);
                dataMap=new HashMap<String, Object>();
                dataMap.put("GroupId", groupId);
                List<Map<String, Object>> inGroupUsers=new ArrayList<Map<String,Object>>();
                Map<String, Object> um;
                UserPo up;
                Map<String, UserPo> entryGroupUsers=(Map<String, UserPo>)retM.get("entryGroupUsers");
                for (String k: entryGroupUsers.keySet()) {
                    up=entryGroupUsers.get(k);
                    um=new HashMap<String, Object>();
                    um.put("UserId", up.getUserId());
                    inGroupUsers.add(um);
                }
                dataMap.put("InGroupUsers", inGroupUsers);
                MapContent _mc=new MapContent(dataMap);
                bMsg.setMsgContent(_mc);
                //发送广播消息
                for (String k: entryGroupUsers.keySet()) {
                    String _sp[] = k.split("::");
                    mUdk=new MobileUDKey();
                    mUdk.setDeviceId(_sp[0]);
                    mUdk.setPCDType(Integer.parseInt(_sp[1]));
                    mUdk.setUserId(_sp[2]);
                    pmm.getSendMemory().addUniqueMsg2Queue(mUdk, bMsg, new CompareGroupMsg());
                }
            }
        }
    }
    //退出组处理
    class ExitGroup extends Thread {
        private MsgNormal sourceMsg;//源消息
        protected ExitGroup(String name, MsgNormal sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
            MobileUDKey mUdk=MobileUDKey.buildFromMsg(sourceMsg);
            if (mUdk==null) return;

            String groupId="";
            try {
                groupId+=((MapContent)sourceMsg.getMsgContent()).get("GroupId");
            } catch(Exception e) {}
            if (groupId.length()==0) return;
            MsgNormal retMsg=MessageUtils.buildAckMsg(sourceMsg);
            retMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            retMsg.setBizType(sourceMsg.getBizType());
            retMsg.setCmdType(sourceMsg.getCmdType());
            retMsg.setCommand(0x0A);
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("GroupId", groupId);
            MapContent mc=new MapContent(dataMap);
            retMsg.setMsgContent(mc);

            Map<String, Object> retM=null;
            GroupInterCom gic=gmm.getGroupInterCom(groupId);
            if (!mUdk.isUser()) retMsg.setReturnType(0x00);
            else if (gic==null) retMsg.setReturnType(0x02);
            else {
                retM=gic.delEntryUser(mUdk);
                String rt = (String)retM.get("returnType");
                if (rt.equals("3")) retMsg.setReturnType(0x04);//该用户不在指定组
                else if (rt.equals("2")) retMsg.setReturnType(0x08);//用户未在对讲
                else retMsg.setReturnType(1);//正确加入组
            }
            pmm.getSendMemory().addMsg2Queue(mUdk, retMsg);
            
            //删除所有通过这个组发给他的消息
            ConcurrentLinkedQueue<Message> tempMsgs=pmm.getSendMemory().getSendQueue(mUdk);
            if (tempMsgs!=null&&!tempMsgs.isEmpty()) {
                for (Message m: tempMsgs) {
                    if (m instanceof MsgMedia) {
                        MsgMedia _mm=(MsgMedia)m;
                        if (_mm.getBizType()==1&&groupId.equals(_mm.getObjId())) {
                            tempMsgs.remove(m);
                        }
                    }
                }
            }

            //广播消息信息组织
            if (retM!=null&&retM.containsKey("needBroadCast")) {
                MsgNormal bMsg=getBroadCastMessage(retMsg);
                bMsg.setCommand(0x10);
                dataMap=new HashMap<String, Object>();
                dataMap.put("GroupId", groupId);
                List<Map<String, Object>> inGroupUsers=new ArrayList<Map<String,Object>>();
                Map<String, Object> um;
                UserPo up;
                Map<String, UserPo> entryGroupUsers=(Map<String, UserPo>)retM.get("entryGroupUsers");
                for (String k: entryGroupUsers.keySet()) {
                    up=entryGroupUsers.get(k);
                    um=new HashMap<String, Object>();
                    //TODO 这里的号码可能还需要处理
                    um.put("UserId", up.getUserId());
                    inGroupUsers.add(um);
                }
                dataMap.put("InGroupUsers", inGroupUsers);
                MapContent _mc=new MapContent(dataMap);
                bMsg.setMsgContent(_mc);
                //发送广播消息
                for (String k: entryGroupUsers.keySet()) {
                    String _sp[] = k.split("::");
                    mUdk=new MobileUDKey();
                    mUdk.setDeviceId(_sp[0]);
                    mUdk.setPCDType(Integer.parseInt(_sp[1]));
                    mUdk.setUserId(_sp[2]);
                    pmm.getSendMemory().addUniqueMsg2Queue(mUdk, bMsg, new CompareGroupMsg());
                }
            }
        }
    }

    //开始对讲
    class BeginPTT extends Thread {
        private MsgNormal sourceMsg;//源消息
        protected BeginPTT(String name, MsgNormal sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
            MobileUDKey mUdk=MobileUDKey.buildFromMsg(sourceMsg);
            if (mUdk==null) return;

            String groupId="";
            try {
                groupId+=((MapContent)sourceMsg.getMsgContent()).get("GroupId");
            } catch(Exception e) {}
            if (groupId.length()==0) return;

            MsgNormal retMsg=MessageUtils.buildAckMsg(sourceMsg);
            retMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            retMsg.setBizType(sourceMsg.getBizType());
            retMsg.setCmdType(sourceMsg.getCmdType());
            retMsg.setCommand(9);
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("GroupId", groupId);
            MapContent mc=new MapContent(dataMap);
            retMsg.setMsgContent(mc);

            GroupInterCom gic=gmm.getGroupInterCom(groupId);
            if (!mUdk.isUser()) retMsg.setReturnType(0x00);
            else if (gic==null) retMsg.setReturnType(0x02);
            else {
                gic.setLastTalkTime(gic.getSpeaker()==null?null:gic.getSpeaker().getUserId());
                Map<String, UserPo> _m=gic.setSpeaker(mUdk);
                if (_m.containsKey("E")) retMsg.setReturnType(0x04);
                else if (_m.containsKey("O")) retMsg.setReturnType(0x05);
                else if (_m.containsKey("F")) retMsg.setReturnType(0x08);
                else if (CallingMemoryManage.getInstance().isTalk(mUdk.getUserId(),"")) retMsg.setReturnType(0x90);//电话通话判断 //TODO 这里应该用全局锁
                else retMsg.setReturnType(0x01);//成功可以开始对讲了
            }
            pmm.getSendMemory().addMsg2Queue(mUdk, retMsg);

            //广播开始对讲消息
            if (retMsg.getReturnType()==1) {
                MsgNormal bMsg=getBroadCastMessage(retMsg);
                retMsg.setReMsgId(sourceMsg.getMsgId());
                bMsg.setCommand(0x10);
                dataMap=new HashMap<String, Object>();
                dataMap.put("GroupId", groupId);
                dataMap.put("TalkUserId", mUdk.getUserId());
                MapContent _mc=new MapContent(dataMap);
                bMsg.setMsgContent(_mc);
                //发送广播消息
                String ptterId=mUdk.getUserId();
                Map<String, UserPo> entryGroupUsers=gic.getEntryGroupUserMap();
                for (String k: entryGroupUsers.keySet()) {
                    String _sp[] = k.split("::");
                    if (ptterId.equals(_sp[1])) continue;
                    mUdk=new MobileUDKey();
                    mUdk.setDeviceId(_sp[0]);
                    mUdk.setPCDType(Integer.parseInt(_sp[1]));
                    mUdk.setUserId(_sp[2]);
                    pmm.getSendMemory().addUniqueMsg2Queue(mUdk, bMsg, new CompareGroupMsg());
                }
            }
        }
    }
    //结束对讲
    class EndPTT extends Thread {
        private MsgNormal sourceMsg;//源消息
        protected EndPTT(String name, MsgNormal sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
            MobileUDKey mUdk=MobileUDKey.buildFromMsg(sourceMsg);
            if (mUdk==null) return;

            String groupId="";
            try {
                groupId+=((MapContent)sourceMsg.getMsgContent()).get("GroupId");
            } catch(Exception e) {}
            if (groupId.length()==0) return;
            MsgNormal retMsg=MessageUtils.buildAckMsg(sourceMsg);
            retMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            retMsg.setBizType(sourceMsg.getBizType());
            retMsg.setCmdType(sourceMsg.getCmdType());
            retMsg.setCommand(0x0A);
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("GroupId", groupId);
            MapContent mc=new MapContent(dataMap);
            retMsg.setMsgContent(mc);

            GroupInterCom gic=gmm.getGroupInterCom(groupId);
            if (!mUdk.isUser()) retMsg.setReturnType(0x00);
            else if (gic==null) retMsg.setReturnType(0x02);
            else {
                gic.setLastTalkTime(gic.getSpeaker()==null?null:gic.getSpeaker().getUserId());
                int _r=gic.endPTT(mUdk);
                if (_r==-1) retMsg.setReturnType(0x04);
                else if (_r==0) retMsg.setReturnType(0x08);
                else retMsg.setReturnType(0x01);//结束对讲
            }
            pmm.getSendMemory().addMsg2Queue(mUdk, retMsg);

            /**不在这里处理了，在收到所有的包后处理这个逻辑
            if (retMsg.getReturnType().equals("1001")) {
                //广播开始对讲消息，放到收到结束语音包的时候再处理了
                Message bMsg=getBroadCastMessage(retMsg);
                bMsg.setCommand("b2");
                dataMap=new HashMap<String, Object>();
                dataMap.put("GroupId", groupId);
                dataMap.put("TalkUserId", mk.getUserId());
                bMsg.setMsgContent(dataMap);
                //发送广播消息
                Map<String, UserPo> entryGroupUsers=gic.getEntryGroupUserMap();
                for (String k: entryGroupUsers.keySet()) {
                    String _sp[] = k.split("::");
                    mk=new MobileKey();
                    mk.setMobileId(_sp[0]);
                    mk.setUserId(_sp[1]);
                    bMsg.setToAddr(MobileUtils.getAddr(mk));
                    pmm.getSendMemory().addUniqueMsg2Queue(mk, bMsg, new CompareGroupMsg());
                }
            }
            **/
        }
    }

    private MsgNormal getBroadCastMessage(MsgNormal msg) {
        MsgNormal ret=new MsgNormal();
        ret.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        ret.setFromType(1);
        ret.setToType(0);
        ret.setMsgType(0);
        ret.setAffirm(msg==null?1:msg.getAffirm());
        ret.setBizType(msg.getBizType());
        ret.setCmdType(msg.getCmdType());
        return ret;
    }

    class CompareGroupMsg implements CompareMsg<MsgNormal> {
        @Override
        public boolean compare(MsgNormal msg1, MsgNormal msg2) {
            if (msg1.getFromType()==msg2.getFromType()
              &&msg1.getToType()==msg2.getToType()
              &&msg1.getBizType()==msg2.getBizType()
              &&msg1.getCmdType()==msg2.getCmdType()
              &&msg1.getCommand()==msg2.getCommand() ) {
                if (msg1.getMsgContent()==null&&msg2.getMsgContent()==null) return true;
                if (((msg1.getMsgContent()!=null&&msg2.getMsgContent()!=null))
                  &&(((MapContent)msg1.getMsgContent()).get("GroupId").equals(((MapContent)msg2.getMsgContent()).get("GroupId")))) return true;
            }
            return false;
        }
    }
}