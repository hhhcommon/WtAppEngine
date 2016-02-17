package com.woting.appengine.calling.monitor;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.calling.mem.CallingMemoryManage;
import com.woting.appengine.calling.model.OneCall;
import com.woting.appengine.calling.model.ProcessedMsg;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.mobile.mediaflow.mem.TalkMemoryManage;
import com.woting.appengine.mobile.mediaflow.model.WholeTalk;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.appengine.mobile.push.model.Message;
import com.woting.appengine.mobile.session.mem.SessionMemoryManage;
import com.woting.passport.UGA.service.UserService;
/**
 * 电话控制线程
 * @author wanghui
 *
 */
public class CallCtlThread extends Thread {
    private CallingMemoryManage cmm=CallingMemoryManage.getInstance();
    private PushMemoryManage pmm=PushMemoryManage.getInstance();
    private SessionMemoryManage smm=SessionMemoryManage.getInstance();
    private GroupMemoryManage gmm=GroupMemoryManage.getInstance();

    private OneCall callData;//所控制的通话数据
    private boolean isCallerTalked=false; //是否“被叫者”说话
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
                if (this.callData.getStatus()==1&&(System.currentTimeMillis()-this.callData.getBeginDialTime()>this.callData.getIt1_expire())) {
                    dealOutLine();
                }
                //一段时间后未收到“被叫者”手工应答Ack，的处理
                if ((this.callData.getStatus()==1||this.callData.getStatus()==2)&&(System.currentTimeMillis()-this.callData.getBeginDialTime()>this.callData.getIt2_expire())) {
                    dealNoAck();
                }
                //“被叫者”第一次说话
                if (!isCallerTalked&&this.callData.getCallWt()!=null) {
                    dealCallerFirstTalk();
                    isCallerTalked=true;
                }
                //读取预处理的消息
                Message m=this.callData.pollPreMsg();//第一条必然是呼叫信息
                if (m==null) continue;

