package com.woting.appengine.mobile.mediaflow.monitor;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.web.context.support.WebApplicationContextUtils;

import com.mysql.jdbc.StringUtils;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
//import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.calling.mem.CallingMemoryManage;
import com.woting.appengine.calling.model.OneCall;
//import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.intercom.model.GroupInterCom;
import com.woting.appengine.mobile.mediaflow.mem.TalkMemoryManage;
import com.woting.appengine.mobile.mediaflow.model.CompareAudioFlowMsg;
import com.woting.appengine.mobile.mediaflow.model.TalkSegment;
import com.woting.appengine.mobile.mediaflow.model.WholeTalk;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.push.core.message.Message;
import com.woting.push.core.message.MsgMedia;
import com.woting.passport.UGA.persis.pojo.UserPo;
import com.woting.passport.UGA.service.UserService;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.SessionService;

public class DealMediaflow extends Thread {
    private PushMemoryManage pmm=PushMemoryManage.getInstance();
    private GroupMemoryManage gmm=GroupMemoryManage.getInstance();
    private CallingMemoryManage cmm=CallingMemoryManage.getInstance();

    private SessionService sessionService=null;
    private UserService userService=null;

    /**
     * 给线程起一个名字的构造函数
     * @param name 线程名称
     */
    public DealMediaflow(String name) {
        super("流数据处理线程"+((name==null||name.trim().length()==0)?"":"::"+name));
        //创建SessionService对象
        ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
            sessionService=(SessionService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("redisSessionService");
            userService=(UserService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("userService");
        }
    }

    @Override
    public void run() {
        System.out.println(this.getName()+"开始执行");
        String tempStr="";
        while(true) {
            try {
                sleep(10);
                //读取Receive内存中的typeMsgMap中的内容
                Message m=pmm.getReceiveMemory().pollTypeQueue("media");
                if (m==null||!(m instanceof MsgMedia)) continue;

                MsgMedia mm=(MsgMedia)m;
                //暂存，解码
                if (!mm.isAck()) {//收到音频包
                    tempStr="处理消息[SeqId="+mm.getSeqNo()+"]-收到音频数据包::(User="+mm.getFromType()+";TalkId="+mm.getTalkId();
                    System.out.println(tempStr);
                    (new ReceiveAudioDatagram("{"+tempStr+"}处理线程", mm)).start();
                } else {//收到回执包
                    tempStr="处理消息[SeqId="+mm.getSeqNo()+"]-收到音频回执包::(User="+mm.getFromType()+";TalkId="+mm.getTalkId();
                    System.out.println(tempStr);
                    (new ReceiveAudioAnswer("{"+tempStr+"}处理线程", mm)).start();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    //收到音频数据包
    class ReceiveAudioDatagram extends Thread {
        private MsgMedia sourceMsg;//源消息
        protected ReceiveAudioDatagram(String name, MsgMedia sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
            MobileUDKey mUdk=(MobileUDKey)sourceMsg.getExtInfo();
            if (mUdk==null||!mUdk.isUser()) return;
            String talkerId=mUdk.getUserId();
            String talkId=sourceMsg.getTalkId();
            if (StringUtils.isEmptyOrWhitespaceOnly(talkId)) return;
            int seqNum=sourceMsg.getSeqNo();
            String objId=sourceMsg.getObjId();
            if (StringUtils.isEmptyOrWhitespaceOnly(objId)) return;

            int talkType=sourceMsg.getBizType();

            TalkMemoryManage tmm=TalkMemoryManage.getInstance();
//            Map<String, Object> dataMap=new HashMap<String, Object>();
            //组织回执消息
//            Message retMsg=new Message();
//            retMsg.setFromAddr("{(audioflow)@@(www.woting.fm||S)}");
//            retMsg.setToAddr(MobileUtils.getAddr(mk));
//            retMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
//            retMsg.setReMsgId(sourceMsg.getMsgId());
//            retMsg.setMsgType(-1);
//            retMsg.setAffirm(1);
//            retMsg.setMsgBizType("AUDIOFLOW");
//            retMsg.setCmdType(sourceMsg.getCmdType());
//            retMsg.setCommand("-1");
//            dataMap.put("TalkId", talkId);
//            dataMap.put("ObjId", objId);
//            dataMap.put("SeqNum", seqNum+"");
//            retMsg.setMsgContent(dataMap);

            GroupInterCom gic=null;
            OneCall oc=null;
            if (talkType==1) {//组对讲
                gic=gmm.getGroupInterCom(objId);
                if (gic==null||gic.getSpeaker()==null||!gic.getSpeaker().getUserId().equals(talkerId)) {
//                    if (gic==null||gic.getSpeaker()==null||!gic.getSpeaker().getUserId().equals(talkerId)) {
//                    retMsg.setReturnType("1002");
//                    pmm.getSendMemory().addUniqueMsg2Queue(mk, retMsg, new CompareAudioFlowMsg());
                    return;
                }
            } else {//电话
                oc=cmm.getCallData(objId);
                if (oc==null) {
//                    retMsg.setReturnType("1003");
//                    pmm.getSendMemory().addUniqueMsg2Queue(mk, retMsg, new CompareAudioFlowMsg());
                    return;
                }
            }
            WholeTalk wt=null;
            synchronized (tmm) {
                wt=tmm.getWholeTalk(talkId);
                if (wt==null) {
                    wt=new WholeTalk();
                    wt.setTalkId(talkId);
                    wt.setTalkerMk(mUdk);
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
            TalkSegment ts=new TalkSegment();
            ts.setWt(wt);
            ts.setData(sourceMsg.getMediaData());
            if (talkType==1) ts.setSendUserMap(gic.getEntryGroupUserMap());//组对讲
            else {//电话
                UserPo u=null; 
                MobileUDKey otherUdk=oc.getOtherUdk(talkerId);
                otherUdk=(MobileUDKey)sessionService.getActivedUserUDK(otherUdk.getUserId(), otherUdk.getPCDType());
                if (mUdk!=null) u=userService.getUserById(otherUdk.getUserId());
                if (u!=null) {
                    Map<String, UserPo> um=new HashMap<String, UserPo>();
                    um.put(mUdk.toString(), u);
                    ts.setSendUserMap(um);
                }
            }
            ts.setSeqNum(seqNum);
            wt.addSegment(ts);
            if (talkType==1) gic.setLastTalkTime(talkerId);
            else oc.setLastUsedTime();

            //发送正常回执
//            retMsg.setReturnType("1001");
//            pmm.getSendMemory().addUniqueMsg2Queue(mk, retMsg, new CompareAudioFlowMsg());

//            if (new String(ts.getData()).equals("####")) System.out.println("deCode:::====="+new String(ts.getData()));
            //发送广播消息，简单处理，只把这部分消息发给目的地，是声音数据文件
            MsgMedia bMsg=new MsgMedia();
            bMsg.setFromType(1);
            bMsg.setToType(0);
            bMsg.setMsgType(0);
            bMsg.setAffirm(1);
            bMsg.setBizType(sourceMsg.getBizType());
            bMsg.setTalkId(talkId);
            bMsg.setObjId(objId);
            bMsg.setSeqNo(seqNum);
            bMsg.setMediaData(sourceMsg.getMediaData());

            for (String k: ts.getSendUserMap().keySet()) {
                String _sp[]=k.split("::");
                mUdk=new MobileUDKey();
                mUdk.setDeviceId(_sp[0]);
                mUdk.setPCDType(Integer.parseInt(_sp[1]));
                mUdk.setUserId(_sp[2]);
                pmm.getSendMemory().addUniqueMsg2Queue(mUdk, bMsg, new CompareAudioFlowMsg());
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
        private MsgMedia sourceMsg;//源消息
        protected ReceiveAudioAnswer(String name, MsgMedia sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
            MobileUDKey mUdk=(MobileUDKey)sourceMsg.getExtInfo();
            if (mUdk==null||!mUdk.isUser()) return;
            String talkerId=mUdk.getUserId();
            String talkId=sourceMsg.getTalkId();
            if (StringUtils.isEmptyOrWhitespaceOnly(talkerId)) return;
            int seqNum=sourceMsg.getSeqNo();
            if (seqNum<0) return;
            String groupId=sourceMsg.getObjId();
            if (StringUtils.isEmptyOrWhitespaceOnly(groupId)) return;

            int talkType=sourceMsg.getBizType();

            TalkMemoryManage tmm=TalkMemoryManage.getInstance();
            WholeTalk wt=tmm.getWholeTalk(talkId);
            if (wt!=null) {
                if (sourceMsg.getReturnType()==1) {
                    TalkSegment ts=wt.getTalkData().get(Math.abs(seqNum));
                    if (ts!=null&&ts.getSendFlagMap().get(mUdk.toString())!=null) ts.getSendFlagMap().put(mUdk.toString(), 2);
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