package com.woting.appengine.mobile.push.monitor.socket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
//import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import javax.servlet.ServletContext;

import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.util.DateUtils;
import com.spiritdata.framework.util.JsonUtils;
//import com.spiritdata.framework.util.FileNameUtils;
//import com.spiritdata.framework.util.JsonUtils;
//import com.spiritdata.framework.util.SequenceUUID;
//import com.spiritdata.framework.util.StringUtils;
//import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.mobile.mediaflow.mem.TalkMemoryManage;
import com.woting.appengine.mobile.mediaflow.model.TalkSegment;
import com.woting.appengine.mobile.mediaflow.model.WholeTalk;
import com.woting.push.core.message.Message;
import com.woting.push.core.message.MessageUtils;
import com.woting.push.core.message.MsgMedia;
import com.woting.push.core.message.MsgNormal;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.SessionService;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;

/**
 * 处理Socket的线程，此线程是处理一个客户端连接的基础线程。其中包括一个主线程，两个子线程<br/>
 * [主线程(监控本连接的健康状况)]:<br/>
 * 启动子线程，并监控Socket连接的健康状况<br/>
 * [子线程(处理业务逻辑)]:<br/>
 * 发送|正常消息：对应的消息队列中有内容，就发送<br/>
 * 接收|正常+心跳：判断是否连接正常的逻辑也在这个现成中<br/>
 */
//注意，服务端把心跳的功能去掉了
public class SocketHandle extends Thread {
    private FetchMsg fetchMsg;
    private SendMsg sendMsg;
    private ReceiveMsg receiveMsg;
    private DealByteArray dealByteArray;

    private BufferedInputStream socketIn=null;
    private BufferedOutputStream socketOut=null;//若是确认消息才用得到

    //控制信息
    protected SocketMonitorConfig smc=null;//套接字监控配置
    protected volatile Object socketSendLock=new Object();
    protected volatile boolean running=true;
    protected long lastVisitTime=System.currentTimeMillis();

    protected ArrayBlockingQueue<Byte> receiveByteQueue=new ArrayBlockingQueue<Byte>(10240);
    protected ArrayBlockingQueue<byte[]> sendMsgQueue=new ArrayBlockingQueue<byte[]>(512);

    //数据
    protected Socket socket=null;
    protected String socketDesc;
    protected MobileUDKey mUdk=null;

    //内存数据
    private PushMemoryManage pmm=PushMemoryManage.getInstance();

    private SessionService sessionService=null;

