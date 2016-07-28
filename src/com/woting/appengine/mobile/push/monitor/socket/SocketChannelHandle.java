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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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
public class SocketChannelHandle extends Thread {
    private final String lineSeparator;
    //三个业务子线程
//    private SendBeat sendBeat;
    private SendMsg sendMsg;
    private ReceiveMsg receiveMsg;
    private DealByteArray dealByteArray;

//    private BufferedInputStream socketIn=null;
//    private PrintWriter socketOut=null;//若是确认消息才用得到

    //控制信息
    protected SocketMonitorConfig smc=null;//套接字监控配置
    protected volatile boolean running=true;
    protected long lastVisitTime=System.currentTimeMillis();

    protected ArrayBlockingQueue<Byte> receiveByteQueue=new ArrayBlockingQueue<Byte>(10240);
    //数据
    protected SocketChannel socketChannel=null;
    protected String socketDesc;
    protected MobileKey mk=null;
    private ByteBuffer recvBuffer;
    private ByteBuffer sendBuffer;
    protected volatile Object socketSendLock=new Object();

    //内存数据
    private PushMemoryManage pmm=PushMemoryManage.getInstance();

    /*
     * 构造函数，同时线程注册到Map中。
     * @param client 客户端Socket
     */
    public SocketChannelHandle(SocketChannel sc, SocketMonitorConfig smc) throws Exception {
        lineSeparator = java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));
        this.smc=smc;
        socketChannel=sc;
        socketChannel.socket().setTcpNoDelay(true);
