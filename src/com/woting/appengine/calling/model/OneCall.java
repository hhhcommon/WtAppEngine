package com.woting.appengine.calling.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.woting.appengine.mobile.mediaflow.model.WholeTalk;
import com.woting.appengine.mobile.push.model.Message;

/**
 * 一次通话所需要的数据结构。
 * 每次通话都由一个线程负责处理，因此这个类的实例是一个独享的结构，可以不考虑多线程。
 * 但消息的写入可能由多个线程处理，因此要采用同步方法addPreMsg。
 * 
 * @author wanghui
 */
public class OneCall implements Serializable {
    private static final long serialVersionUID = -2635864824531924446L;

    private volatile Object preMsglock=new Object();
    private volatile Object statuslock=new Object();

    private String callId;//本次通话的Id
    public String getCallId() {
        return callId;
    }
    private String diallorId;//呼叫者Id
    public String getDiallorId() {
        return diallorId;
    }
    private String callorId;//被叫者Id
    public String getCallorId() {
        return callorId;
    }
    private long createTime;//本对象创建时间
    public long getCreateTime() {
        return createTime;
    }
    private long beginDialTime;//向“被叫者”发起呼叫的时间
    public long getBeginDialTime() {
        return beginDialTime;
    }
    public void setBeginDialTime() {
        this.beginDialTime=System.currentTimeMillis();
    }
    private long lastUsedTime;//最后被使用时间
    public long getLastUsedTime() {
        return lastUsedTime;
    }
    public void setLastUsedTime() {
        this.lastUsedTime=System.currentTimeMillis();
    }

    //以下是两个判断超时的参数，这种方法允许每个不同的通话采用自己的机制处理超时
    private final long it1_expire;//检查是否在线的过期时间
    private final long it2_expire;//检查是否无应答的过期时间
    public long getIt1_expire() {
        return it1_expire;
    }
    public long getIt2_expire() {
        return it2_expire;
    }

    private volatile int status=0; //通话过程的状态10呼叫；这个在写的时候再完善
    public int getStatus() {
        synchronized(statuslock) {
            return status;
        }
    }
    public void setStatus_1() {//已向“被叫者”发出拨号信息
        synchronized(statuslock) {
            this.status=1;
        }
    }
    public void setStatus_2() {//已收到“被叫者”的自动呼叫反馈，等待“被叫者”手工应答
        synchronized(statuslock) {
            this.status=2;
        }
    }
    public void setStatus_3() {//已收到“被叫者”的手动反馈反馈ACK，可以通话了
        synchronized(statuslock) {
            this.status=3;//这是通话状态
        }
    }
    public void setStatus_4() {//通话挂断状态
        synchronized(statuslock) {
            this.status=4;
        }
    }
    public void setStatus_9() {//结束对话
        synchronized(statuslock) {
            this.status=9;
        }
    }

    //以下两个对象用来记录App->Server的消息
    private LinkedList<Message> preMsgQueue;//预处理(还未处理)的本呼叫的消息
    private List<ProcessedMsg> processedMsgList;//已经处理过的消息
    //以下对象用来记录Server->app的消息
    private List<Message> sendedMsgList;//已经发出的消息，这里记录仅仅是作为日志的材料

    /**
     * 一次通话的结构，这个构造函数限定：
     * 若要构造此类，必须要知呼叫者，被叫者和通话Id。
     * 构造函数还创建了需要内存结构
     * @param callId 通话Id
     * @param diallorId 呼叫者Id
     * @param callorId 被叫者Id
     */
    public OneCall(String callId, String diallorId, String callorId, long it1_expire, long it2_expire) {
        super();
        this.callId = callId;
        this.diallorId = diallorId;
        this.callorId = callorId;
        this.createTime=System.currentTimeMillis();
        this.beginDialTime=-1;//不在使用的情况

        this.it1_expire=(it1_expire<=0?500:it1_expire);
        this.it2_expire=(it2_expire<=0?30000:it2_expire);

        this.status=0;//仅创建，还未处理

        this.preMsgQueue=new LinkedList<Message>();
        this.processedMsgList=new ArrayList<ProcessedMsg>();
        this.sendedMsgList=new ArrayList<Message>();

        this.dialWt=null;
        this.callWt=null;
    }

    /**
     * 按照FIFO的队列方式，获取一条待处理的消息，并删除他
     * @return 待处理的消息
     */
    public Message pollPreMsg() {
        return preMsgQueue.poll();
    }
    public List<ProcessedMsg> getProcessedMsgList() {
        return processedMsgList;
    }
    public List<Message> getSendedMsgList() {
        return sendedMsgList;
    }

    //以下为添加对象的方法
    public void addPreMsg(Message msg) {
        synchronized(preMsglock) {
            this.preMsgQueue.add(msg);
        }
    }

    public void addSendedMsg(Message msg) {
        this.sendedMsgList.add(msg);
    }

    public void addProcessedMsg(ProcessedMsg pm) {
        this.processedMsgList.add(pm);
    }

    public String getOtherId(String oneId) {
        String otherId=null;
        if (this.getCallorId().equals(oneId)) otherId=this.getDiallorId();
        if (this.getDiallorId().equals(oneId)) otherId=this.getCallorId();
        return otherId;
    }

    //呼叫者语音信息
    private WholeTalk dialWt=null;
    public WholeTalk getDialWt() {
        return dialWt;
    }
    public void setDialWt(WholeTalk dialWt) {
        this.dialWt = dialWt;
    }
    //被叫者语音信息
    private WholeTalk callWt=null;
    public WholeTalk getCallWt() {
        return callWt;
    }
    public void setCallWt(WholeTalk callWt) {
        this.callWt = callWt;
    }
}