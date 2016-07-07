package com.woting.appengine.mobile.mediaflow.monitor;

import java.util.HashMap;
import java.util.Map;

import com.mysql.jdbc.StringUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.calling.mem.CallingMemoryManage;
import com.woting.appengine.calling.model.OneCall;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.intercom.model.GroupInterCom;
import com.woting.appengine.mobile.mediaflow.mem.TalkMemoryManage;
import com.woting.appengine.mobile.mediaflow.model.CompareAudioFlowMsg;
import com.woting.appengine.mobile.mediaflow.model.TalkSegment;
import com.woting.appengine.mobile.mediaflow.model.WholeTalk;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.appengine.mobile.push.model.Message;
import com.woting.appengine.mobile.session.mem.SessionMemoryManage;
import com.woting.appengine.mobile.session.model.MobileSession;
import com.woting.passport.UGA.persistence.pojo.UserPo;

public class DealMediaflow extends Thread {
    private PushMemoryManage pmm=PushMemoryManage.getInstance();
    private GroupMemoryManage gmm=GroupMemoryManage.getInstance();
    private CallingMemoryManage cmm=CallingMemoryManage.getInstance();
    private SessionMemoryManage smm=SessionMemoryManage.getInstance();

    /**
     * 给线程起一个名字的构造函数
     * @param name 线程名称
     */
    public DealMediaflow(String name) {
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
                if (m.getCommand().equals("1")) {//收到音频包
                    tempStr="处理消息[MsgId="+m.getMsgId()+"]-收到音频数据包::(User="+m.getFromAddr()+";TalkId="+((Map)m.getMsgContent()).get("TalkId")+")";
                    System.out.println(tempStr);
                    (new ReceiveAudioDatagram("{"+tempStr+"}处理线程", m)).start();
                } else if (m.getCommand().equals("-b1")) {//收到回执包
                    tempStr="处理消息[MsgId="+m.getMsgId()+"]-收到音频回执包::(User="+m.getFromAddr()+";TalkId="+((Map)m.getMsgContent()).get("TalkId")+")";
                    System.out.println(tempStr);
                    (new ReceiveAudioAnswer("{"+tempStr+"}处理线程", m)).start();
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
            MobileKey mk=MobileUtils.getMobileKey(sourceMsg,1);
            if (!mk.isUser()) return;
            String talkerId=mk.getUserId();
            String talkId=((Map)sourceMsg.getMsgContent()).get("TalkId")+"";
            if (StringUtils.isEmptyOrWhitespaceOnly(talkId)) return;
            int seqNum=Integer.parseInt(((Map)sourceMsg.getMsgContent()).get("SeqNum")+"");
            String objId=((Map)sourceMsg.getMsgContent()).get("ObjId")+"";
            if (StringUtils.isEmptyOrWhitespaceOnly(objId)) return;

            int talkType=1;//组对讲
            if (sourceMsg.getCmdType().equals("TALK_TELPHONE")) talkType=2;//电话

            TalkMemoryManage tmm = TalkMemoryManage.getInstance();
            Map<String, Object> dataMap=new HashMap<String, Object>();
            //组织回执消息
            Message retMsg=new Message();
            retMsg.setFromAddr("{(audioflow)@@(www.woting.fm||S)}");
            retMsg.setToAddr(MobileUtils.getAddr(mk));
            retMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            retMsg.setReMsgId(sourceMsg.getMsgId());
            retMsg.setMsgType(-1);
            retMsg.setAffirm(1);
            retMsg.setMsgBizType("AUDIOFLOW");
            retMsg.setCmdType(sourceMsg.getCmdType());
            retMsg.setCommand("-1");
            dataMap.put("TalkId", talkId);
            dataMap.put("ObjId", objId);
            dataMap.put("SeqNum", seqNum+"");
            retMsg.setMsgContent(dataMap);

            GroupInterCom gic=null;
            OneCall oc=null;
            if (talkType==1) {//组对讲
                gic=gmm.getGroupInterCom(objId);
                if (gic==null||gic.getSpeaker()==null||!gic.getSpeaker().getUserId().equals(talkerId)) {
                    retMsg.setReturnType("1002");
                    pmm.getSendMemory().addUniqueMsg2Queue(mk, retMsg, new CompareAudioFlowMsg());
                    return;
                }
            } else {//电话
                oc=cmm.getCallData(objId);
                if (oc==null) {
                    retMsg.setReturnType("1003");
                    pmm.getSendMemory().addUniqueMsg2Queue(mk, retMsg, new CompareAudioFlowMsg());
                    return;
                }
            }
            WholeTalk wt=null;
            synchronized (tmm) {
                wt=tmm.getWholeTalk(talkId);
                if (wt==null) {
                    wt = new WholeTalk();
                    wt.setTalkId(talkId);
                    wt.setTalkerMk(mk);
                    wt.setObjId(objId);
                    wt.setTalkType(talkType);
                    tmm.addWt(wt);
                    wt.startMonitor(wt);
                    //加入电话控制中
                    if (talkType==2) {
                        if (talkerId.equals(oc.getCallerId())) oc.addCallerWt(wt);//呼叫者
                        else
                        if (talkerId.equals(oc.getCallederId())) oc.addCallederWt(wt);//被叫者
                    }
                }
            }
            TalkSegment ts = new TalkSegment();
            ts.setWt(wt);
            ts.setData((((Map)sourceMsg.getMsgContent()).get("AudioData")+"").getBytes());
            if (talkType==1) ts.setSendUserMap(gic.getEntryGroupUserMap());//组对讲
            else {//电话
                String userId=oc.getOtherId(talkerId);
                MobileSession ms=smm.getActivedUserSessionByUserId(userId);
                UserPo u=null;
                if (ms!=null) {
                    u=(UserPo)smm.getActivedUserSessionByUserId(userId).getAttribute("user");
                }
                if (u!=null) {
                    Map<String, UserPo> um=new HashMap<String, UserPo>();
                    um.put(ms.getKey().toString(), u);
                    ts.setSendUserMap(um);
                }
            }
            ts.setSeqNum(seqNum);
            wt.addSegment(ts);
            if (talkType==1) gic.setLastTalkTime(talkerId);
            else oc.setLastUsedTime();

            //发送正常回执
            retMsg.setReturnType("1001");
            pmm.getSendMemory().addUniqueMsg2Queue(mk, retMsg, new CompareAudioFlowMsg());

//            if (new String(ts.getData()).equals("####")) System.out.println("deCode:::====="+new String(ts.getData()));
            //发送广播消息，简单处理，只把这部分消息发给目的地，是声音数据文件
            Message bMsg=new Message();
            bMsg.setFromAddr("{(audioflow)@@(www.woting.fm||S)}");
            bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            bMsg.setMsgType(1);
            bMsg.setAffirm(1);
            bMsg.setMsgBizType("AUDIOFLOW");
            bMsg.setCmdType(sourceMsg.getCmdType());
            bMsg.setCommand("b1");
            dataMap=new HashMap<String, Object>();
            dataMap.put("TalkId", talkId);
            dataMap.put("ObjId", objId);
            dataMap.put("SeqNum", seqNum+"");
            dataMap.put("AudioData", ((Map)sourceMsg.getMsgContent()).get("AudioData"));
            bMsg.setMsgContent(dataMap);
            for (String k: ts.getSendUserMap().keySet()) {
                String _sp[] = k.split("::");
                mk=new MobileKey();
                mk.setMobileId(_sp[0]);
                mk.setPCDType(Integer.parseInt(_sp[1]));
                mk.setUserId(_sp[2]);
                bMsg.setToAddr(MobileUtils.getAddr(mk));
                pmm.getSendMemory().addUniqueMsg2Queue(mk, bMsg, new CompareAudioFlowMsg());
                //处理流数据
                ts.getSendFlagMap().put(k, 0);
                ts.getSendTimeMap().get(k).add(System.currentTimeMillis());
            }

            //看是否是结束包
            if (wt.isReceiveCompleted()) {
                if (talkType==1) {
                    gic.sendEndPTT();
                    gic.delSpeaker(talkerId);
                }
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
            MobileKey mk=MobileUtils.getMobileKey(sourceMsg,1);
            if (!mk.isUser()) return;
            String talkerId=mk.getUserId();
            String talkId=((Map)sourceMsg.getMsgContent()).get("TalkId")+"";
            if (StringUtils.isEmptyOrWhitespaceOnly(talkerId)) return;
            int seqNum=Integer.parseInt(((Map)sourceMsg.getMsgContent()).get("SeqNum")+"");
            if (seqNum<0) return;
            String groupId=((Map)sourceMsg.getMsgContent()).get("GroupId")+"";
            if (StringUtils.isEmptyOrWhitespaceOnly(groupId)) return;

            int talkType=1;//组对讲
            if (sourceMsg.getCmdType().equals("TALK_TELPHONE")) talkType=2;//电话

            TalkMemoryManage tmm = TalkMemoryManage.getInstance();
            WholeTalk wt = tmm.getWholeTalk(talkId);
            if (wt!=null) {
                if (sourceMsg.getReturnType().equals("1001")) {
                    TalkSegment ts = wt.getTalkData().get(Math.abs(seqNum));
                    if (ts!=null&&ts.getSendFlagMap().get(mk.toString())!=null) ts.getSendFlagMap().put(mk.toString(), 2);
                }
                if (wt.isSendCompleted()) {
                    tmm.removeWt(wt);
                    //发送结束对讲消息
                    if (talkType==1)  {
                        GroupInterCom gic=gmm.getGroupInterCom(groupId);
                        if (gic!=null&&gic.getSpeaker()!=null) {
                            gic.sendEndPTT();
                            gic.delSpeaker(talkerId);
                        }
                    }
                }
            }
        }
    }
}