//        socketIn=new BufferedInputStream(socket.getInputStream());
//        socketOut=new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
        recvBuffer=ByteBuffer.wrap(new byte[1024]);
    }
    /*
     * 主线程
     */
    public void run() {
        socketDesc="Socket["+socketChannel.socket().getRemoteSocketAddress()+",socketKey="+socketChannel.socket().hashCode()+"]";
        System.out.println("<"+(new Date()).toString()+">"+socketDesc+"主线程启动");
        //启动业务处理线程
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
            while (true) {//有任何一个字线程出问题，则关闭这个连接
                try { sleep(10); } catch (InterruptedException e) {};
                Thread.sleep(this.smc.get_MonitorDelay());
                //判断时间戳，看连接是否还有效
                if (System.currentTimeMillis()-lastVisitTime>this.smc.calculate_TimeOut()) {
                    long t=System.currentTimeMillis();
                    System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"超时关闭");
                    break;
                }
                if (this.sendMsg.isInterrupted||!this.sendMsg.isRunning) {
                    this.receiveMsg._interrupt();
                    this.dealByteArray._interrupt();
                    break;
                }
                if (this.receiveMsg.isInterrupted||!this.receiveMsg.isRunning) {
                    this.sendMsg._interrupt();
                    this.dealByteArray._interrupt();
                    break;
                }
                if (this.dealByteArray.isInterrupted||!this.dealByteArray.isRunning) {
                    this.sendMsg._interrupt();
                    this.receiveMsg._interrupt();
                    break;
                }
            }
        } catch (InterruptedException e) {//有异常就退出
            long t=System.currentTimeMillis();
            System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"主监控线程出现异常:"+e.getMessage());
            e.printStackTrace();
        } finally {//关闭所有子任务进程
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
                   &&*/(this.sendMsg!=null&&!this.sendMsg.isRunning)
                   &&(this.receiveMsg!=null&&!this.receiveMsg.isRunning)
                   &&(this.dealByteArray!=null&&!this.dealByteArray.isRunning)) {
                    canClose=true;
                    continue;
                }
                try { sleep(10); } catch (InterruptedException e) {};
            }
            try {
                try{socketChannel.socket().close();}catch(Exception e){};
                try{socketChannel.close();}catch(Exception e){};
            } finally {
                this.sendMsg=null;
                this.receiveMsg=null;
                this.dealByteArray=null;
                this.socketChannel=null;
            }
        }
    }

    //=====================================================================================
    /*
     * 发送消息线程
     */
    class SendMsg extends Thread {
        private boolean isInterrupted=false;
        private boolean isRunning=true;
        protected SendMsg(String name) {
            super.setName(name);
        }
        protected void _interrupt(){
            isInterrupted = true;
            this.interrupt();
            super.interrupt();
        }
        public void run() {
            this.isRunning=true;
            try {
                while (pmm.isServerRuning()&&SocketChannelHandle.this.running&&!isInterrupted) {
                    try {
                        try { sleep(10); } catch (InterruptedException e) {};
                        long t=System.currentTimeMillis();
                        //发消息
                        if (SocketChannelHandle.this.mk!=null) {
                            //获得消息
                            Message m=pmm.getSendMessages(mk);
                            String mStr="";
                            if (m!=null) {
                                mStr=m.toJson();
                                //发送消息
                                synchronized(socketSendLock) {
                                    boolean canSend=true;
                                    //判断是否过期的语音包，这个比较特殊
                                    if (m.getMsgBizType().equals("AUDIOFLOW")) {
                                        if (t-m.getSendTime()>60*1000) {
                                            canSend=false;
                                        }
                                    }
                                    if (canSend) {
                                        sendByChannel(mStr);
                                        try {
                                            pmm.logQueue.add(t+"::send::"+mk.toString()+"::"+mStr);
                                        } catch (Exception e) {
                                            e.printStackTrace();
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
                                    }
                                    try { sleep(10); } catch (InterruptedException e) {};//给10毫秒的延迟
                                }
                            }
                            //发送通知类消息
                            if (mk.isUser()) {
                                Message nm=pmm.getNotifyMessages(mk.getUserId());
                                if (nm!=null) {
                                    mStr=nm.toJson();
                                    nm.setToAddr(MobileUtils.getAddr(mk));
                                    synchronized(socketSendLock) {
                                        sendByChannel(mStr);
                                        try {
                                            pmm.logQueue.add(t+"::send::"+mk.toString()+"::"+mStr);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        try { sleep(10); } catch (InterruptedException e) {};//给10毫秒的延迟
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
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
            isInterrupted = true;
            super.interrupt();
            this.interrupt();
        }
        public void run() {
            this.isRunning=true;
            try {
                //若总线程运行(并且)Socket处理主线程可运行(并且)本线程可运行(并且)本线程逻辑正确[未中断(并且)可以继续]
                while(pmm.isServerRuning()&&SocketChannelHandle.this.running&&(!isInterrupted&&canContinue)) {
                    try {
                        int read=socketChannel.read(recvBuffer);
                        if (read>0) {
                            recvBuffer.flip();
                            while (recvBuffer.hasRemaining()) {
                                byte b=recvBuffer.get();
                                receiveByteQueue.add(b);
                            }
                            recvBuffer.clear();
                        }
                        continueErrCodunt=0;
                    } catch(Exception e) {
                        long t=System.currentTimeMillis();
                        System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"接收消息线程出现异常:"+e.getMessage());
                        if (e instanceof SocketException) {
                            canContinue=false;
                        } else {
                            if ( (++continueErrCodunt>=SocketChannelHandle.this.smc.get_RecieveErr_ContinueCount())
                                ||(++sumErrCount>=SocketChannelHandle.this.smc.get_RecieveErr_SumCount() )) {
                                 canContinue=false;
                             }
                        }
                    }//end try
                    try { sleep(10); } catch (InterruptedException e) {};
                }//end while
            } catch(Exception e) {
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
            isInterrupted = true;
            super.interrupt();
            this.interrupt();
        }
        public void run() {
            String filePath="/opt/logs/receiveLogs";
//            String filePath="c:/opt/logs/receiveLogs";
            File dir=new File(filePath);
            if (!dir.isDirectory()) dir.mkdirs();
            File f=new File(filePath+"/"+socketChannel.socket().hashCode()+".log");
            FileWriter fw = null;
            if (!f.exists()) {
                try {
                    f.createNewFile();
                    fw = new FileWriter(f, false);
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
                    if (sb.length()<=0) continue;

                    long t=System.currentTimeMillis();
                    SocketChannelHandle.this.lastVisitTime=t;
                    //判断是否是心跳信号
                    if (sb.toString().equals("b")) { //发送回执心跳
                        synchronized(socketSendLock) {
                            sendByChannel("B");
                            System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"[发送回执心跳]");
                            try { sleep(10); } catch (InterruptedException e) {};//给10毫秒的延迟
                        }
                        continue;
                    }

                    try {
                        String temp=SocketChannelHandle.this.mk==null?"NULL":SocketChannelHandle.this.mk.toString();
                        pmm.logQueue.add(t+"::recv::"+temp+"::"+sb);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> recMap=(Map<String, Object>)JsonUtils.jsonToObj(sb.toString(), Map.class);
                        if (recMap!=null&&recMap.size()>0) {
                            //处理注册
                            Map<String, Object> retM = MobileUtils.dealMobileLinked(recMap, 1);
                            if ((""+retM.get("ReturnType")).equals("2003")) {
                                String outStr="[{\"MsgId\":\""+SequenceUUID.getUUIDSubSegment(4)+"\",\"ReMsgId\":\""+recMap.get("MsgId")+"\",\"BizType\":\"NOLOG\"}]";//空，无内容包括已经收到
                                synchronized(socketSendLock) {
                                    sendByChannel(outStr);
                                    try { sleep(10); } catch (InterruptedException e) {};//给10毫秒的延迟
                                }
                            } else {
                                SocketChannelHandle.this.mk=MobileUtils.getMobileKey(recMap);
                                if (SocketChannelHandle.this.mk!=null) {//存入接收队列
                                    if (!(recMap.get("BizType")+"").equals("REGIST")) pmm.getReceiveMemory().addPureQueue(recMap);
                                }
                            }
                        }
                    } catch(Exception e) {
                        System.out.println("==============================================================");
                        System.out.println("EXCEPTIOIN::"+e.getClass().getName()+"/t"+e.getMessage());
                        System.out.println("JSONERROR::"+sb);
                        System.out.println("==============================================================");
                    }
                }
            } catch(Exception e) {
                long t=System.currentTimeMillis();
                System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"字节流处理线程出现异常:" + e.getMessage());
            } finally {
                this.isRunning=false;
            }
        }
    }
    private void sendByChannel(String msg) throws IOException {
        byte[] sendData=(msg+lineSeparator).getBytes();
        sendBuffer=ByteBuffer.wrap(sendData);
        while(sendBuffer.hasRemaining()){
            socketChannel.write(sendBuffer);
        }
        socketChannel.socket().getOutputStream().flush();
    }
}