                this.callData.setLastUsedTime();
                int flag=1;
                ProcessedMsg pMsg=new ProcessedMsg(m, System.currentTimeMillis(), this.getClass().getName());
                try {
                    if (m.getCommand().equals("1")) dial(m);//呼叫处理
                    else if (m.getCommand().equals("-b1")) flag=dealAutoDialFeedback(m); //被叫方的自动反馈
                    else if (m.getCommand().equals("2")) ackDial(m); //被叫方的手工应答
                    else if (m.getCommand().equals("3")) hangup(m); //挂断通话
                    pMsg.setStatus(flag);
                } catch(Exception e) {
                    pMsg.setStatus(-1);
                } finally {
                    pMsg.setEndTime(System.currentTimeMillis());
                    this.callData.addProcessedMsg(pMsg);
                }
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                cleanTalk(2);//强制清除声音
                cleanData();//清除数据
            }
        }
    }

    //===========以下是分步处理过程，全部是私有函数
    //处理呼叫(1)
    private void dial(Message m) {
        String callId =((Map)m.getMsgContent()).get("CallId")+"";
        String diallorId=MobileUtils.getMobileKey(m).getUserId();
        String callorId=((Map)m.getMsgContent()).get("CallorId")+"";

        Map<String, Object> dataMap=null;
        boolean isBusy=true; //是否占线
        boolean callorIsExisted=false; //是否被叫者存在

        //返回给呼叫者的消息
        Message toDiallorMsg=new Message();
        toDiallorMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        toDiallorMsg.setReMsgId(m.getMsgId());
        toDiallorMsg.setToAddr(m.getFromAddr());
        toDiallorMsg.setFromAddr(m.getToAddr());
        toDiallorMsg.setMsgType(-1);
        toDiallorMsg.setMsgBizType("CALL_CTL");
        toDiallorMsg.setCmdType("CALL");
        toDiallorMsg.setCommand("-1");
        //判断被叫者是否存在
        //从Spring中得到userService
        Object u=null;
        ServletContext sc = (ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent();
        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
            UserService us = (UserService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("userService");
            try {u=us.getUserById(callorId);} catch(Exception e){};
        }
        if (u==null) toDiallorMsg.setReturnType("10021");
        else
        if (smm.getUserSessionByUserId(callorId)==null) toDiallorMsg.setReturnType("10022");
        else callorIsExisted=true;
        //判断是否占线
        if (callorIsExisted) {
            //TODO ***占线的判断要用索机制，现在先不进行处理。这个比较复杂，涉及到多线程的问题。
            if (diallorId.equals(callorId))  toDiallorMsg.setReturnType("10033");
            else
            if (gmm.isTalk(callorId)) toDiallorMsg.setReturnType("10032");
            else
            if (cmm.isTalk(callorId)) toDiallorMsg.setReturnType("10031");
            else isBusy=false;
        }
        if (callorIsExisted&&!isBusy) toDiallorMsg.setReturnType("1001");
        dataMap=new HashMap<String, Object>();
        dataMap.put("CallId", callId);
        dataMap.put("DiallorId", diallorId);
        dataMap.put("CallorId", callorId);
        toDiallorMsg.setMsgContent(dataMap);
        pmm.getSendMemory().addMsg2Queue(MobileUtils.getMobileKey(m), toDiallorMsg);
        //记录到已发送列表
        this.callData.addSendedMsg(toDiallorMsg);

        //若不存在用户或占线要删除数据及这个过程
        if (!callorIsExisted||isBusy) shutdown();

        //给被叫者发送信息
        if (callorIsExisted&&smm.getUserSessionByUserId(callorId)!=null) {
            Message toCallorMsg=new Message();
            toCallorMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            toCallorMsg.setMsgType(1);
            toCallorMsg.setAffirm(isBusy?0:1);
            toCallorMsg.setMsgBizType("CALL_CTL");
            toCallorMsg.setCommand("b1");
            dataMap.put("DialType", isBusy?"2":"1");
            toCallorMsg.setMsgContent(dataMap);

            pmm.getSendMemory().addMsg2Queue(smm.getUserSessionByUserId(callorId).getKey(), toCallorMsg);
            //记录到已发送列表
            this.callData.addSendedMsg(toCallorMsg);
            //修改状态
            this.callData.setStatus_1();
            //开始计时，两个过程
            this.callData.setBeginDialTime();
        }
    }

    //处理被叫方的自动呼叫反馈(-b1)
    private int dealAutoDialFeedback(Message m) {
        //首先判断这个消息是否符合处理的要求：callid, callorId, diallorId是否匹配
        if (!this.callData.getDiallorId().equals(((Map)m.getMsgContent()).get("DiallorId")+"")||!this.callData.getCallorId().equals(MobileUtils.getMobileKey(m).getUserId())) return 3;
        if (this.callData.getStatus()==1) {//状态正确，如果是其他状态，这个消息抛弃
            //发送给呼叫者的消息
            Message toDiallorMsg=new Message();
            toDiallorMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            toDiallorMsg.setToAddr(MobileUtils.getAddr(smm.getUserSessionByUserId(this.callData.getDiallorId()).getKey()));
            toDiallorMsg.setFromAddr("{(phone)@@(www.woting.fm||S)}");

            toDiallorMsg.setMsgType(1);
            toDiallorMsg.setAffirm(0);
            toDiallorMsg.setMsgBizType("CALL_CTL");
            toDiallorMsg.setCmdType("CALL");
            toDiallorMsg.setCommand("b4");

            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("CallId", this.callData.getCallId());
            dataMap.put("DiallorId", this.callData.getDiallorId());
            dataMap.put("CallorId", this.callData.getCallorId());
            dataMap.put("OnLineType", "1");
            toDiallorMsg.setMsgContent(dataMap);

            pmm.getSendMemory().addMsg2Queue(smm.getUserSessionByUserId(this.callData.getDiallorId()).getKey(), toDiallorMsg);
            //记录到已发送列表
            this.callData.addSendedMsg(toDiallorMsg);
            //修改状态
            this.callData.setStatus_2();
            return 1;
        } else return 2;//被抛弃
    }

    //处理“被叫者”应答(2)
    private int ackDial(Message m) {
        //首先判断这个消息是否符合处理的要求：callid, callorId, diallorId是否匹配
        if (!this.callData.getDiallorId().equals(((Map)m.getMsgContent()).get("DiallorId")+"")||!this.callData.getCallorId().equals(MobileUtils.getMobileKey(m).getUserId())) return 3;
        if (this.callData.getStatus()==1||this.callData.getStatus()==2) {//状态正确，如果是其他状态，这个消息抛弃
            //构造“应答传递ACK”消息，并发送给“呼叫者”
            Message toDiallorMsg=new Message();
            toDiallorMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            toDiallorMsg.setToAddr(MobileUtils.getAddr(smm.getUserSessionByUserId(this.callData.getDiallorId()).getKey()));
            toDiallorMsg.setFromAddr("{(phone)@@(www.woting.fm||S)}");
            
            toDiallorMsg.setMsgType(1);
            toDiallorMsg.setAffirm(0);
            toDiallorMsg.setMsgBizType("CALL_CTL");
            toDiallorMsg.setCmdType("CALL");
            toDiallorMsg.setCommand("b2");

            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("CallId", this.callData.getCallId());
            dataMap.put("DiallorId", this.callData.getDiallorId());
            dataMap.put("CallorId", this.callData.getCallorId());
            dataMap.put("ACKType", ((Map)m.getMsgContent()).get("ACKType")+"");
            toDiallorMsg.setMsgContent(dataMap);

            pmm.getSendMemory().addMsg2Queue(smm.getUserSessionByUserId(this.callData.getDiallorId()).getKey(), toDiallorMsg);
            //记录到已发送列表
            this.callData.addSendedMsg(toDiallorMsg);
            //修改状态
            this.callData.setStatus_3();
            return 1;
        } else return 2;//被抛弃
    }

    //处理“挂断”(3)
    private int hangup(Message m) {
        //首先判断是那方在进行挂断
        String hangupperId=MobileUtils.getMobileKey(m).getUserId();
        this.callData.getOtherId(hangupperId);
        String otherId=this.callData.getOtherId(hangupperId);
        if (otherId==null) return 3;
        //给另一方发送“挂断传递”消息
        Message otherMsg=new Message();
        otherMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        otherMsg.setToAddr(MobileUtils.getAddr(smm.getUserSessionByUserId(this.callData.getDiallorId()).getKey()));
        otherMsg.setFromAddr("{(phone)@@(www.woting.fm||S)}");
        
        otherMsg.setMsgType(1);
        otherMsg.setAffirm(0);
        otherMsg.setMsgBizType("CALL_CTL");
        otherMsg.setCmdType("CALL");
        otherMsg.setCommand("b3");

        Map<String, Object> dataMap=new HashMap<String, Object>();
        dataMap.put("CallId", this.callData.getCallId());
        dataMap.put("DiallorId", this.callData.getDiallorId());
        dataMap.put("CallorId", this.callData.getCallorId());
        dataMap.put("HangupType", "1");
        otherMsg.setMsgContent(dataMap);

        pmm.getSendMemory().addMsg2Queue(smm.getUserSessionByUserId(otherId).getKey(), otherMsg);
        //记录到已发送列表
        this.callData.addSendedMsg(otherMsg);
        //修改状态
        this.callData.setStatus_4();
        return 1;
    }

    //=======以下两个超时处理
    //处理“被叫者”不在线
    private void dealOutLine() {
        //发送给“呼叫者”的消息
        Message toDiallorMsg=new Message();
        toDiallorMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        toDiallorMsg.setToAddr(MobileUtils.getAddr(smm.getUserSessionByUserId(this.callData.getDiallorId()).getKey()));
        toDiallorMsg.setFromAddr("{(phone)@@(www.woting.fm||S)}");

        toDiallorMsg.setMsgType(1);
        toDiallorMsg.setAffirm(0);
        toDiallorMsg.setMsgBizType("CALL_CTL");
        toDiallorMsg.setCmdType("CALL");
        toDiallorMsg.setCommand("b4");
        Map<String, Object> dataMap=new HashMap<String, Object>();
        dataMap.put("CallId", this.callData.getCallId());
        dataMap.put("DiallorId", this.callData.getDiallorId());
        dataMap.put("CallorId", this.callData.getCallorId());
        dataMap.put("OnLineType", "2");
        toDiallorMsg.setMsgContent(dataMap);

        pmm.getSendMemory().addMsg2Queue(smm.getUserSessionByUserId(this.callData.getDiallorId()).getKey(), toDiallorMsg);
        //记录到已发送列表
        this.callData.addSendedMsg(toDiallorMsg);

        shutdown();
    }

    //处理“被叫者”未手工应答
    private void dealNoAck() {
        //1、构造“应答传递ACK”消息，并发送给“呼叫者”
        Message toDiallorMsg=new Message();
        toDiallorMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        toDiallorMsg.setToAddr(MobileUtils.getAddr(smm.getUserSessionByUserId(this.callData.getDiallorId()).getKey()));
        toDiallorMsg.setFromAddr("{(phone)@@(www.woting.fm||S)}");
        
        toDiallorMsg.setMsgType(1);
        toDiallorMsg.setAffirm(0);
        toDiallorMsg.setMsgBizType("CALL_CTL");
        toDiallorMsg.setCmdType("CALL");
        toDiallorMsg.setCommand("b2");

        Map<String, Object> dataMap=new HashMap<String, Object>();
        dataMap.put("CallId", this.callData.getCallId());
        dataMap.put("DiallorId", this.callData.getDiallorId());
        dataMap.put("CallorId", this.callData.getCallorId());
        dataMap.put("ACKType", "32");
        toDiallorMsg.setMsgContent(dataMap);

        pmm.getSendMemory().addMsg2Queue(smm.getUserSessionByUserId(this.callData.getDiallorId()).getKey(), toDiallorMsg);
        //记录到已发送列表
        this.callData.addSendedMsg(toDiallorMsg);

        //2、构造“挂断传递”消息，并发送给“被叫者”
        Message toCallorMsg=new Message();
        toCallorMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        toCallorMsg.setToAddr(MobileUtils.getAddr(smm.getUserSessionByUserId(this.callData.getDiallorId()).getKey()));
        toCallorMsg.setFromAddr("{(phone)@@(www.woting.fm||S)}");
        
        toCallorMsg.setMsgType(1);
        toCallorMsg.setAffirm(0);
        toCallorMsg.setMsgBizType("CALL_CTL");
        toCallorMsg.setCmdType("CALL");
        toCallorMsg.setCommand("b3");

        dataMap=new HashMap<String, Object>();
        dataMap.put("CallId", this.callData.getCallId());
        dataMap.put("DiallorId", this.callData.getDiallorId());
        dataMap.put("CallorId", this.callData.getCallorId());
        dataMap.put("HangupType", "2");
        toCallorMsg.setMsgContent(dataMap);

        pmm.getSendMemory().addMsg2Queue(smm.getUserSessionByUserId(this.callData.getCallorId()).getKey(), toCallorMsg);
        //记录到已发送列表
        this.callData.addSendedMsg(toCallorMsg);

        shutdown();
    }

    //===处理第一次被叫者说话的特殊流程
    private void  dealCallerFirstTalk() {
        if (this.callData.getStatus()==1||this.callData.getStatus()==2) {//等同于“被叫者”手工应答
            //构造“应答传递ACK”消息，并发送给“呼叫者”
            Message toDiallorMsg=new Message();
            toDiallorMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            toDiallorMsg.setToAddr(MobileUtils.getAddr(smm.getUserSessionByUserId(this.callData.getDiallorId()).getKey()));
            toDiallorMsg.setFromAddr("{(phone)@@(www.woting.fm||S)}");
            
            toDiallorMsg.setMsgType(1);
            toDiallorMsg.setAffirm(0);
            toDiallorMsg.setMsgBizType("CALL_CTL");
            toDiallorMsg.setCmdType("CALL");
            toDiallorMsg.setCommand("b2");

            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("CallId", this.callData.getCallId());
            dataMap.put("DiallorId", this.callData.getDiallorId());
            dataMap.put("CallorId", this.callData.getCallorId());
            dataMap.put("ACKType", "1");//可以通话
            toDiallorMsg.setMsgContent(dataMap);

            pmm.getSendMemory().addMsg2Queue(smm.getUserSessionByUserId(this.callData.getDiallorId()).getKey(), toDiallorMsg);
            //记录到已发送列表
            this.callData.addSendedMsg(toDiallorMsg);
            //修改状态
            this.callData.setStatus_3();
        }
    }

    //=====以下三个为清除和关闭的操作
    //关闭
    private void shutdown() {
        callData.setStatus_9();
    }

    //清除数据，把本电话控制的数据从内存数据链中移除
    private void cleanData() {
        //把内容写入日志文件
        cmm.removeCallData(this.callData.getCallId());
        this.callData.setDialWt(null);
        this.callData.setCallWt(null);
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
    }
}