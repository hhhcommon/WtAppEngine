package com.woting.appengine.intercom.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.calling.mem.CallingMemoryManage;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.intercom.model.GroupInterCom;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.appengine.mobile.push.model.CompareMsg;
import com.woting.appengine.mobile.push.model.Message;
import com.woting.passport.UGA.persistence.pojo.UserPo;

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
                Message m=pmm.getReceiveMemory().pollTypeQueue("INTERCOM_CTL");
                if (m==null) continue;

                if (m.getCmdType().equals("GROUP")) {
                    if (m.getCommand().equals("1")) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-用户进入组::(User="+m.getFromAddr()+";Group="+((Map)m.getMsgContent()).get("GroupId")+")";
                        System.out.println(tempStr);
                        (new EntryGroup("{"+tempStr+"}处理线程", m)).start();
                    } else if (m.getCommand().equals("2")) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-用户退出组::(User="+m.getFromAddr()+";Group="+((Map)m.getMsgContent()).get("GroupId")+")";
                        System.out.println(tempStr);
                        (new ExitGroup("{"+tempStr+"}处理线程", m)).start();
                    }
                } else if (m.getCmdType().equals("PTT")) {
                    if (m.getCommand().equals("1")) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-开始对讲::(User="+m.getFromAddr()+";Group="+((Map)m.getMsgContent()).get("GroupId")+")";
                        System.out.println(tempStr);
                        (new BeginPTT("{"+tempStr+"}处理线程", m)).start();
                    } else if (m.getCommand().equals("2")) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-结束对讲::(User="+m.getFromAddr()+";Group="+((Map)m.getMsgContent()).get("GroupId")+")";
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
        private Message sourceMsg;//源消息
        protected EntryGroup(String name, Message sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
            MobileKey mk=MobileUtils.getMobileKey(sourceMsg,1);
            if (mk==null) return;

            String groupId="";
            try {
                groupId+=((Map)sourceMsg.getMsgContent()).get("GroupId");
            } catch(Exception e) {}
            if (groupId.length()==0) return;
            Message retMsg=new Message();
            retMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            retMsg.setReMsgId(sourceMsg.getMsgId());
            retMsg.setToAddr(sourceMsg.getFromAddr());
            retMsg.setFromAddr(sourceMsg.getToAddr());
            retMsg.setMsgType(-1);
            retMsg.setAffirm(0);
            retMsg.setMsgBizType(sourceMsg.getMsgBizType());
            retMsg.setCmdType(sourceMsg.getCmdType());
            retMsg.setCommand("-1");
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("GroupId", groupId);
            retMsg.setMsgContent(dataMap);

            Map<String, Object> retM=null;
            GroupInterCom gic=gmm.getGroupInterCom(groupId);
            if (!mk.isUser()) retMsg.setReturnType("999");
            else if (gic==null) retMsg.setReturnType("1000");
            else {
                retM=gic.insertEntryUser(mk);
                String rt = (String)retM.get("returnType");
                if (rt.equals("3")) retMsg.setReturnType("1002");//该用户不在指定组
                else if (rt.equals("2")) retMsg.setReturnType("1003");//该用户已经在指定组
                else retMsg.setReturnType("1001");//正确加入组
            }
            pmm.getSendMemory().addMsg2Queue(mk, retMsg);

            //广播消息信息组织
            if (retM!=null&&retM.containsKey("needBroadCast")) {
                Message bMsg=getBroadCastMessage(retMsg);
                bMsg.setCommand("b1");
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
                bMsg.setMsgContent(dataMap);
                //发送广播消息
                for (String k: entryGroupUsers.keySet()) {
                    String _sp[] = k.split("::");
                    mk=new MobileKey();
                    mk.setMobileId(_sp[0]);
                    mk.setPCDType(Integer.parseInt(_sp[1]));
                    mk.setUserId(_sp[2]);
                    bMsg.setToAddr(MobileUtils.getAddr(mk));
                    pmm.getSendMemory().addUniqueMsg2Queue(mk, bMsg, new CompareGroupMsg());
                }
            }
        }
    }
    //退出组处理
    class ExitGroup extends Thread {
        private Message sourceMsg;//源消息
        protected ExitGroup(String name, Message sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
            MobileKey mk=MobileUtils.getMobileKey(sourceMsg,1);
            if (mk==null) return;

            String groupId="";
            try {
                groupId+=((Map)sourceMsg.getMsgContent()).get("GroupId");
            } catch(Exception e) {}
            if (groupId.length()==0) return;
            Message retMsg=new Message();
            retMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            retMsg.setReMsgId(sourceMsg.getMsgId());
            retMsg.setToAddr(sourceMsg.getFromAddr());
            retMsg.setFromAddr(sourceMsg.getToAddr());
            retMsg.setMsgType(1);
            retMsg.setAffirm(0);
            retMsg.setMsgBizType(sourceMsg.getMsgBizType());
            retMsg.setCmdType(sourceMsg.getCmdType());
            retMsg.setCommand("-2");
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("GroupId", groupId);
            retMsg.setMsgContent(dataMap);

            Map<String, Object> retM=null;
            GroupInterCom gic=gmm.getGroupInterCom(groupId);
            if (!mk.isUser()) retMsg.setReturnType("999");
            else if (gic==null) retMsg.setReturnType("1000");
            else {
                retM=gic.delEntryUser(mk);
                String rt = (String)retM.get("returnType");
                if (rt.equals("3")) retMsg.setReturnType("1002");//该用户不在指定组
                else if (rt.equals("2")) retMsg.setReturnType("1003");//该用户已经在指定组
                else retMsg.setReturnType("1001");//正确加入组
            }
            pmm.getSendMemory().addMsg2Queue(mk, retMsg);
            
            //广播消息信息组织
            if (retM!=null&&retM.containsKey("needBroadCast")) {
                Message bMsg=getBroadCastMessage(retMsg);
                bMsg.setCommand("b1");
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
                bMsg.setMsgContent(dataMap);
                //发送广播消息
                for (String k: entryGroupUsers.keySet()) {
                    String _sp[] = k.split("::");
                    mk=new MobileKey();
                    mk.setMobileId(_sp[0]);
                    mk.setPCDType(Integer.parseInt(_sp[1]));
                    mk.setUserId(_sp[2]);
                    bMsg.setToAddr(MobileUtils.getAddr(mk));
                    pmm.getSendMemory().addUniqueMsg2Queue(mk, bMsg, new CompareGroupMsg());
                }
            }
            //删除所有通过这个组发给他的消息
            ConcurrentLinkedQueue<Message> tempMsgs = pmm.getSendMemory().getSendQueue(mk);
            if (tempMsgs!=null&&!tempMsgs.isEmpty()) {
                for (Message m: tempMsgs) {
                    if (m.getMsgBizType().equals("AUDIOFLOW")&&m.getCmdType().equals("TALK_INTERCOM")&&groupId.equals(((Map)m.getMsgContent()).get("ObjId")+"")) {
                        tempMsgs.remove(m);
                    }
                }
            }
        }
    }

    //开始对讲
    class BeginPTT extends Thread {
        private Message sourceMsg;//源消息
        protected BeginPTT(String name, Message sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
            MobileKey mk=MobileUtils.getMobileKey(sourceMsg,1);
            if (mk==null) return;

            String groupId="";
            try {
                groupId+=((Map)sourceMsg.getMsgContent()).get("GroupId");
            } catch(Exception e) {}
            if (groupId.length()==0) return;
            Message retMsg=new Message();
            retMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            retMsg.setReMsgId(sourceMsg.getMsgId());
            retMsg.setToAddr(sourceMsg.getFromAddr());
            retMsg.setFromAddr(sourceMsg.getToAddr());
            retMsg.setMsgType(-1);
            retMsg.setAffirm(0);
            retMsg.setMsgBizType(sourceMsg.getMsgBizType());
            retMsg.setCmdType(sourceMsg.getCmdType());
            retMsg.setCommand("-1");
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("GroupId", groupId);
            retMsg.setMsgContent(dataMap);

            GroupInterCom gic=gmm.getGroupInterCom(groupId);
            if (!mk.isUser()) retMsg.setReturnType("999");
            else if (gic==null) retMsg.setReturnType("1000");
            else {
                gic.setLastTalkTime(gic.getSpeaker()==null?null:gic.getSpeaker().getUserId());
                Map<String, UserPo> _m=gic.setSpeaker(mk);
                if (_m.containsKey("E")) retMsg.setReturnType("1002");
                else if (_m.containsKey("O")) retMsg.setReturnType("1003");
                else if (_m.containsKey("F")) retMsg.setReturnType("2001");
                else if (CallingMemoryManage.getInstance().isTalk(mk.getUserId(),"")) retMsg.setReturnType("2002");//电话通话判断 //TODO 这里应该用全局锁
                else retMsg.setReturnType("1001");//成功可以开始对讲了
            }
            pmm.getSendMemory().addMsg2Queue(mk, retMsg);

            //广播开始对讲消息
            if (retMsg.getReturnType().equals("1001")) {
                Message bMsg=getBroadCastMessage(retMsg);
                bMsg.setCommand("b1");
                dataMap=new HashMap<String, Object>();
                dataMap.put("GroupId", groupId);
                dataMap.put("TalkUserId", mk.getUserId());
                bMsg.setMsgContent(dataMap);
                //发送广播消息
                String ptterId=mk.getUserId();
                Map<String, UserPo> entryGroupUsers=gic.getEntryGroupUserMap();
                for (String k: entryGroupUsers.keySet()) {
                    String _sp[] = k.split("::");
                    if (ptterId.equals(_sp[1])) continue;
                    mk=new MobileKey();
                    mk.setMobileId(_sp[0]);
                    mk.setPCDType(Integer.parseInt(_sp[1]));
                    mk.setUserId(_sp[2]);
                    bMsg.setToAddr(MobileUtils.getAddr(mk));
                    pmm.getSendMemory().addUniqueMsg2Queue(mk, bMsg, new CompareGroupMsg());
                }
            }
        }
    }
    //结束对讲
    class EndPTT extends Thread {
        private Message sourceMsg;//源消息
        protected EndPTT(String name, Message sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
            MobileKey mk=MobileUtils.getMobileKey(sourceMsg,1);
            if (mk==null) return;

            String groupId="";
            try {
                groupId+=((Map)sourceMsg.getMsgContent()).get("GroupId");
            } catch(Exception e) {}
            if (groupId.length()==0) return;
            Message retMsg=new Message();
            retMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            retMsg.setReMsgId(sourceMsg.getMsgId());
            retMsg.setToAddr(sourceMsg.getFromAddr());
            retMsg.setFromAddr(sourceMsg.getToAddr());
            retMsg.setMsgType(-1);
            retMsg.setAffirm(0);
            retMsg.setMsgBizType(sourceMsg.getMsgBizType());
            retMsg.setCmdType(sourceMsg.getCmdType());
            retMsg.setCommand("-2");
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("GroupId", groupId);
            retMsg.setMsgContent(dataMap);

            GroupInterCom gic=gmm.getGroupInterCom(groupId);
            if (!mk.isUser()) retMsg.setReturnType("999");
            else if (gic==null) retMsg.setReturnType("1000");
            else {
                gic.setLastTalkTime(gic.getSpeaker()==null?null:gic.getSpeaker().getUserId());
                int _r=gic.endPTT(mk);
                if (_r==-1) retMsg.setReturnType("1002");
                else if (_r==0) retMsg.setReturnType("1003");
                else retMsg.setReturnType("1001");//结束对讲
            }
            pmm.getSendMemory().addMsg2Queue(mk, retMsg);

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

    private Message getBroadCastMessage(Message msg) {
        Message ret=new Message();
        ret.setMsgId(SequenceUUID.getUUIDSubSegment(4));

        ret.setFromAddr(msg.getFromAddr());
        ret.setToAddr(msg.getToAddr());

        ret.setMsgType(1);
        ret.setAffirm(msg.getAffirm());

        ret.setMsgBizType(msg.getMsgBizType());
        ret.setCmdType(msg.getCmdType());
        return ret;
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
}