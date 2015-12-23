package com.woting.mobile.push.mem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.woting.mobile.model.MobileKey;
import com.woting.mobile.push.model.Message;
import com.woting.mobile.push.model.SendMessageList;
import java.util.concurrent.atomic.AtomicBoolean;

public class PushMemoryManage {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static PushMemoryManage instance = new PushMemoryManage();
    }
    public static PushMemoryManage getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    //数据区
    protected ReceiveMemory rm; //接收数据内存结构
    protected SendMemory sm; //发送数据内存结构
    protected SendedMemory hasSm; //发送数据内存结构
    //数据区

    public ReceiveMemory getReceiveMemory() {
        return this.rm;
    }
    public SendMemory getSendMemory() {
        return this.sm;
    }

    /*
     * 构造方法，初始化消息推送的内存结构
     */
    private PushMemoryManage() {
        rm=ReceiveMemory.getInstance();
        sm=SendMemory.getInstance();
        hasSm=SendedMemory.getInstance();
    }

    /**
     * 清理发送内存结构，把没有数据的设备删除掉<br/>
     * 包括待发送列表和已发送列表
     * ConcurrentLinkedQueue<Message>
     */
    public void clean() {
        if (this.sm.msgMap!=null&&!this.sm.msgMap.isEmpty()) {
            for (MobileKey sKey: this.sm.msgMap.keySet()) {
                ConcurrentLinkedQueue<Message> mq = this.sm.msgMap.get(sKey);
                if (mq==null||mq.isEmpty()) this.sm.msgMap.remove(sKey);
            }
        }
        if (this.hasSm.msgMap!=null&&!this.hasSm.msgMap.isEmpty()) {
            for (MobileKey sKey: this.hasSm.msgMap.keySet()) {
                SendMessageList sml = this.hasSm.msgMap.get(sKey);
                if (sml==null||sml.size()==0) this.hasSm.msgMap.remove(sKey);
            }
        }
    }

    /**
     * 根据设备标识MobileKey(mk)，获得发送消息体，注意，消息体不得多于3条。<br/>
     * 若有更多需要回传的消息，需通过下次请求给出。
     * @param mk 设备标识
     * @return 消息体
     */
    public List<Message> getSendMessages(MobileKey mk) {
        List<Message> retl = new ArrayList<Message>();
        //从发送队列取一条消息
        Message m1= sm.pollTypeQueue(mk);
        if (m1!=null) retl.add(m1);
        SendMessageList hasSl = hasSm.getSendedMessagList(mk);
        if (hasSl!=null&&hasSl.size()>0) {
            int lowerIndex=3-retl.size();
            if (hasSl.size()<lowerIndex) lowerIndex=hasSl.size();
            for (int i=0; i<lowerIndex; i++) {
                retl.add(hasSl.get(i));
            }
        }
        try {
            if (m1!=null) hasSl.add(m1);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private AtomicBoolean serverRuning=new AtomicBoolean(false); //推送服务是否正常运行
    public boolean isServerRuning() {
        return this.serverRuning.get();
    }
    public void setServerRuning(boolean serverRuning) {
        this.serverRuning.set(serverRuning);
    }
}