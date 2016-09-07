package com.woting.appengine.mobile.push.mem;

 import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletContext;

import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.core.structure.StrArrayQueue;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.SessionService;
import com.woting.push.core.message.Message;
import com.woting.appengine.mobile.push.model.SendMessageList;
import com.woting.appengine.mobile.push.monitor.socket.SocketHandle;

public class PushMemoryManage {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static PushMemoryManage instance=new PushMemoryManage();
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
    private ConcurrentHashMap<MobileUDKey, SocketHandle> userSocketM;//用户和Socket的关联关系
    //数据区

    private SessionService sessionService=null;

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
        userSocketM=new ConcurrentHashMap<MobileUDKey, SocketHandle>();
        rm=ReceiveMemory.getInstance();
        sm=SendMemory.getInstance();
        logQueue=new StrArrayQueue(10240);
        //创建SessionService对象
        ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
        if (sc!=null&&WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
            sessionService=(SessionService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("redisSessionService");
        }
    }

    /**
     * 清理发送内存结构，把没有数据的设备删除掉<br/>
     * 包括待发送列表和已发送列表
     * ConcurrentLinkedQueue<Message>
     */
    public void clean() {
        if (this.sm.msgMap!=null&&!this.sm.msgMap.isEmpty()) {
            for (String sKey: this.sm.msgMap.keySet()) {
                ConcurrentLinkedQueue<Message> mq=this.sm.msgMap.get(sKey);
                if (mq==null||mq.isEmpty()) this.sm.msgMap.remove(sKey);
            }
        }
        if (this.sm.msgSendedMap!=null&&!this.sm.msgSendedMap.isEmpty()) {
            for (String sKey: this.sm.msgSendedMap.keySet()) {
                SendMessageList sml=this.sm.msgSendedMap.get(sKey);
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
    public Message getSendMessages(MobileUDKey mk, SocketHandle s) {
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
            SendMessageList hasSl=sm.getSendedMessagList(mk);
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
    public Message getSendMessages(MobileUDKey mk) {
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
            SendMessageList hasSl=sm.getSendedMessagList(mk);
            if (hasSl.size()>0) {
                m=hasSl.get(0);
            }*/
        }
        return m;
    }

    public void setUserSocketMap(MobileUDKey mUdk, SocketHandle s) {
        if (sessionService==null) {
            ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
            if (sc!=null&&WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                sessionService=(SessionService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("redisSessionService");
            }
        }
        synchronized(userSocketLock) {
            MobileUDKey s_mUdk=null;
            for (MobileUDKey _mUdk: userSocketM.keySet()) {
                if (userSocketM.get(_mUdk).equals(s)) {
                    s_mUdk=_mUdk;
                    break;
                }
            }
            if (s_mUdk!=null&&!s_mUdk.equals(mUdk)) {
                //修改RedisSession
//                MobileSession ms=SessionMemoryManage.getInstance().getSession(s_mk);
//                ms.expire();
                sessionService.logoutSession(mUdk);
            }

            SocketHandle _s=userSocketM.get(mUdk);
            if (!s.equals(_s)&&_s!=null) _s.stopHandle();
            userSocketM.put(mUdk, s);
        }
    }
    public void removeSocket(SocketHandle sh) {
        synchronized(userSocketLock) {
            for (MobileUDKey mUdk: userSocketM.keySet()) {
                if (userSocketM.get(mUdk).equals(sh)) {
                    userSocketM.remove(mUdk);
                    break;
                }
            }
        }
    }
    public void removeMk(MobileUDKey mUdk) {
        synchronized(userSocketLock) {
            userSocketM.remove(mUdk);
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