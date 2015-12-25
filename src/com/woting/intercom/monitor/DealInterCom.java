package com.woting.intercom.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.spiritdata.framework.util.SequenceUUID;
import com.woting.intercom.mem.GroupMemoryManage;
import com.woting.intercom.model.GroupInterCom;
import com.woting.mobile.MobileUtils;
import com.woting.mobile.model.MobileKey;
import com.woting.mobile.push.mem.PushMemoryManage;
import com.woting.mobile.push.model.Message;
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
        String tempStr = "";
        while(true) {
            try {
                sleep(10);
                //读取Receive内存中的typeMsgMap中的内容
                Message m = pmm.getReceiveMemory().pollTypeQueue("INTERCOM_CTL");
                if (m==null) continue;
                
                if (m.getCmdType().equals("GROUP")) {
                    if (m.getCommand().equals("1")) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-用户进入组::(User=("+m.getFromAddr()+");Group="+((Map)m.getMsgContent()).get("GroupId")+")";
                        System.out.println(tempStr);
                        (new EntryGroup("{"+tempStr+"}处理线程", m)).start();
                    } else if (m.getCommand().equals("2")) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-用户退出组::(User=("+m.getFromAddr()+");Group="+((Map)m.getMsgContent()).get("GroupId")+")";
                        System.out.println(tempStr);
                        (new ExitGroup("{"+tempStr+"}处理线程", m)).start();
                    }
                } else if (m.getCmdType().equals("PTT")) {
                    if (m.getCommand().equals("1")) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-开始对讲::(User=("+m.getFromAddr()+");Group="+((Map)m.getMsgContent()).get("GroupId")+")";
                        System.out.println(tempStr);
                        (new BeginPTT("{"+tempStr+"}处理线程", m)).start();
                    } else if (m.getCommand().equals("2")) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-结束对讲::(User=("+m.getFromAddr()+");Group="+((Map)m.getMsgContent()).get("GroupId")+")";
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
            retMsg.setAffirem(0);

            retMsg.setMsgBizType(sourceMsg.getMsgBizType());
            retMsg.setCmdType(sourceMsg.getCmdType());
            retMsg.setCommand("-1");

            retMsg.setSendTime(System.currentTimeMillis());

            GroupInterCom gic = gmm.getGroupInterCom(groupId);
            MobileKey mk = MobileUtils.getMobileKey(sourceMsg);
            if (mk!=null) {
                if (mk.isUser()) {
                    Map<String, Object> dataMap = new HashMap<String, Object>();
                    dataMap.put("GroupId", groupId);
                    retMsg.setMsgContent(dataMap);
                    Map<MobileKey, UserPo> upm = gic.insertEntryUser(mk);
                    if (upm==null) {//该用户不在指定组
                        retMsg.setReturnType("1002");
                        pmm.getSendMemory().addMsg2Queue(mk, retMsg);
                    } else if (upm.size()==0) {//该用户已经在制定组
                        retMsg.setReturnType("1003");
                        pmm.getSendMemory().addMsg2Queue(mk, retMsg);
                    } else {//加入成功
                        retMsg.setReturnType("1001");
                        pmm.getSendMemory().addMsg2Queue(mk, retMsg);

                        //广播消息信息组织
                        Message bMsg = getBroadCastMessage(retMsg);
                        dataMap = new HashMap<String, Object>();
                        dataMap.put("GroupId", groupId);
                        List<Map<String, Object>> inGroupUsers = new ArrayList<Map<String,Object>>();
                        Map<String, Object> um;
                        UserPo up;
                        for (MobileKey k: upm.keySet()) {
                            up=upm.get(k);
                            um=new HashMap<String, Object>();
                            //TODO 这里的号码可能还需要处理
                            um.put("UserId", up.getUserId());
                            um.put("InnerPhoneNum", up.getInnerPhoneNum());
                            inGroupUsers.add(um);
                        }
                        dataMap.put("InGroupUsers", inGroupUsers);
                        bMsg.setMsgContent(dataMap);
                        //发送广播消息
                        for (MobileKey k: upm.keySet()) {
                            pmm.getSendMemory().addUniqueMsg2Queue(k, bMsg);
                        }
                    }
                } else {
                    retMsg.setReturnType("1000");
                    pmm.getSendMemory().addMsg2Queue(mk, retMsg);
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
            retMsg.setAffirem(0);

            retMsg.setMsgBizType(sourceMsg.getMsgBizType());
            retMsg.setCmdType(sourceMsg.getCmdType());
            retMsg.setCommand("-2");

            retMsg.setSendTime(System.currentTimeMillis());

            GroupInterCom gic = gmm.getGroupInterCom(groupId);
            MobileKey mk = MobileUtils.getMobileKey(sourceMsg);
            if (mk!=null) {
                if (mk.isUser()) {
                    Map<String, Object> dataMap = new HashMap<String, Object>();
                    dataMap.put("GroupId", groupId);
                    retMsg.setMsgContent(dataMap);
                    Map<MobileKey, UserPo> upm = gic.delEntryUser(mk);
                    if (upm==null) {//该用户不在指定组
                        retMsg.setReturnType("1002");
                        pmm.getSendMemory().addMsg2Queue(mk, retMsg);
                    } else if (upm.size()==0) {//该用户已经在制定组
                        retMsg.setReturnType("1003");
                        pmm.getSendMemory().addMsg2Queue(mk, retMsg);
                    } else {//正式加入，这时可以广播了
                        retMsg.setReturnType("1001");

                        //广播消息信息组织
                        Message bMsg = getBroadCastMessage(retMsg);
                        dataMap = new HashMap<String, Object>();
                        dataMap.put("GroupId", groupId);
                        List<Map<String, Object>> inGroupUsers = new ArrayList<Map<String,Object>>();
                        Map<String, Object> um;
                        UserPo up;
                        for (MobileKey k: upm.keySet()) {
                            up=upm.get(k);
                            um=new HashMap<String, Object>();
                            //TODO 这里的号码可能还需要处理
                            um.put("UserId", up.getUserId());
                            um.put("InnerPhoneNum", up.getInnerPhoneNum());
                            inGroupUsers.add(um);
                        }
                        dataMap.put("InGroupUsers", inGroupUsers);
                        bMsg.setMsgContent(dataMap);
                        //发送广播消息
                        for (MobileKey k: upm.keySet()) {
                            pmm.getSendMemory().addUniqueMsg2Queue(k, bMsg);
                        }
                    }
                } else {
                    retMsg.setReturnType("1000");
                    pmm.getSendMemory().addMsg2Queue(mk, retMsg);
                }
            }
        }
    }

    /*
     * 开始对讲处理
     */
    class BeginPTT extends Thread {
        private Message sourceMsg;//源消息
        protected BeginPTT(String name, Message sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
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
            retMsg.setAffirem(0);

            retMsg.setMsgBizType(sourceMsg.getMsgBizType());
            retMsg.setCmdType(sourceMsg.getCmdType());
            //retMsg.setCommand(0-sourceMsg.getCommand());

            retMsg.setSendTime(System.currentTimeMillis());

            GroupInterCom gic = gmm.getGroupInterCom(groupId);
            MobileKey mk = MobileUtils.getMobileKey(sourceMsg);
            if (mk!=null) {
                if (mk.isUser()) {
                    
                } else {
                    retMsg.setReturnType("1000");
                    try {
                        pmm.getSendMemory().addSendedMsg(mk, retMsg);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    /*
     * 结束对讲处理
     */
    class EndPTT extends Thread {
        private Message sourceMsg;//源消息
        protected EndPTT(String name, Message sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
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
            retMsg.setAffirem(0);

            retMsg.setMsgBizType(sourceMsg.getMsgBizType());
            retMsg.setCmdType(sourceMsg.getCmdType());
//            retMsg.setCommand(0-sourceMsg.getCommand());

            retMsg.setSendTime(System.currentTimeMillis());

            GroupInterCom gic = gmm.getGroupInterCom(groupId);
            MobileKey mk = MobileUtils.getMobileKey(sourceMsg);
            if (mk!=null) {
                if (mk.isUser()) {
                    
                } else {
                    retMsg.setReturnType("1000");
                    try {
                        pmm.getSendMemory().addSendedMsg(mk, retMsg);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private Message getBroadCastMessage(Message msg) {
        Message ret=new Message();
        ret.setMsgId(SequenceUUID.getUUIDSubSegment(4));

        ret.setFromAddr(msg.getFromAddr());
        ret.setToAddr(msg.getToAddr());

        ret.setMsgType(1);
        ret.setAffirem(msg.getAffirem());

        ret.setMsgBizType(msg.getMsgBizType());
        ret.setCmdType(msg.getCmdType());
        ret.setCommand("b1");

        ret.setSendTime(System.currentTimeMillis());
        return ret;
    }
}