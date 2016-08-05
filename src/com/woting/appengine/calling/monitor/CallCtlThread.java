package com.woting.appengine.calling.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.calling.mem.CallingMemoryManage;
import com.woting.appengine.calling.model.OneCall;
import com.woting.appengine.calling.model.ProcessedMsg;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.mobile.mediaflow.mem.TalkMemoryManage;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.push.core.message.Message;
import com.woting.push.core.message.MsgMedia;
import com.woting.push.core.message.MsgNormal;
import com.woting.push.core.message.content.MapContent;
import com.woting.appengine.mobile.session.mem.SessionMemoryManage;
import com.woting.passport.UGA.persistence.pojo.UserPo;

/**
 * 电话控制线程
 * @author wanghui
 */
public class CallCtlThread extends Thread {
    private CallingMemoryManage cmm=CallingMemoryManage.getInstance();
    private PushMemoryManage pmm=PushMemoryManage.getInstance();
    private SessionMemoryManage smm=SessionMemoryManage.getInstance();
    private GroupMemoryManage gmm=GroupMemoryManage.getInstance();

    private OneCall callData;//所控制的通话数据
    private boolean isCallerTalked=false; //是否“被叫者”说话

    private String callerAddr="";
    private MobileKey callerKey=null;
    private String callederAddr="";
    private MobileKey callederKey=null;

    /**
     * 构造函数，必须给定一个通话控制数据
     * @param callData
     */
    public CallCtlThread(OneCall callData) {
        super();
        super.setName("电话控制线程[callId="+callData.getCallId()+"]");
        this.callData=callData;
        this.isCallerTalked=false;
    }

    /**
     * 控制过程
     * @see java.lang.Thread#run()
     */
    public void run() {
        while (true) {
            try {
                sleep(50);//等50毫秒
                if (callData.getStatus()==9) {//结束进程
                    cleanData();
                    break;
                }
                if (callData.getStatus()==4) {//已经挂断了 //清除声音内容
                    cleanTalk(1);
                }

                //一段时间后未收到自动回复，的处理
                if (this.callData.getStatus()==1
                  &&this.callData.getBeginDialTime()!=-1
                  &&(System.currentTimeMillis()-this.callData.getBeginDialTime()>this.callData.getIt1_expire()))
                {
                    dealOutLine();
                }
                //一段时间后未收到“被叫者”手工应答Ack，的处理
                if ((this.callData.getStatus()==1||this.callData.getStatus()==2)
                  &&this.callData.getBeginDialTime()!=-1
                  &&(System.currentTimeMillis()-this.callData.getBeginDialTime()>this.callData.getIt2_expire()))
                {
                    dealNoAck();
                }
                //一段时间后未收到任何消息，通话过期
                if ((this.callData.getStatus()==1||this.callData.getStatus()==2||this.callData.getStatus()==3)
                  &&(System.currentTimeMillis()-this.callData.getLastUsedTime()>this.callData.getIt3_expire()))
                {
                    dealCallExpire();
                }

                //“被叫者”第一次说话
                if (!isCallerTalked&&this.callData.getCallederWts().size()>0) {
                    dealCallerFirstTalk();
                    isCallerTalked=true;
                }
                //读取预处理的消息
                MsgNormal m=this.callData.pollPreMsg();//第一条必然是呼叫信息
                if (m==null) continue;

                this.callData.setLastUsedTime();
                int flag=1;
                ProcessedMsg pMsg=new ProcessedMsg(m, System.currentTimeMillis(), this.getClass().getName());
                try {
                    if (m.getCmdType()==1) {
                        if (m.getCommand()==1) dial(m);//呼叫处理
                        else
                        if (m.getCommand()==0x90) flag=dealAutoDialFeedback(m); //“被叫者”的自动反馈
                        else
                        if (m.getCommand()==2) flag=ackDial(m); //“被叫者”的手工应答
                        else
                        if (m.getCommand()==3) hangup(m); //挂断通话
                    }
                    if (m.getCmdType()==2) {
                        if (m.getCommand()==1) beginPTT(m);//开始对讲
                        else
                        if (m.getCommand()==2) endPTT(m); //结束对讲
                    }
                    pMsg.setStatus(flag);
                } catch(Exception e) {
                    pMsg.setStatus(-1);
                } finally {
                    pMsg.setEndTime(System.currentTimeMillis());
                    this.callData.addProcessedMsg(pMsg);
                }
            } catch(Exception e) {
                e.printStackTrace();
                cleanTalk(2);//强制清除声音
            }
        }
    }

