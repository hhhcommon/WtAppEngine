package com.woting.appengine.mobile.push.mem;

 import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.spiritdata.framework.core.structure.StrArrayQueue;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.push.core.message.Message;
import com.woting.appengine.mobile.push.model.SendMessageList;
import com.woting.appengine.mobile.push.monitor.socket.SocketHandle;
import com.woting.appengine.mobile.session.mem.SessionMemoryManage;
import com.woting.appengine.mobile.session.model.MobileSession;

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
    public StrArrayQueue logQueue;//日志数据

    private Object userSocketLock=new Object();//用户和Sockek对应的临界区的锁，这个锁是读写一致的，虽然慢，但能保证数据一致性
    public ConcurrentHashMap<MobileKey, SocketHandle> userSocketM;//用户和Socket的关联关系
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
        userSocketM=new ConcurrentHashMap<MobileKey, SocketHandle>();
        rm=ReceiveMemory.getInstance();
        sm=SendMemory.getInstance();
        logQueue=new StrArrayQueue(10240);
    }

    /**
     * 清理发送内存结构，把没有数据的设备删除掉<br/>
     * 包括待发送列表和已发送列表
     * ConcurrentLinkedQueue<Message>
     */
    public void clean() {
        if (this.sm.msgMap!=null&&!this.sm.msgMap.isEmpty()) {
            for (String sKey: this.sm.msgMap.keySet()) {
                ConcurrentLinkedQueue<Message> mq = this.sm.msgMap.get(sKey);
                if (mq==null||mq.isEmpty()) this.sm.msgMap.remove(sKey);
            }
        }
        if (this.sm.msgSendedMap!=null&&!this.sm.msgSendedMap.isEmpty()) {
            for (String sKey: this.sm.msgSendedMap.keySet()) {
                SendMessageList sml = this.sm.msgSendedMap.get(sKey);
                if (sml==null||sml.size()==0) this.sm.msgSendedMap.remove(sKey);
            }
        }
    }

    /**
     * 根据设备标识MobileKey(mk)，获得发送消息体。<br/>
     * 若有更多需要回传的消息，需通过下次请求给出。
     * @param mk 设备标识
     * @return 消息体
     */
    public Message getSendMessages(MobileKey mk, SocketHandle s) {
        Message m=null;
        //从发送队列取一条消息
        boolean canRead=false;
        synchronized(userSocketLock) {
            canRead=s.equals(userSocketM.get(mk));
        }
        if (canRead) m=sm.pollQueue(mk);
        if (m!=null) {
            if (m.isAffirm()) {
//                try {
//                    sm.addSendedMsg(mk, m);
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                }
            }
        } else {/*
            SendMessageList hasSl = sm.getSendedMessagList(mk);
            if (hasSl.size()>0) {
                m=hasSl.get(0);
            }*/
        }
        return m;
    }
    /**
     * 根据设备标识MobileKey(mk)，获得发送消息体。<br/>
     * 若有更多需要回传的消息，需通过下次请求给出。
     * @param mk 设备标识
     * @return 消息体
     */
    public Message getSendMessages(MobileKey mk) {
        Message m=sm.pollQueue(mk);
        if (m!=null) {
            if (m.isAffirm()) {
//                try {
//                    sm.addSendedMsg(mk, m);
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                }
            }
        } else {/*
            SendMessageList hasSl = sm.getSendedMessagList(mk);
            if (hasSl.size()>0) {
                m=hasSl.get(0);
            }*/
        }
        return m;
    }

    public void setUserSocketMap(MobileKey mk, SocketHandle s) {
        synchronized(userSocketLock) {
            MobileKey s_mk=null;
            for (MobileKey _mk: userSocketM.keySet()) {
                if (userSocketM.get(_mk).equals(s)) {
                    s_mk=_mk;
                    break;
                }
            }
            if (s_mk!=null&&!s_mk.equals(mk)) {
                MobileSession ms=SessionMemoryManage.getInstance().getSession(s_mk);
                ms.expire();
            }

            SocketHandle _s=userSocketM.get(mk);
            if (!s.equals(_s)&&_s!=null) _s.stopHandle();
            userSocketM.put(mk, s);
        }
    }
    public void removeSocket(SocketHandle sh) {
        synchronized(userSocketLock) {
            for (MobileKey mk: userSocketM.keySet()) {
                if (userSocketM.get(mk).equals(sh)) {
                    userSocketM.remove(mk);
                    break;
                }
            }
        }
    }
    public void removeMk(MobileKey mk) {
        synchronized(userSocketLock) {
            userSocketM.remove(mk);
        }
    }
    /**
     * 根据用户标识userId，获得发送消息。<br/>
     * 若有更多需要回传的消息，需通过下次请求给出。
     * @param userId 用户标识
     * @return 消息体
     */
    public Message getNotifyMessages(String userId) {
        return sm.pollNotifyQueue(userId);
    }

    private AtomicBoolean serverRuning=new AtomicBoolean(false); //推送服务是否正常运行
    public boolean isServerRuning() {
        return this.serverRuning.get();
    }
    public void setServerRuning(boolean serverRuning) {
        this.serverRuning.set(serverRuning);
    }
}