    //判断Socket是否起作用
    private boolean socketOk() {
        if (System.currentTimeMillis()-lastVisitTime>this.smc.calculate_TimeOut()) {
            long t=System.currentTimeMillis();
            System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"超时关闭");
            return false;
        }
        return socket!=null&&socket.isBound()&&socket.isConnected()&&!socket.isClosed();
    }

    /**
     * 构造函数，同时线程注册到Map中。
     * @param client 客户端Socket
     */
    public SocketHandle(Socket client, SocketMonitorConfig smc) throws Exception {
        this.socket=client;
        this.socket.setTcpNoDelay(true);
        socketIn=new BufferedInputStream(socket.getInputStream());
        socketOut=new BufferedOutputStream(socket.getOutputStream());
        this.smc=smc;
        //创建SessionService对象
        ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
            sessionService=(SessionService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("redisSessionService");
        }
    }
    public void stopHandle() {
        running=false;
    }
    private void closeHandle() {
        if (this.fetchMsg!=null) {try {this.fetchMsg._interrupt();} catch(Exception e) {}}
        if (this.sendMsg!=null) {try {this.sendMsg._interrupt();} catch(Exception e) {}}
        if (this.receiveMsg!=null) {try {this.receiveMsg._interrupt();} catch(Exception e) {}}
        if (this.dealByteArray!=null) {try {this.dealByteArray._interrupt();} catch(Exception e) {}}
        boolean canClose=false;
        int loopCount=0;
        while(!canClose) {
            loopCount++;
            if (loopCount>this.smc.get_TryDestoryAllCount()) {
                canClose=true;
                continue;
            }
            if (/* (this.sendBeat!=null&&!this.sendBeat.isRunning)
               &&*/(this.fetchMsg!=null&&!this.fetchMsg.isRunning)
               &&(this.sendMsg!=null&&!this.sendMsg.isRunning)
               &&(this.receiveMsg!=null&&!this.receiveMsg.isRunning)
               &&(this.dealByteArray!=null&&!this.dealByteArray.isRunning)) {
                canClose=true;
                continue;
            }
            try { sleep(10); } catch (InterruptedException e) {};
        }
        if (this.fetchMsg!=null) this.fetchMsg.interrupt();
        if (this.sendMsg!=null) this.sendMsg.interrupt();
        if (this.receiveMsg!=null) this.receiveMsg.interrupt();
        if (this.dealByteArray!=null) this.dealByteArray.interrupt();
        try {
            this.socketIn.close();
        } catch (IOException e) {
        } finally {
            this.socketIn=null;
        }
        try {
            this.socketOut.close();
        } catch (IOException e1) {
        } finally {
            this.socketOut=null;
        }
        try {
            SocketHandle.this.socket.close(); //这是主线程的关键
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pmm.removeSocket(this);
            this.fetchMsg=null;
            this.sendMsg=null;
            this.receiveMsg=null;
            this.dealByteArray=null;
            this.socket=null;
        }
    }
    /**
     * 主线程
     */
    public void run() {
        socketDesc="Socket["+socket.getRemoteSocketAddress()+",socketKey="+socket.hashCode()+"]";
        System.out.println("<"+(new Date()).toString()+">"+socketDesc+"主线程启动");
        //启动业务处理线程
        this.fetchMsg=new FetchMsg(socketDesc+"获取发送消息");
        this.fetchMsg.start();
        System.out.println("<"+(new Date()).toString()+">"+socketDesc+"获取发送消息线程启动");
        this.sendMsg=new SendMsg(socketDesc+"发送消息");
        this.sendMsg.start();
        System.out.println("<"+(new Date()).toString()+">"+socketDesc+"发送消息线程启动");
        this.receiveMsg=new ReceiveMsg(socketDesc+"接收消息");
        this.receiveMsg.start();
        System.out.println("<"+(new Date()).toString()+">"+socketDesc+"接收消息线程启动");
        this.dealByteArray=new DealByteArray(socketDesc+"处理收到的字节流");
        this.dealByteArray.start();
        System.out.println("<"+(new Date()).toString()+">"+socketDesc+"接收消息线程启动");

        //主线程
        try {
            while (running) {//有任何一个字线程出问题，则关闭这个连接
                //判断时间戳，看连接是否还有效
                if (!socketOk()) {
                    this.fetchMsg._interrupt();
                    this.sendMsg._interrupt();
                    this.receiveMsg._interrupt();
                    this.dealByteArray._interrupt();
                    System.out.println("============NOSOCKETOK");
                    break;
                } else {
                    if (this.fetchMsg.isInterrupted||!this.fetchMsg.isRunning) {
                        this.sendMsg._interrupt();
                        this.receiveMsg._interrupt();
                        this.dealByteArray._interrupt();
                        System.out.println("============fetchMsg");
                        break;
                    }
                    if (this.sendMsg.isInterrupted||!this.sendMsg.isRunning) {
                        this.fetchMsg._interrupt();
                        this.receiveMsg._interrupt();
                        this.dealByteArray._interrupt();
                        System.out.println("============sendMsg");
                        break;
                    }
                    if (this.receiveMsg.isInterrupted||!this.receiveMsg.isRunning) {
                        this.fetchMsg._interrupt();
                        this.sendMsg._interrupt();
                        this.dealByteArray._interrupt();
                        System.out.println("============receiveMsg");
                        break;
                    }
                    if (this.dealByteArray.isInterrupted||!this.dealByteArray.isRunning) {
                        this.fetchMsg._interrupt();
                        this.sendMsg._interrupt();
                        this.receiveMsg._interrupt();
                        System.out.println("============dealByteArray");
                        break;
                    }
                }
                try { sleep(this.smc.get_MonitorDelay()); } catch (InterruptedException e) {};
            }
        } catch (Exception e) {//有异常就退出
            long t=System.currentTimeMillis();
            System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"主监控线程出现异常:"+e.getMessage());
            e.printStackTrace();
        } finally {//关闭所有子任务进程
            closeHandle();
        }
    }

    //=====================================================================================
    /*
     * 从总消息中获取本用户所需的消息的线程
     */
    class FetchMsg extends Thread {
        private boolean isInterrupted=false;
        private boolean isRunning=true;
        private boolean canAdd=false;
//        private String mStr="";
        protected FetchMsg(String name) {
            super.setName(name);
        }
        protected void _interrupt(){
            isInterrupted=true;
            this.interrupt();
            super.interrupt();
        }
        public void run() {
            this.isRunning=true;
            try {
                while (pmm.isServerRuning()&&SocketHandle.this.running&&!isInterrupted) {
                    if (SocketHandle.this.mUdk!=null) {
                        canAdd=true;
                        //获得控制消息
                        Message m=pmm.getSendMessages(mUdk, SocketHandle.this);
                        if (m!=null) {
                            long t=System.currentTimeMillis();
                            if (m instanceof MsgMedia) {
                                if (t-m.getSendTime()>60*1000) {
                                    canAdd=false;
                                }
                            }
                            if (canAdd) {
                                if (m!=null) {
                                    sendMsgQueue.add(m.toBytes());
                                }
                            }
                            if (m instanceof MsgMedia) {//对语音广播包做特殊处理
                                MsgMedia _mm=(MsgMedia)m;
                                try {
                                    TalkMemoryManage tmm=TalkMemoryManage.getInstance();
                                    WholeTalk wt=tmm.getWholeTalk(_mm.getTalkId());
                                    TalkSegment ts=wt.getTalkData().get(Math.abs(_mm.getSeqNo()));
                                    if (ts.getSendFlagMap().get(mUdk.toString())!=null) ts.getSendFlagMap().put(mUdk.toString(), 1);
                                } catch(Exception e) {e.printStackTrace();}
                            }
                        }
                        //获得通知类消息
                        if (mUdk.isUser()) {
                            Message nm=pmm.getNotifyMessages(mUdk.getUserId());
                            if (nm!=null) {
                                sendMsgQueue.add(nm.toBytes());
                            }
                        }
                    }
                    try { sleep(5); } catch (InterruptedException e) {};
                }
            } catch (Exception e) {
                e.printStackTrace();
                long t=System.currentTimeMillis();
                System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"获取发送消息线程出现异常:" + e.getMessage());
            } finally {
                this.isRunning=false;
            }
        }
    }
    /*
     * 发送消息线程
     */
    class SendMsg extends Thread {
        private boolean isInterrupted=false;
        private boolean isRunning=true;
        private byte[] mBytes=null;
        protected SendMsg(String name) {
            super.setName(name);
        }
        protected void _interrupt(){
            isInterrupted=true;
            this.interrupt();
            super.interrupt();
        }
        public void run() {
            String filePath="/opt/logs/sendLogs";
            File dir=new File(filePath);
            if (!dir.isDirectory()) dir.mkdirs();
            File f=new File(filePath+"/"+SocketHandle.this.socket.hashCode()+".log");
            FileOutputStream fos=null;
            try {
                if (!f.exists()) f.createNewFile();
                fos=new FileOutputStream(f, false);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            this.isRunning=true;
            try {
                while (pmm.isServerRuning()&&SocketHandle.this.running&&!isInterrupted&&socketOk()) {
                    try {
                        synchronized(socketSendLock) {
                            if (socketOut!=null&&!socket.isOutputShutdown()&&!sendMsgQueue.isEmpty()) {
                                mBytes=sendMsgQueue.poll();
                                if (mBytes==null||mBytes.length<=2) continue;
                                socketOut.write(mBytes);
                                socketOut.flush();
                                if (mBytes.length==3&&mBytes[0]=='b'&&mBytes[1]=='|'&&mBytes[2]=='^') continue;
                                try {
                                    //pmm.logQueue.add(System.currentTimeMillis()+"::send::"+(mUdk==null?"null":mUdk.toString())+"::"+(new String(mBytes)));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (fos!=null) {
                                    try {
                                        fos.write(mBytes);
                                        fos.write(13);
                                        fos.write(10);
                                        fos.flush();
                                    } catch (IOException e) {
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        long t=System.currentTimeMillis();
                        System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"发送消息线程出现异常:" + e.getMessage());
                    }
                }
            } catch (Exception e) {
                long t=System.currentTimeMillis();
                System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"发送消息线程出现异常:" + e.getMessage());
            } finally {
                this.isRunning=false;
                try { fos.close(); } catch (Exception e) {};
                fos=null;
            }
        }
    }

    /*
     * 接收消息+心跳线程
     */
    class ReceiveMsg extends Thread {
        private boolean isInterrupted=false;
        private boolean isRunning=true;
        private boolean canContinue=true;
        private int continueErrCodunt=0;
        private int sumErrCount=0;
        protected ReceiveMsg(String name) {
            super.setName(name);
        }
        protected void _interrupt(){
            isInterrupted=true;
            super.interrupt();
            this.interrupt();
        }
        public void run() {
            this.isRunning=true;
            try {
                //若总线程运行(并且)Socket处理主线程可运行(并且)本线程可运行(并且)本线程逻辑正确[未中断(并且)可以继续]
                while(pmm.isServerRuning()&&SocketHandle.this.running&&(!isInterrupted&&canContinue)) {
                    try {
                        int r;
                        while ((r=socketIn.read())!=-1) {
                            receiveByteQueue.add((byte)r);
                        }
                        continueErrCodunt=0;
                    } catch(Exception e) {
                        e.printStackTrace();
                        long t=System.currentTimeMillis();
                        System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"接收消息线程出现异常:"+e.getMessage());
                        if (e instanceof SocketException) {
                            canContinue=false;
                        } else {
                            if ( (++continueErrCodunt>=SocketHandle.this.smc.get_RecieveErr_ContinueCount())
                                ||(++sumErrCount>=SocketHandle.this.smc.get_RecieveErr_SumCount() )) {
                                 canContinue=false;
                             }
                        }
                    }//end try
                    try { sleep(10); } catch (InterruptedException e) {};
                }//end while
            } catch(Exception e) {
                e.printStackTrace();
                long t=System.currentTimeMillis();
                System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"接收消息线程出现异常:" + e.getMessage());
            } finally {
                this.isRunning=false;
            }
        }
    }

    /*
     * 处理收到的字节流
     */
    class DealByteArray extends Thread {
        private boolean isInterrupted=false;
        private boolean isRunning=true;
        private int _headLen=36;
        protected DealByteArray(String name) {
            super.setName(name);
        }
        protected void _interrupt(){
            isInterrupted=true;
            super.interrupt();
            this.interrupt();
        }
        public void run() {
            String filePath="/opt/logs/receiveLogs";
            File dir=new File(filePath);
            if (!dir.isDirectory()) dir.mkdirs();
            File f=new File(filePath+"/"+SocketHandle.this.socket.hashCode()+".log");
            FileOutputStream fos=null;
            try {
                if (!f.exists()) f.createNewFile();
                fos=new FileOutputStream(f, false);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            this.isRunning=true;
            byte[] ba=new byte[2048];
            byte[] mba=null;

            int i=0;
            short _dataLen=-3;
            boolean hasBeginMsg=false; //是否开始了一个消息
            int isAck=-1;
            int isRegist=0;
            int msgType=-1;//消息类型
            byte[] endMsgFlag={0x00,0x00,0x00};

            try {
                while(true) {
                    int r=-1;
                    while (true) {
                        r=receiveByteQueue.take();
                        if (fos!=null) fos.write(r);
                        ba[i++]=(byte)r;
                        endMsgFlag[0]=endMsgFlag[1];
                        endMsgFlag[1]=endMsgFlag[2];
                        endMsgFlag[2]=(byte)r;

                        if (!hasBeginMsg) {
                            if (endMsgFlag[0]=='b'&&endMsgFlag[1]=='^'&&endMsgFlag[2]=='^') {
                                break;
                            } else if ((endMsgFlag[0]=='|'&&endMsgFlag[1]=='^')||(endMsgFlag[0]=='^'&&endMsgFlag[1]=='|')) {
                                hasBeginMsg=true;
                                ba[0]=endMsgFlag[0];
                                ba[1]=endMsgFlag[1];
                                ba[2]=endMsgFlag[2];
                                i=3;
                                continue;
                            } else if ((endMsgFlag[1]=='|'&&endMsgFlag[2]=='^')||(endMsgFlag[1]=='^'&&endMsgFlag[2]=='|')) {
                                hasBeginMsg=true;
                                ba[0]=endMsgFlag[1];
                                ba[1]=endMsgFlag[2];
                                i=2;
                                continue;
                            }
                            if (i>2) {
                                for (int n=1;n<=i;n++) ba[n-1]=ba[n];
                                --i;
                            }
                        } else {
                            if (msgType==-1) msgType=MessageUtils.decideMsg(ba);
                            if (msgType==0) {//0=控制消息(一般消息)
                                if (isAck==-1&&i==12) {
                                    if (((ba[2]&0x80)==0x80)&&((ba[2]&0x00)==0x00)&&((ba[i-1]&0xF0)==0x00)) isAck=1; else isAck=0;
                                    if ((ba[i-1]&0xF0)==0xF0) isRegist=1;
                                } else  if (isAck==1) {//是回复消息
                                    if (isRegist==1) { //是注册消息
                                        if (i==48&&endMsgFlag[2]==0) _dataLen=80; else _dataLen=91;
                                        if (_dataLen>=0&&i==_dataLen) break;
                                    } else { //非注册消息
                                        if (_dataLen<0) _dataLen=45;
                                        if (_dataLen>=0&&i==_dataLen) break;
                                    }
                                } else  if (isAck==0) {//是一般消息
                                    if (isRegist==1) {//是注册消息
                                        if (((ba[2]&0x80)==0x80)&&((ba[2]&0x00)==0x00)) {
                                            if (i==48&&endMsgFlag[2]==0) _dataLen=80; else _dataLen=91;
                                        } else {
                                            if (i==47&&endMsgFlag[2]==0) _dataLen=79; else _dataLen=90;
                                        }
                                        if (_dataLen>=0&&i==_dataLen) break;
                                    } else {//非注册消息
                                        if (_dataLen==-3&&endMsgFlag[1]=='^'&&endMsgFlag[2]=='^') _dataLen++;
                                        else if (_dataLen>-3&&_dataLen<-1) _dataLen++;
                                        else if (_dataLen==-1) {
                                            _dataLen=(short)(((endMsgFlag[2]<<8)|endMsgFlag[1]&0xff));
                                            if (_dataLen==0) break;
                                        } else if (_dataLen>=0) {
                                            if (--_dataLen==0) break;
                                        }
                                    }
                                }
                            } else if (msgType==1) {//1=媒体消息
                                if (isAck==-1) {
                                    if (((ba[2]&0x80)==0x80)&&((ba[2]&0x40)==0x00)) isAck=1; else isAck=0;
                                } else if (isAck==1) {//是回复消息
                                    if (i==_headLen+1) break;
                                } else if (isAck==0) {//是一般媒体消息
                                    if (i==_headLen+2) _dataLen=(short)(((ba[_headLen+1]<<8)|ba[_headLen]&0xff));
                                    if (_dataLen>=0&&i==_dataLen+_headLen+2) break;
                                }
                            }
                        }
                    }
                    fos.flush();
                    mba=Arrays.copyOfRange(ba, 0, i);

                    i=0;
                    _dataLen=-3;
                    hasBeginMsg=false;
                    isAck=-1;
                    isRegist=0;
                    msgType=-1;
                    endMsgFlag[0]=0x00;
                    endMsgFlag[1]=0x00;
                    endMsgFlag[2]=0x00;

                    long t=System.currentTimeMillis();
                    if (mba==null||mba.length<3) continue;
                    SocketHandle.this.lastVisitTime=t;
                    //判断是否是心跳信号
                    if (mba.length==3&&mba[0]=='b'&&mba[1]=='^'&&mba[2]=='^') { //发送回执心跳
                        byte[] rB=new byte[3];
                        rB[0]='B';
                        rB[1]='^';
                        rB[2]='^';
                        sendMsgQueue.add(rB);
                        continue;
                    }

                    try {
                        String temp=SocketHandle.this.mUdk==null?"NULL":SocketHandle.this.mUdk.toString();
                        //pmm.logQueue.add(t+"::recv::"+temp+"::"+(new String(mba)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Message ms=null;
                        try {
                            ms=MessageUtils.buildMsgByBytes(mba);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println(JsonUtils.objToJson(ms));

                        if (ms!=null&&!ms.isAck()) {
                            if (ms instanceof MsgNormal) {
                                MobileUDKey _mUdk=MobileUDKey.buildFromMsg(ms);
                                //处理注册
                                Map<String, Object> retM=sessionService.dealUDkeyEntry(_mUdk, "socket/entry");
                                if (!(""+retM.get("ReturnType")).equals("1001")) {
                                    MsgNormal ackM=MessageUtils.buildAckMsg((MsgNormal)ms);
                                    ackM.setBizType(15);
                                    ackM.setPCDType(((MsgNormal)ms).getPCDType());
                                    ackM.setUserId(((MsgNormal)ms).getUserId());
                                    ackM.setIMEI(((MsgNormal)ms).getIMEI());
                                    ackM.setReturnType(0);//失败
                                    sendMsgQueue.add(ackM.toBytes());
                                } else {//登录成功
                                    _mUdk.setUserId(""+retM.get("UserId"));
                                    if (((MsgNormal)ms).getBizType()==15) {
                                        MsgNormal ackM=MessageUtils.buildAckMsg((MsgNormal)ms);
                                        ackM.setBizType(15);
                                        ackM.setPCDType(((MsgNormal)ms).getPCDType());
                                        ackM.setUserId(((MsgNormal)ms).getUserId());
                                        ackM.setIMEI(((MsgNormal)ms).getIMEI());
                                        ackM.setReturnType(1);//成功
                                        sendMsgQueue.add(ackM.toBytes());
                                    } else {
                                        SocketHandle.this.mUdk=_mUdk;
                                        if (SocketHandle.this.mUdk!=null) {//存入接收队列
                                            pmm.setUserSocketMap(SocketHandle.this.mUdk, SocketHandle.this);
                                            pmm.getReceiveMemory().addPureQueue(ms);
                                        }
                                    }
                                }
                            } else {//数据流
                                ((MsgMedia)ms).setExtInfo(SocketHandle.this.mUdk);
                                pmm.getReceiveMemory().addPureQueue(ms);
                            }
                        }
                    } catch(Exception e) {
                        System.out.println("==============================================================");
                        e.printStackTrace();
                        System.out.println("==============================================================");
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
                long t=System.currentTimeMillis();
                System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"字节流处理线程出现异常:" + e.getMessage());
            } finally {
                try { fos.close(); } catch (Exception e) {};
                fos=null;
                this.isRunning=false;
            }
        }
    }
}