    //===========以下是分步处理过程，全部是私有函数
    //处理呼叫(CALL:1)
    private void dial(MsgNormal m) {
        System.out.println("处理呼叫信息前==[callid="+this.callData.getCallId()+"]:status="+this.callData.getStatus());
        String callId =((Map)m.getMsgContent()).get("CallId")+"";
        String callerId=MobileUtils.getMobileKey(m).getUserId();
        String callederId=((Map)m.getMsgContent()).get("CallederId")+"";

        Map<String, Object> dataMap=null;
        boolean isBusy=true; //是否占线
        boolean callorExisted=false; //是否被叫者存在

        //返回给呼叫者的消息
        MsgNormal toCallerMsg=new MsgNormal();
        toCallerMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        toCallerMsg.setReMsgId(m.getMsgId());
        toCallerMsg.setToType(m.getFromType());
        toCallerMsg.setFromType(m.getToType());
        toCallerMsg.setMsgType(0);
        toCallerMsg.setBizType(0x02);
        toCallerMsg.setCmdType(1);
        toCallerMsg.setCommand(0x09);
        //判断被叫者是否存在
        if (smm.getActivedUserSessionByUserId(callerId)==null) toCallerMsg.setReturnType(2);
        else
        if (smm.getActivedUserSessionByUserId(callederId)==null) toCallerMsg.setReturnType(3);
        else callorExisted=true;
        //判断是否占线
        if (callorExisted) {
            if (callerId.equals(callederId))  toCallerMsg.setReturnType(6);
            else
            if (gmm.isTalk(callederId)) toCallerMsg.setReturnType(5);
            else
            if (cmm.isTalk(callederId, callId)) toCallerMsg.setReturnType(4);
            else isBusy=false;
        }
        if (callorExisted&&!isBusy) toCallerMsg.setReturnType(1);
        dataMap=new HashMap<String, Object>();
        dataMap.put("CallId", callId);
        dataMap.put("CallerId", callerId);
        dataMap.put("CallederId", callederId);
        MapContent mc=new MapContent(dataMap);
        toCallerMsg.setMsgContent(mc);
        pmm.getSendMemory().addMsg2Queue(MobileUtils.getMobileKey(m), toCallerMsg);
        //记录到已发送列表
        this.callData.addSendedMsg(toCallerMsg);

        //给被叫者发送信息
        if (callorExisted&&!callerId.equals(callederId)) {
            MsgNormal toCallederMsg=new MsgNormal();
            toCallederMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            toCallederMsg.setMsgType(0);
            toCallederMsg.setAffirm(1);
            toCallederMsg.setBizType(2);
            toCallederMsg.setCmdType(1);
            toCallederMsg.setCommand(0x10);
            dataMap=new HashMap<String, Object>();
            dataMap.put("DialType", isBusy?"2":"1");
            dataMap.put("CallId", callId);
            dataMap.put("CallerId", callerId);
            dataMap.put("CallederId", callederId);
            MapContent _mc=new MapContent(dataMap);
            toCallederMsg.setMsgContent(_mc);
            //加入“呼叫者”的用户信息给被叫者
            Map<String, Object> callerInfo=new HashMap<String, Object>();
            UserPo u=(UserPo)smm.getActivedUserSessionByUserId(callerId).getAttribute("user");
            callerInfo.put("UserName", u.getLoginName());
            callerInfo.put("UserNum", u.getUserNum());
            callerInfo.put("Portrait", u.getPortraitMini());
            callerInfo.put("Mail", u.getMailAddress());
            callerInfo.put("Descn", u.getDescn());
            dataMap.put("CallerInfo", callerInfo);

            pmm.getSendMemory().addMsg2Queue(smm.getActivedUserSessionByUserId(callederId).getKey(), toCallederMsg);
            //记录到已发送列表
            this.callData.addSendedMsg(toCallederMsg);
            //开始计时，两个过程
            this.callData.setBeginDialTime();
        }

        //若不存在用户或占线要删除数据及这个过程
        if (!callorExisted||isBusy) shutdown();
        else {
            //修改状态
            this.callData.setStatus_1();
            this.callerKey=smm.getActivedUserSessionByUserId(callerId).getKey();
            this.callerAddr=MobileUtils.getAddr(callerKey);
            this.callederKey=smm.getActivedUserSessionByUserId(callederId).getKey();
            this.callederAddr=MobileUtils.getAddr(callederKey);
        }
        System.out.println("处理呼叫信息后==[callid="+this.callData.getCallId()+"]:status="+this.callData.getStatus());
    }

