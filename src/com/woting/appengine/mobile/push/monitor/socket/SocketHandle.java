package com.woting.appengine.mobile.push.monitor.socket;

import java.io.BufferedInputStream;
//import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
//import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import com.spiritdata.framework.util.DateUtils;
//import com.spiritdata.framework.util.FileNameUtils;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.SequenceUUID;
//import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.common.util.MobileUtils;
//import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.mobile.mediaflow.mem.TalkMemoryManage;
import com.woting.appengine.mobile.mediaflow.model.TalkSegment;
import com.woting.appengine.mobile.mediaflow.model.WholeTalk;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.appengine.mobile.push.model.Message;

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
    private PrintWriter socketOut=null;//若是确认消息才用得到

    //控制信息
    protected SocketMonitorConfig smc=null;//套接字监控配置
    protected volatile Object socketSendLock=new Object();
    protected volatile boolean running=true;
    protected long lastVisitTime=System.currentTimeMillis();

    protected ArrayBlockingQueue<Byte> receiveByteQueue=new ArrayBlockingQueue<Byte>(10240);
    protected ArrayBlockingQueue<String> sendMsgQueue=new ArrayBlockingQueue<String>(512);

    //数据
    protected Socket socket=null;
    protected String socketDesc;
    protected MobileKey mk=null;

    //内存数据
    private PushMemoryManage pmm=PushMemoryManage.getInstance();

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
        socketOut=new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
        this.smc=smc;
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
        this.socketOut.close();
        this.socketOut=null;
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
        private String mStr="";
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
                    if (SocketHandle.this.mk!=null) {
                        canAdd=true;
                        //获得控制消息
                        Message m=pmm.getSendMessages(mk, SocketHandle.this);
                        if (m==null) continue;
                        long t=System.currentTimeMillis();
                        if (m.getMsgBizType().equals("AUDIOFLOW")) {
                            if (t-m.getSendTime()>60*1000) {
                                canAdd=false;
                            }
                        }
                        if (canAdd) {
                            if (m!=null) {
                                mStr=m.toJson();
                                sendMsgQueue.add(mStr);
                            }
                        }
                        if (m.getMsgBizType().equals("AUDIOFLOW")&&m.getCommand().equals("b1")) {//对语音广播包做特殊处理
                            try {
                                String talkId=((Map)m.getMsgContent()).get("TalkId")+"";
                                String seqNum=((Map)m.getMsgContent()).get("SeqNum")+"";
                                TalkMemoryManage tmm=TalkMemoryManage.getInstance();
                                WholeTalk wt=tmm.getWholeTalk(talkId);
                                TalkSegment ts=wt.getTalkData().get(Math.abs(Integer.parseInt(seqNum)));
                                if (ts.getSendFlagMap().get(mk.toString())!=null) ts.getSendFlagMap().put(mk.toString(), 1);
                            } catch(Exception e) {e.printStackTrace();}
                        }
                        //获得通知类消息
                        if (mk.isUser()) {
                            Message nm=pmm.getNotifyMessages(mk.getUserId());
                            if (nm!=null) {
                                nm.setToAddr(MobileUtils.getAddr(mk));
                                mStr=nm.toJson();
                                sendMsgQueue.add(mStr);
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
        private String mStr="";
        protected SendMsg(String name) {
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
                while (pmm.isServerRuning()&&SocketHandle.this.running&&!isInterrupted&&socketOk()) {
                    try {
                        synchronized(socketSendLock) {
                            if (socketOut!=null&&!socketOut.checkError()&&!socket.isOutputShutdown()&&!sendMsgQueue.isEmpty()) {
                                mStr=sendMsgQueue.poll();
                                socketOut.println(mStr);
                                socketOut.flush();
                                if (!mStr.equals("B")) {
                                    try {
                                        pmm.logQueue.add(System.currentTimeMillis()+"::send::"+mk.toString()+"::"+mStr);
                                    } catch (Exception e) {
                                        e.printStackTrace();
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
                            receiveByteQueue.add((byte) r);
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
            FileWriter fw=null;
            if (!f.exists()) {
                try {
                    f.createNewFile();
                    fw=new FileWriter(f, false);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            this.isRunning=true;
            try {
                while(true) {
                    StringBuffer sb=new StringBuffer();
                    int r=-1;
                    while (true) {
                        r=receiveByteQueue.take();
                        if (fw!=null) fw.write((char)r);
                        if (r!=10&&r!=13) sb.append((char)r); else break;
                    }
                    fw.flush();
                    long t=System.currentTimeMillis();
                    if (sb.length()==0) continue;
                    SocketHandle.this.lastVisitTime=t;
                    //判断是否是心跳信号
                    if (sb.toString().equals("b")) { //发送回执心跳
                        sendMsgQueue.add("B");
                        continue;
                    }

                    try {
                        String temp=SocketHandle.this.mk==null?"NULL":SocketHandle.this.mk.toString();
                        pmm.logQueue.add(t+"::recv::"+temp+"::"+sb);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> recMap=(Map<String, Object>)JsonUtils.jsonToObj(sb.toString(), Map.class);
                        if (recMap!=null&&recMap.size()>0) {
                            //处理注册
                            Map<String, Object> retM=MobileUtils.dealMobileLinked(recMap, 1);
                            if ((""+retM.get("ReturnType")).equals("2003")) {
                                String outStr="[{\"MsgId\":\""+SequenceUUID.getUUIDSubSegment(4)+"\",\"ReMsgId\":\""+recMap.get("MsgId")+"\",\"BizType\":\"NOLOG\"}]";//空，无内容包括已经收到
                                sendMsgQueue.add(outStr);
                            } else {
                                SocketHandle.this.mk=MobileUtils.getMobileKey(recMap);
                                if (SocketHandle.this.mk!=null) {//存入接收队列
                                    pmm.setUserSocketMap(SocketHandle.this.mk, SocketHandle.this);
                                    if (!(recMap.get("BizType")+"").equals("REGIST")) pmm.getReceiveMemory().addPureQueue(recMap);
                                }
                            }
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                        System.out.println("==============================================================");
                        System.out.println("EXCEPTIOIN::"+e.getClass().getName()+"/t"+e.getMessage());
                        System.out.println("JSONERROR::"+sb);
                        System.out.println("==============================================================");
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
                long t=System.currentTimeMillis();
                System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"字节流处理线程出现异常:" + e.getMessage());
            } finally {
                this.isRunning=false;
            }
        }
    }
}