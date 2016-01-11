package com.woting.appengine.mobile.mediaflow.monitor;

import java.util.HashMap;
import java.util.Map;

import com.mysql.jdbc.StringUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.intercom.model.GroupInterCom;
import com.woting.appengine.mobile.mediaflow.mem.TalkMemoryManage;
import com.woting.appengine.mobile.mediaflow.model.TalkSegment;
import com.woting.appengine.mobile.mediaflow.model.WholeTalk;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.appengine.mobile.push.model.CompareMsg;
import com.woting.appengine.mobile.push.model.Message;

public class DealMf extends Thread {
    private PushMemoryManage pmm=PushMemoryManage.getInstance();
    private GroupMemoryManage gmm=GroupMemoryManage.getInstance();

    /**
     * 给线程起一个名字的构造函数
     * @param name 线程名称
     */
    public DealMf(String name) {
        super("流数据处理线程"+((name==null||name.trim().length()==0)?"":"::"+name));
    }

    @Override
    public void run() {
        System.out.println(this.getName()+"开始执行");
        String tempStr="";
        while(true) {
            try {
                sleep(10);
                //读取Receive内存中的typeMsgMap中的内容
                Message m=pmm.getReceiveMemory().pollTypeQueue("AUDIOFLOW");
                if (m==null) continue;

                //暂存，解码
                if (m.getCmdType().equals("TALK")) {
                    if (m.getCommand().equals("1")) {//收到音频包
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-收到音频数据包::(User="+m.getFromAddr()+";TalkId="+((Map)m.getMsgContent()).get("TalkId")+")";
                        System.out.println(tempStr);
                        (new ReceiveAudioDatagram("{"+tempStr+"}处理线程", m)).start();
                    } else if (m.getCommand().equals("-b1")) {//收到回执包
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-收到音频回执包::(User="+m.getFromAddr()+";TalkId="+((Map)m.getMsgContent()).get("TalkId")+")";
                        System.out.println(tempStr);
                        (new ReceiveAudioAnswer("{"+tempStr+"}处理线程", m)).start();
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    //收到音频数据包
    class ReceiveAudioDatagram extends Thread {
        private Message sourceMsg;//源消息
        protected ReceiveAudioDatagram(String name, Message sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
            MobileKey mk=MobileUtils.getMobileKey(sourceMsg);
            if (!mk.isUser()) return;
            String talkerId=mk.getUserId();
            String talkId=((Map)sourceMsg.getMsgContent()).get("TalkId")+"";
            if (StringUtils.isEmptyOrWhitespaceOnly(talkId)) return;
            int seqNum=Integer.parseInt(((Map)sourceMsg.getMsgContent()).get("SeqNum")+"");
            if (seqNum<0) return;
            String groupId=((Map)sourceMsg.getMsgContent()).get("GroupId")+"";
            if (StringUtils.isEmptyOrWhitespaceOnly(groupId)) return;

            TalkMemoryManage tmm = TalkMemoryManage.getInstance();
            WholeTalk wt = tmm.getWholeTalk(mk);
            GroupInterCom gic=gmm.getGroupInterCom(groupId);
            if (!gic.getSpeaker().getUserId().equals(talkerId)) return;
            if (wt==null) {
                wt = new WholeTalk();
                wt.setTalkId(talkId);
                wt.setTalkerMk(mk);
                wt.setGroupId(groupId);
                wt.setMaxNum(seqNum);
            }
            TalkSegment ts = new TalkSegment();
            ts.setWt(wt);
            ts.setData((((Map)sourceMsg.getMsgContent()).get("TalkId")+"").getBytes());
            ts.setSendUsers(gic.getEntryGroupUserMap());
            ts.setSeqNum(seqNum);
            wt.addSegment(ts);
            //发送回执消息
            //发送广播消息，简单处理，只把这部分消息发给目的地
            Message bMsg=new Message();
            bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            bMsg.setMsgType(1);
            bMsg.setAffirm(1);
            bMsg.setMsgBizType("AUDIOFLOW");
            bMsg.setCmdType("TALK");
            bMsg.setCommand("b1");
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("TalkId", talkId);
            dataMap.put("GroupId", groupId);
            dataMap.put("SeqNum", seqNum);
            dataMap.put("AudioData", ((Map)sourceMsg.getMsgContent()).get("AudioData"));
            for (String k: ts.getSendUsers().keySet()) {
                String _sp[] = k.split("::");
                mk=new MobileKey();
                mk.setMobileId(_sp[0]);
                mk.setUserId(_sp[1]);
                bMsg.setToAddr(MobileUtils.getAddr(mk));
                pmm.getSendMemory().addUniqueMsg2Queue(mk, bMsg, new CompareGroupMsg());
                //处理流数据
                ts.getSendFlags().put(k, "1");
            }
        }
    }

    //收到音频回执包
    class ReceiveAudioAnswer extends Thread {
        private Message sourceMsg;//源消息
        protected ReceiveAudioAnswer(String name, Message sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
            MobileKey mk=MobileUtils.getMobileKey(sourceMsg);
            if (!mk.isUser()) return;
            String talkerId=mk.getUserId();
            String talkId=((Map)sourceMsg.getMsgContent()).get("TalkId")+"";
            if (StringUtils.isEmptyOrWhitespaceOnly(talkId)) return;
            int seqNum=Integer.parseInt(((Map)sourceMsg.getMsgContent()).get("SeqNum")+"");
            if (seqNum<0||seqNum!=-1) return;
            String groupId=((Map)sourceMsg.getMsgContent()).get("GroupId")+"";
            if (StringUtils.isEmptyOrWhitespaceOnly(groupId)) return;

            TalkMemoryManage tmm = TalkMemoryManage.getInstance();
            WholeTalk wt = tmm.getWholeTalk(mk);
            if (wt!=null) {
                if (sourceMsg.getReturnType().equals("1001")) {
                    TalkSegment ts = wt.getTalkData().get(seqNum);
                    String s = ts.getSendFlags().get(mk.toString());
                    if (s!=null) {
                        ts.getSendFlags().put(mk.toString(), "2");
                    }
                }
                if (seqNum==-1||wt.isCompleted()) {
                    tmm.removeWt(wt);
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