    //处理“被叫者”的自动呼叫反馈(CALL:-b1)
    private int dealAutoDialFeedback(MsgNormal m) {
        System.out.println("处理自动应答前==[callid="+this.callData.getCallId()+"]:status="+this.callData.getStatus());
        //首先判断这个消息是否符合处理的要求：callid, callerId, callederId是否匹配
        if (!this.callData.getCallerId().equals(((Map)m.getMsgContent()).get("CallerId")+"")||!this.callData.getCallederId().equals(MobileUtils.getMobileKey(m).getUserId())) return 3;
        if (this.callData.getStatus()==1) {//状态正确，如果是其他状态，这个消息抛弃
            //发送给呼叫者的消息
            MsgNormal toCallerMsg=new MsgNormal();
            toCallerMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            toCallerMsg.setFromType(1);
            toCallerMsg.setToType(0);
            toCallerMsg.setMsgType(0);
            toCallerMsg.setAffirm(1);
            toCallerMsg.setBizType(2);
            toCallerMsg.setCmdType(1);
            toCallerMsg.setCommand(0x40);
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("CallId", this.callData.getCallId());
            dataMap.put("CallerId", this.callData.getCallerId());
            dataMap.put("CallederId", this.callData.getCallederId());
            dataMap.put("OnLineType", "1");
            MapContent mc=new MapContent(dataMap);
            toCallerMsg.setMsgContent(mc);

            pmm.getSendMemory().addMsg2Queue(this.callerKey, toCallerMsg);
            this.callData.addSendedMsg(toCallerMsg);//记录到已发送列表

            this.callData.setStatus_2();//修改状态
            System.out.println("处理自动应答后==[callid="+this.callData.getCallId()+"]:status="+this.callData.getStatus());
            return 1;
        } else return 2;//被抛弃
    }

    //处理“被叫者”应答(CALL:2)
    private int ackDial(MsgNormal m) {
        //首先判断这个消息是否符合处理的要求：callid, callerId, callederId是否匹配
        if (!this.callData.getCallerId().equals(((Map)m.getMsgContent()).get("CallerId")+"")||!this.callData.getCallederId().equals(MobileUtils.getMobileKey(m).getUserId())) return 3;
        if (this.callData.getStatus()==1||this.callData.getStatus()==2) {//状态正确，如果是其他状态，这个消息抛弃
            //应答状态
            int ackType=2; //拒绝
            try {
                ackType=Integer.parseInt(""+((Map)m.getMsgContent()).get("ACKType"));
            } catch(Exception e) {}
            //构造“应答传递ACK”消息，并发送给“呼叫者”
            MsgNormal toCallerMsg=new MsgNormal();
            toCallerMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            toCallerMsg.setFromType(1);
            toCallerMsg.setToType(0);
            toCallerMsg.setMsgType(0);
            toCallerMsg.setAffirm(1);
            toCallerMsg.setBizType(2);
            toCallerMsg.setCmdType(1);
            toCallerMsg.setCommand(0x20);
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("CallId", this.callData.getCallId());
            dataMap.put("CallerId", this.callData.getCallerId());
            dataMap.put("CallederId", this.callData.getCallederId());
            dataMap.put("ACKType", ackType);
            MapContent mc=new MapContent(dataMap);
            toCallerMsg.setMsgContent(mc);

            pmm.getSendMemory().addMsg2Queue(this.callerKey, toCallerMsg);
            this.callData.addSendedMsg(toCallerMsg);//记录到已发送列表

            if (ackType==1) this.callData.setStatus_3();//修改状态:正常通话
            else
            if (ackType==2||ackType==31) this.callData.setStatus_4();//修改状态:挂断
            System.out.println("接到被叫者手工应答后==[callid="+this.callData.getCallId()+"]:status="+this.callData.getStatus());
            return 1;
        } else return 2;//被抛弃
    }

