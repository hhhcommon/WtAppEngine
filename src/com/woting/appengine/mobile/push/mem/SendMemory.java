package com.woting.appengine.mobile.push.mem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.push.model.CompareMsg;
import com.woting.appengine.mobile.push.model.Message;
import com.woting.appengine.mobile.push.model.SendMessageList;

public class SendMemory {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static SendMemory instance = new SendMemory();
    }
    public static SendMemory getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    protected ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> msgMap;//将要发送的消息列表，String是用户的Key信息
    protected ConcurrentHashMap<String, SendMessageList> msgSendedMap;//已发送的信息情况
    protected ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>> notifyMsgMap;//通知类消息Map，String是用户的Id信息

    /*
     * 初始化发送消息结构
     */
    private SendMemory() {
        msgMap=new ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>>();
        notifyMsgMap=new ConcurrentHashMap<String, ConcurrentLinkedQueue<Message>>();
        msgSendedMap=new ConcurrentHashMap<String, SendMessageList>();
    }

    protected Map<String, SendMessageList> getMsgSendedMap() {
        return this.msgSendedMap;
    }

    //发送消息处理
    /**
     * 向某一用户的通知类消息队列放入一条消息
     * @param userId 用户标识
     * @param msg 消息数据
     */
    public void addMsg2NotifyQueue(String userId, Message msg) {
        ConcurrentLinkedQueue<Message> notifyQueue=notifyMsgMap.get(userId);
        if (notifyQueue==null) {
            notifyQueue=new ConcurrentLinkedQueue<Message>();
            notifyMsgMap.put(userId, notifyQueue);
        }
        synchronized(notifyQueue) {
            if (msg.getSendTime()==0) msg.setSendTime(System.currentTimeMillis());
            notifyQueue.add(msg);
        }
    }

    /**
     * 向某一设移动设备的输出队列中插入
     * @param mk 移动设备标识
     * @param msg 消息数据
     */
    public void addMsg2Queue(MobileKey mk, Message msg) {
        ConcurrentLinkedQueue<Message> mobileQueue=this.msgMap.get(mk.toString());
        if (mobileQueue==null) {
            mobileQueue=new ConcurrentLinkedQueue<Message>();
            this.msgMap.put(mk.toString(), mobileQueue);
        }
        synchronized(mobileQueue) {
            if (msg.getSendTime()==0) msg.setSendTime(System.currentTimeMillis());
            mobileQueue.add(msg);
        }
    }

    /**
     * 向某一设移动设备的输出队列中插入唯一消息，唯一消息是指，同一时间某类消息对一个设备只能有一个消息内容。
     * @param mk 移动设备标识
     * @param msg 消息数据
     */
    public void addUniqueMsg2Queue(MobileKey mk, Message msg, CompareMsg compMsg) {
        //唯一化处理
        //1-首先把一已发送列表中的同类消息删除
        SendMessageList sendedMl = this.msgSendedMap.get(mk.toString());
        if (sendedMl!=null&&sendedMl.size()>0) {
            for (int i=sendedMl.size()-1; i>=0; i--) {
                Message m=sendedMl.get(i);
                if (compMsg!=null&&compMsg.compare(m, msg)) sendedMl.remove(i);
            }
        }
        //2-加入现有的队列
        ConcurrentLinkedQueue<Message> mobileQueue=this.msgMap.get(mk.toString());
        if (mobileQueue==null) {
            mobileQueue=new ConcurrentLinkedQueue<Message>();
            this.msgMap.put(mk.toString(), mobileQueue);
        }
        synchronized(mobileQueue) {
            List<Message> removeMsg = new ArrayList<Message>();
            for (Message m: mobileQueue) {
                if (compMsg!=null&&compMsg.compare(m, msg)) removeMsg.add(m);
            }
            for (Message m: removeMsg) {
                mobileQueue.remove(m);
            }
            if (msg.getSendTime()==0) msg.setSendTime(System.currentTimeMillis());
            mobileQueue.add(msg);
        }
    }

    /**
     * 从某一设备的发送队列中取出要发送的消息，并从该队列中将这条消息移除
     * @param mk 设备标识
     * @return 消息
     */
    public Message pollQueue(MobileKey mk) {
        if (this.msgMap==null) return null;
        if (this.msgMap.get(mk.toString())==null) return null;
        return this.msgMap.get(mk.toString()).poll();
    }

    /**
     * 从某一设备的发送队列中取出要发送的消息，但不从该队列移除这条消息，这条消息还存在于设备发送队列中
     * @param mk 设备标识
     * @return 消息
     */
    public Message peekMobileQueue(MobileKey mk) {
        if (this.msgMap==null) return null;
        if (this.msgMap.get(mk.toString())==null) return null;
        return this.msgMap.get(mk.toString()).peek();
    }

    /**
     * 从通知消息发送队列中取出要发送的消息，并从该队列中将这条消息移除
     * @param userId 用户标识
     * @return 消息
     */
    public Message pollNotifyQueue(String userId) {
        if (this.notifyMsgMap==null) return null;
        if (this.notifyMsgMap.get(userId)==null) return null;
        return this.notifyMsgMap.get(userId).poll();
    }

    //已发送消息处理
    /**
     * 向某一设移动设备的已发送列表插入数据
     * @param mk
     * @param msg
     * @return 插入成功返回true，否则返回false
     * @throws IllegalAccessException 
     */
    public boolean addSendedMsg(MobileKey mk, Message msg) throws IllegalAccessException {
        SendMessageList mobileSendedList=this.msgSendedMap.get(mk.toString());
        if (mobileSendedList==null) {
            mobileSendedList=new SendMessageList(mk);
            this.msgSendedMap.put(mk.toString(), mobileSendedList);
        }
        return mobileSendedList.add(msg);
    }

    /**
     * 根据某一移动设备标识，获得已发送消息列表
     * @param mk
     * @return
     */
    public SendMessageList getSendedMessagList(MobileKey mk) {
        if (mk!=null) return this.msgSendedMap.get(mk.toString());
        return null;
    }
    /**
     * 根据某一移动设备标识，获得发送消息列表
     * @param mk
     * @return
     */
    public ConcurrentLinkedQueue<Message> getSendQueue(MobileKey mk) {
        if (mk!=null) return this.msgMap.get(mk.toString());
        return null;
    }
}