package com.woting.appengine.mobile.push.mem;

//import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.woting.push.core.message.Message;

/**
 * 消息接收的数据结构。<br/>
 * 定义消息接收的数据结构，并给出操作这些数据的方法(数据逻辑方法)，但不涉及任何处理逻辑
 * @author wanghui
 */
public class ReceiveMemory {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static ReceiveMemory instance = new ReceiveMemory();
    }
    /**
     * 得到单例的对象
     * @return 接收消息对象
     */
    public static ReceiveMemory getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end0

    private ConcurrentLinkedQueue<Message> pureMsgQueue; //总接收队列所有收到的信息都会暂时先放入这个队列中
    private ConcurrentHashMap<String, ConcurrentLinkedDeque<Message>> typeMsgMap; //分类接收队列，不同类型的消息会由不同类去处理

    /*
     * 初始化，创建两个主要的对象
     */
    private ReceiveMemory() {
       this.pureMsgQueue=new ConcurrentLinkedQueue<Message>();
       this.typeMsgMap=new ConcurrentHashMap<String, ConcurrentLinkedDeque<Message>>();
    }

    //原始消息处理
    /**
     * 向原始接收队列加入消息
     * @param msg 消息，此消息是从底层读取的最原始的消息，采用字节数组
     * @return 若成功插入，返回true，否则返回false，由于总接收队列是无边界的并发链表队列，理论上不会返回false
     */
    public boolean addPureQueue(Message msg) {
        return this.pureMsgQueue.offer(msg);
    }

    /**
     * 从原始接收队列获取消息，并从队列移除该消息
     * @return  消息元素
     */
    public synchronized Message pollPureQueue() {
        if (this.pureMsgQueue==null) return null;
        return this.pureMsgQueue.poll();
    }

    //分类消息处理
    /**
     * 按某一分类向分类接收双向队列的末尾加入消息
     * @param _type 分类标识
     * @param msg 消息
     * @return 若成功插入，返回true，否则返回false，由于分类接收队列是无边界的并发链表队列，理论上不会返回false
     */
    public boolean appendTypeMsgMap(String _type, Message msg) {
        ConcurrentLinkedDeque<Message> typeDeque=this.typeMsgMap.get(_type);
        if (typeDeque==null) {
            synchronized(this.typeMsgMap) {
                typeDeque=new ConcurrentLinkedDeque<Message>();
                this.typeMsgMap.put(_type, typeDeque);
            }
        }
        return typeDeque.offer(msg);
    }

    /**
     * 按某一分类向分类接收双向队列的开头加入消息
     * @param _type 分类标识
     * @param msg 消息
     */
    public void addFirstTypeMsgMap(String _type, Message msg) {
        ConcurrentLinkedDeque<Message> typeDeque=this.typeMsgMap.get(_type);
        if (typeDeque==null) {
            synchronized(this.typeMsgMap) {
                typeDeque=new ConcurrentLinkedDeque<Message>();
                this.typeMsgMap.put(_type, typeDeque);
            }
        }
        typeDeque.addFirst(msg);
    }
    
    /**
     * 从分类接收队列获取消息，并从该队列移除消息
     * @param _type 分类标识
     * @return 消息
     */
    public synchronized Message pollTypeQueue(String _type) {
        if (this.typeMsgMap==null) return null;
        if (this.typeMsgMap.get(_type)==null) return null;
        return this.typeMsgMap.get(_type).poll();
    }

    /**
     * 从分类接收队列获取消息，但不从该队列移除消息，该消息还存在于这个分类接收队列中
     * @param _type 分类标识
     * @return 消息
     */
    public Message peekTypeQueue(String _type) {
        if (this.typeMsgMap==null) return null;
        if (this.typeMsgMap.get(_type)==null) return null;
        return this.typeMsgMap.get(_type).peek();
    }
}