    //处理“挂断”(CALL:3)
    private int hangup(MsgNormal m) {
        //首先判断是那方在进行挂断
        String hangupperId=MobileUtils.getMobileKey(m).getUserId();
        String otherId=this.callData.getOtherId(hangupperId);
        if (otherId==null) return 3;
        //给另一方发送“挂断传递”消息
        MsgNormal otherMsg=new MsgNormal();
        otherMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        otherMsg.setFromType(1);
        otherMsg.setToType(0);
        otherMsg.setMsgType(1);
        otherMsg.setAffirm(0);
        otherMsg.setBizType(2);
        otherMsg.setCmdType(1);
        otherMsg.setCommand(0x30);
        Map<String, Object> dataMap=new HashMap<String, Object>();
        dataMap.put("CallId", this.callData.getCallId());
        dataMap.put("CallerId", this.callData.getCallerId());
        dataMap.put("CallederId", this.callData.getCallederId());
        dataMap.put("HangupType", "1");
        MapContent mc=new MapContent(dataMap);
        otherMsg.setMsgContent(mc);

        pmm.getSendMemory().addMsg2Queue(smm.getActivedUserSessionByUserId(otherId).getKey(), otherMsg);
        this.callData.addSendedMsg(otherMsg);//记录到已发送列表
        
        this.callData.setStatus_4();//修改状态
        System.out.println("处理挂断消息后==[callid="+this.callData.getCallId()+"]:status="+this.callData.getStatus());
        return 1;
    }

    //处理开始对讲(PTT:1)
    private void beginPTT(MsgNormal m) {
        MsgNormal toSpeakerMsg=new MsgNormal();
        toSpeakerMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        toSpeakerMsg.setFromType(1);
        toSpeakerMsg.setToType(0);
        toSpeakerMsg.setReMsgId(m.getMsgId());
        toSpeakerMsg.setMsgType(0);
        toSpeakerMsg.setAffirm(1);
        toSpeakerMsg.setBizType(2);
        toSpeakerMsg.setCmdType(2);
        toSpeakerMsg.setCommand(9);

        Map<String, Object> dataMap=new HashMap<String, Object>();
        String speaker=MobileUtils.getMobileKey(m).getUserId();
        if (StringUtils.isNullOrEmptyOrSpace(speaker)||smm.getActivedUserSessionByUserId(speaker)==null) {
            toSpeakerMsg.setReturnType(0);
        } else {
            String ret=this.callData.setSpeaker(speaker);
            if (ret.equals("1")) {
                toSpeakerMsg.setReturnType(1);
            } else if (ret.equals("0")) {
                toSpeakerMsg.setReturnType(3);
                dataMap.put("ErrMsg", "当前会话为非对讲模式，不用申请独占的通话资源");
            } else if (ret.startsWith("2::")) {
                toSpeakerMsg.setReturnType(2);
                dataMap.put("Speaker", ret.substring(3));
            } else {
                dataMap.put("ErrMsg", ret.startsWith("-")?ret.substring(ret.indexOf("::")+2):"未知问题");
            }

        }
        dataMap.put("CallId", this.callData.getCallId());
        MapContent mc=new MapContent(dataMap);
        toSpeakerMsg.setMsgContent(mc);
        pmm.getSendMemory().addMsg2Queue(MobileUtils.getMobileKey(m), toSpeakerMsg);
        //记录到已发送列表
        this.callData.addSendedMsg(toSpeakerMsg);
    }

