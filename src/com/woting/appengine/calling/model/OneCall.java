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
    private static final long serialVersionUID=-2635864824531924446L;

    private int callType;//=1是对讲模式；=2是电话模式；若是0，则表明未设置，采用默认值0

    private volatile String speakerId;
    private volatile Object preMsglock=new Object();
    private volatile Object statuslock=new Object();
    private volatile Object speakerlock=new Object();

    private String callId;//本次通话的Id
    public String getCallId() {
        return callId;
    }
    private String callerId;//呼叫者Id
    public String getCallerId() {
        return callerId;
    }
    private String callederId;//被叫者Id
    public String getCallederId() {
        return callederId;
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
    private final long it3_expire;//检查通话过期时间
    public long getIt1_expire() {
        return it1_expire;
    }
    public long getIt2_expire() {
        return it2_expire;
    }
    public long getIt3_expire() {
        return it3_expire;
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
     * @param callType 通话模式
     * @param callId 通话Id
     * @param callId 通话Id
     * @param callerId 呼叫者Id
     * @param callederId 被叫者Id
     * @param it1_expire 占线判断过期时间
     * @param it2_expire 未应答怕判断过期时间
     * @param it3_expire 通话过期时间
     */
    public OneCall(int callType, String callId, String callerId, String callederId, long it1_expire, long it2_expire, long it3_expire) {
        super();
        this.speakerId=null;
        this.callType=callType;
        if (this.callType==0) this.callType=1;//设置为对讲模式
        this.callId=callId;
        this.callerId=callerId;
        this.callederId=callederId;
        this.createTime=System.currentTimeMillis();
        this.beginDialTime=-1;//不在使用的情况
        this.lastUsedTime=System.currentTimeMillis();

        this.it1_expire=(it1_expire<=0?10000:it1_expire);
        this.it2_expire=(it2_expire<=0?60000:it2_expire);
        this.it3_expire=(it3_expire<=0?120000:it3_expire);

        this.status=0;//仅创建，还未处理

        this.preMsgQueue=new LinkedList<Message>();
        this.processedMsgList=new ArrayList<ProcessedMsg>();
        this.sendedMsgList=new ArrayList<Message>();

        this.callerWts=new ArrayList<WholeTalk>();
        this.callederWts=new ArrayList<WholeTalk>();
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
        if (this.getCallerId().equals(oneId)) otherId=this.callederId;
        if (this.getCallederId().equals(oneId)) otherId=this.callerId;
        return otherId;
    }

    /**
     * 设置说话者id
     * @param speakerId 说话者Id
     * @return 返回值：
     *  "1"——设置成功
     *  "0"——由于callType不是对讲模式，不能设置
     *  "-N::描述"——错误的状态：其中N就是状态值，参考status，只有状态3可以通话
     *  "2::当前说话人id"——有人在通话
     */
    public String setSpeaker(String speakerId) {
        if (callType!=1) return "0";
        String ret=null;
        if (this.status!=3) {
            ret="-"+this.status+"::目前状态为["+OneCall.convertStatus(this.status)+"],不能对讲通话";
        } else {
            synchronized (speakerlock) {
                if (this.speakerId==null) {
                    this.speakerId=speakerId;
                    ret="1";
                }
                else ret="2::"+this.speakerId;
            }
        }
        return ret;
    }

    /**
     * 清除说话者id
     * @param speakerId 说话者Id
     * @return 返回值：
     *  "1"——清除成功
     *  "0"——由于callType不是对讲模式，不能设置
     *  "-N::描述"——错误的状态：其中N就是状态值，参考status，只有状态3可以通话
     *  "2"——清除人和当前说话者不一致
     */
    public String cleanSpeaker(String tobeCleanSpeakerId) {
        if (callType!=1) return "0";
        String ret=null;
        if (this.status!=3) {
            ret="-"+this.status+"::目前状态为["+OneCall.convertStatus(this.status)+"],不能结束对讲通话";
        } else {
            synchronized (speakerlock) {
                if (this.speakerId.equals(tobeCleanSpeakerId)) {
                    this.speakerId=null;
                    ret="1";
                }
                else ret="2::"+this.speakerId;
            }
        }
        return ret;
    }

    //呼叫者语音信息
    private List<WholeTalk> callerWts=null;
    public void addCallerWt(WholeTalk callerWt) {
        boolean canAdd=true;
        for (WholeTalk wt: this.callerWts) {
            if (wt.getTalkId().equals(callerWt.getTalkId())) {
                canAdd=false;
                break;
            }
        }
        if (canAdd) this.callerWts.add(callerWt);
    }
    public List<WholeTalk> getCallerWts() {
        return this.callerWts;
    }
    //被叫者语音信息
    private List<WholeTalk> callederWts=null;
    public void addCallederWt(WholeTalk callederWt) {
        boolean canAdd=true;
        for (WholeTalk wt: this.callederWts) {
            if (wt.getTalkId().equals(callederWt.getTalkId())) {
                canAdd=false;
                break;
            }
        }
        if (canAdd) this.callederWts.add(callederWt);
    }
    public List<WholeTalk> getCallederWts() {
        return this.callederWts;
    }

    private static final String convertStatus(int status) {
        if (status==0||status==1||status==2) return "正在呼叫，还未建立连接";
        if (status==3) return "通话中";
        if (status==4) return "正在挂断";
        if (status==9) return "通话已结束";
        
        return "未知状态";
    }
}