    //处理结束对讲(PTT:2)
    private void endPTT(MsgNormal m) {
        MsgNormal toSpeakerMsg=new MsgNormal();
        toSpeakerMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        toSpeakerMsg.setFromType(1);
        toSpeakerMsg.setToType(0);
        toSpeakerMsg.setReMsgId(m.getMsgId());
        toSpeakerMsg.setMsgType(0);
        toSpeakerMsg.setAffirm(1);
        toSpeakerMsg.setBizType(2);
        toSpeakerMsg.setCmdType(2);
        toSpeakerMsg.setCommand(0x0A);

        Map<String, Object> dataMap=new HashMap<String, Object>();
        String speaker=MobileUtils.getMobileKey(m).getUserId();
        if (StringUtils.isNullOrEmptyOrSpace(speaker)||smm.getActivedUserSessionByUserId(speaker)==null) {
            toSpeakerMsg.setReturnType(0);
        } else {
            String ret=this.callData.cleanSpeaker(speaker);
            if (ret.equals("1")) {
                toSpeakerMsg.setReturnType(1);
            } else if (ret.equals("0")) {
                toSpeakerMsg.setReturnType(3);
                dataMap.put("ErrMsg", "当前会话为非对讲模式，不用申请独占的通话资源");
            } else if (ret.startsWith("2::")) {
                toSpeakerMsg.setReturnType(2);
                dataMap.put("Speaker", ret.substring(3));
            } else {
                dataMap.put("ErrMsg", ret.startsWith("-")?ret.substring(ret.indexOf("::")+2):"未知问题");
            }

        }
        dataMap.put("CallId", this.callData.getCallId());
        MapContent mc=new MapContent(dataMap);
        toSpeakerMsg.setMsgContent(mc);
        pmm.getSendMemory().addMsg2Queue(MobileUtils.getMobileKey(m), toSpeakerMsg);
        //记录到已发送列表
        this.callData.addSendedMsg(toSpeakerMsg);
    }

    //=======以下3个超时处理
    //处理“被叫者”不在线
    private void dealOutLine() {
        //发送给“呼叫者”的消息
        MsgNormal toCallerMsg=new MsgNormal();
        toCallerMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        toCallerMsg.setFromType(1);
        toCallerMsg.setToType(0);
        toCallerMsg.setMsgType(0);
        toCallerMsg.setAffirm(1);
        toCallerMsg.setBizType(2);
        toCallerMsg.setCmdType(1);
        toCallerMsg.setCommand(0x40);
        Map<String, Object> dataMap=new HashMap<String, Object>();
        dataMap.put("CallId", this.callData.getCallId());
        dataMap.put("CallerId", this.callData.getCallerId());
        dataMap.put("CallederId", this.callData.getCallederId());
        dataMap.put("OnLineType", "2");
        MapContent mc=new MapContent(dataMap);
        toCallerMsg.setMsgContent(mc);

        pmm.getSendMemory().addMsg2Queue(this.callerKey, toCallerMsg);
        this.callData.addSendedMsg(toCallerMsg);//记录到已发送列表

        shutdown();
        System.out.println("被叫者不在线检测到后==[callid="+this.callData.getCallId()+"]:status="+this.callData.getStatus());
    }

    //处理“被叫者”未手工应答
    private void dealNoAck() {
        //1、构造“应答传递ACK”消息，并发送给“呼叫者”
        MsgNormal toCallerMsg=new MsgNormal();
        toCallerMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        toCallerMsg.setFromType(1);
        toCallerMsg.setToType(0);
        toCallerMsg.setMsgType(0);
        toCallerMsg.setAffirm(1);
        toCallerMsg.setBizType(2);
        toCallerMsg.setCmdType(1);
        toCallerMsg.setCommand(0x20);
        Map<String, Object> dataMap=new HashMap<String, Object>();
        dataMap.put("CallId", this.callData.getCallId());
        dataMap.put("CallerId", this.callData.getCallerId());
        dataMap.put("CallederId", this.callData.getCallederId());
        dataMap.put("ACKType", "32");
        MapContent mc=new MapContent(dataMap);
        toCallerMsg.setMsgContent(mc);
        pmm.getSendMemory().addMsg2Queue(this.callerKey, toCallerMsg);
        this.callData.addSendedMsg(toCallerMsg);//记录到已发送列表

        //2、构造“挂断传递”消息，并发送给“被叫者”
        MsgNormal toCallederMsg=new MsgNormal();
        toCallederMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        toCallerMsg.setFromType(1);
        toCallerMsg.setToType(0);
        toCallederMsg.setMsgType(0);
        toCallederMsg.setAffirm(1);
        toCallederMsg.setBizType(2);
        toCallederMsg.setCmdType(1);
        toCallederMsg.setCommand(0x30);
        dataMap=new HashMap<String, Object>();
        dataMap.put("CallId", this.callData.getCallId());
        dataMap.put("CallerId", this.callData.getCallerId());
        dataMap.put("CallederId", this.callData.getCallederId());
        dataMap.put("HangupType", "2");
        MapContent _mc=new MapContent(dataMap);
        toCallederMsg.setMsgContent(_mc);
        pmm.getSendMemory().addMsg2Queue(this.callederKey, toCallederMsg);
        this.callData.addSendedMsg(toCallederMsg);//记录到已发送列表

        shutdown();
        System.out.println("未手工应答后==[callid="+this.callData.getCallId()+"]:status="+this.callData.getStatus());
    }

    //服务器发现电话过程过期
    private void dealCallExpire() {
        //发送给“呼叫者”的消息
        MsgNormal toCallerMsg=new MsgNormal();
        toCallerMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        toCallerMsg.setFromType(1);
        toCallerMsg.setToType(0);
        toCallerMsg.setMsgType(0);
        toCallerMsg.setAffirm(1);
        toCallerMsg.setBizType(2);
        toCallerMsg.setCmdType(1);
        toCallerMsg.setCommand(0x30);
        Map<String, Object> callerMap=new HashMap<String, Object>();
        callerMap.put("CallId", this.callData.getCallId());
        callerMap.put("CallerId", this.callData.getCallerId());
        callerMap.put("CallederId", this.callData.getCallederId());
        callerMap.put("HangupType", "3");
        MapContent mc=new MapContent(callerMap);
        toCallerMsg.setMsgContent(mc);
        pmm.getSendMemory().addMsg2Queue(this.callerKey, toCallerMsg);
        this.callData.addSendedMsg(toCallerMsg);//记录到已发送列表

        //发送给“被叫者”的消息
        MsgNormal toCallederMsg=new MsgNormal();
        toCallederMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        toCallerMsg.setFromType(1);
        toCallerMsg.setToType(0);
        toCallederMsg.setMsgType(0);
        toCallederMsg.setAffirm(1);
        toCallerMsg.setBizType(2);
        toCallederMsg.setCmdType(1);
        toCallederMsg.setCommand(0x30);
        Map<String, Object> callederMap=new HashMap<String, Object>();
        callederMap.put("CallId", this.callData.getCallId());
        callederMap.put("CallerId", this.callData.getCallerId());
        callederMap.put("CallederId", this.callData.getCallederId());
        callederMap.put("HangupType", "3");
        MapContent _mc=new MapContent(callederMap);
        toCallederMsg.setMsgContent(_mc);
        pmm.getSendMemory().addMsg2Queue(this.callederKey, toCallederMsg);
        this.callData.addSendedMsg(toCallederMsg);//记录到已发送列表

        shutdown();
        System.out.println("通话检测到超时==[callid="+this.callData.getCallId()+"]:status="+this.callData.getStatus());
    }

    //===处理第一次被叫者说话的特殊流程
    private void  dealCallerFirstTalk() {
        if (this.callData.getStatus()==1||this.callData.getStatus()==2) {//等同于“被叫者”手工应答
            //构造“应答传递ACK”消息，并发送给“呼叫者”
            MsgNormal toCallerMsg=new MsgNormal();
            toCallerMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            toCallerMsg.setFromType(1);
            toCallerMsg.setToType(0);

            toCallerMsg.setMsgType(1);
            toCallerMsg.setAffirm(0);
            toCallerMsg.setBizType(2);
            toCallerMsg.setCmdType(1);
            toCallerMsg.setCommand(0x20);

            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("CallId", this.callData.getCallId());
            dataMap.put("CallerId", this.callData.getCallerId());
            dataMap.put("CallederId", this.callData.getCallederId());
            dataMap.put("ACKType", "1");//可以通话
            MapContent mc=new MapContent(dataMap);
            toCallerMsg.setMsgContent(mc);

            pmm.getSendMemory().addMsg2Queue(smm.getActivedUserSessionByUserId(this.callData.getCallerId()).getKey(), toCallerMsg);
            //记录到已发送列表
            this.callData.addSendedMsg(toCallerMsg);
            //修改状态
            this.callData.setStatus_3();
            System.out.println("第一次“被叫者”通话，并处理后==[callid="+this.callData.getCallId()+"]:status="+this.callData.getStatus());
        }
    }

    //=====以下三个为清除和关闭的操作
    //关闭
    private void shutdown() {
        callData.setStatus_9();
        System.out.println("结束进程后1==[callid="+this.callData.getCallId()+"]:status="+this.callData.getStatus());
    }

    //清除数据，把本电话控制的数据从内存数据链中移除
    private void cleanData() {
        //把内容写入日志文件
        System.out.println("结束进行后2，清除数据前==[callid="+this.callData.getCallId()+"]:status="+this.callData.getStatus());
        //清除未发送消息
        ConcurrentLinkedQueue<Message> tempMsgs = pmm.getSendMemory().getSendQueue(this.callerKey);
        if (tempMsgs!=null&&!tempMsgs.isEmpty()) {
            for (Message m: tempMsgs) {
                if (m instanceof MsgNormal) {
                    MsgNormal mn=(MsgNormal)m;
                    if (this.callData.getCallId().equals(((Map)mn.getMsgContent()).get("CallId")+"")) {
                        tempMsgs.remove(m);
                    }
                }
                if (m instanceof MsgMedia) {
                    MsgMedia mm=(MsgMedia)m;
                    if (mm.getBizType()==1&&this.callData.getCallId()==mm.getObjId()) {
                        tempMsgs.remove(m);
                    }
                }
            }
        }
        tempMsgs = pmm.getSendMemory().getSendQueue(this.callederKey);
        if (tempMsgs!=null&&!tempMsgs.isEmpty()) {
            for (Message m: tempMsgs) {
                if (m instanceof MsgNormal) {
                    MsgNormal mn=(MsgNormal)m;
                    if (this.callData.getCallId().equals(((Map)mn.getMsgContent()).get("CallId")+"")) {
                        tempMsgs.remove(m);
                    }
                }
                if (m instanceof MsgMedia) {
                    MsgMedia mm=(MsgMedia)m;
                    if (mm.getBizType()==1&&this.callData.getCallId()==mm.getObjId()) {
                        tempMsgs.remove(m);
                    }
                }
            }
        }
        cmm.removeCallData(this.callData.getCallId());
        this.callData.getCallerWts().clear();
        this.callData.getCallederWts().clear();
        this.callData=null;
    }

    /* 
     * 清除语音数据
     * @param type 若=2是强制删除，不管是否语音传递完成
     */
    private void cleanTalk(int type) {
        //目前都是强制删除
        //删除talk内存数据链
        TalkMemoryManage tmm = TalkMemoryManage.getInstance();
        tmm.cleanCallData(this.callData.getCallId());
        shutdown();
        System.out.println("清除语音数据后==[callid="+this.callData.getCallId()+"]:status="+this.callData.getStatus());
    }
}