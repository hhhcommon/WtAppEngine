package com.woting.appengine.mobile.push.monitor.socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Map;

import com.spiritdata.framework.util.DateUtils;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.intercom.mem.GroupMemoryManage;
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
    //三个业务子线程
//    private SendBeat sendBeat;
    private SendMsg sendMsg;
    private ReceiveMsg receiveMsg;

    //控制信息
    protected SocketMonitorConfig smc=null;//套接字监控配置
    protected volatile Object socketSendLock=new Object();
    protected volatile boolean running=true;
    protected long lastVisitTime=System.currentTimeMillis();

    //数据
    protected Socket socket=null;
    protected String socketDesc;
    protected MobileKey mk=null;

    //内存数据
    private PushMemoryManage pmm=PushMemoryManage.getInstance();

    /*
     * 构造函数，同时线程注册到Map中。
     * @param client 客户端Socket
     */
    public SocketHandle(Socket client, SocketMonitorConfig smc) {
        this.socket=client;
        this.smc=smc;
    }
    /*
     * 主线程
     */
    public void run() {
        socketDesc="Socket["+socket.getRemoteSocketAddress()+",socketKey="+socket.hashCode()+"]";
        System.out.println("<"+(new Date()).toString()+">"+socketDesc+"主线程启动");
        //启动业务处理线程
        this.sendMsg=new SendMsg(socketDesc+"发送消息");
        this.sendMsg.start();
        System.out.println("<"+(new Date()).toString()+">"+socketDesc+"发送消息线程启动");
        this.receiveMsg=new ReceiveMsg(socketDesc+"接收消息");
        this.receiveMsg.start();
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
                    break;
                }
                if (this.receiveMsg.isInterrupted||!this.receiveMsg.isRunning) {
                    this.sendMsg._interrupt();
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
                   &&(this.receiveMsg!=null&&!this.receiveMsg.isRunning) ) {
                    canClose=true;
                    continue;
                }
                try { sleep(10); } catch (InterruptedException e) {};
            }
            try {
                SocketHandle.this.socket.close(); //这是主线程的关键
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                this.sendMsg=null;
                this.receiveMsg=null;
                this.socket=null;
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
            PrintWriter out=null;
            this.isRunning=true;
            try {
                while (pmm.isServerRuning()&&SocketHandle.this.running&&!isInterrupted) {
                    try {
                        try { sleep(10); } catch (InterruptedException e) {};
                        //发消息
                        if (SocketHandle.this.mk!=null) {
                            //获得消息
                            Message m=pmm.getSendMessages(mk);
                            if (m!=null) {
                                //发送消息
                                synchronized(socketSendLock) {
                                    out=new PrintWriter(new BufferedWriter(new OutputStreamWriter(SocketHandle.this.socket.getOutputStream(), "UTF-8")), true);
                                    boolean canSend=true;
                                    //判断是否过期的语音包，这个比较特殊
                                    long t=System.currentTimeMillis();
                                    if (m.getMsgBizType().equals("AUDIOFLOW")) {
                                        if (t-m.getSendTime()>60*1000) {
                                            canSend=false;
                                            System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"[放弃发送语音消息]:<"+SocketHandle.this.mk.toString()+">\n\t"+m.toJson());
                                        }
                                    }
                                    if (canSend) {
                                        System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"[发送]:<"+SocketHandle.this.mk.toString()+">\n\t"+m.toJson());
                                        out.println(m.toJson());
                                        out.flush();
                                    }
                                    try { sleep(10); } catch (InterruptedException e) {};//给10毫秒的延迟
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
                try {
                    if (out!=null) try {out.close();out=null;} catch(Exception e) {out=null;throw e;};
                } catch(Exception e) {
                    e.printStackTrace();
                }
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
            BufferedReader in=null;
            PrintWriter out=null;//若是确认消息才用得到
            this.isRunning=true;
            try {
                //若总线程运行(并且)Socket处理主线程可运行(并且)本线程可运行(并且)本线程逻辑正确[未中断(并且)可以继续]
                while(pmm.isServerRuning()&&SocketHandle.this.running&&(!isInterrupted&&canContinue)) {
                    try {
                        try { sleep(10); } catch (InterruptedException e) {};
                        //String msgId=null;
                        //boolean isAffirm=false;
                        MobileKey mk=null;
                        //接收消息数据
                        in=new BufferedReader(new InputStreamReader(SocketHandle.this.socket.getInputStream(), "UTF-8"));
                        String revMsgStr=in.readLine();
                        if (revMsgStr==null) continue;
                        long t=System.currentTimeMillis();
                        System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"[接收]:<"+SocketHandle.this.mk.toString()+">\n\t"+revMsgStr);

                        SocketHandle.this.lastVisitTime=t;
                        //判断是否是心跳信号
                        if (revMsgStr.equals("b")) { //发送回执心跳
                            synchronized(socketSendLock) {
                                out=new PrintWriter(new BufferedWriter(new OutputStreamWriter(SocketHandle.this.socket.getOutputStream(), "UTF-8")), true);
                                out.println("B");
                                out.flush();
                                try { sleep(10); } catch (InterruptedException e) {};//给10毫秒的延迟
                            }
                            //以下设置lastTime.要删除掉
                            if (SocketHandle.this.mk!=null) {
                                GroupMemoryManage gmm=GroupMemoryManage.getInstance();
                                gmm.setLastTalkTime(SocketHandle.this.mk);
                            }
                            continue;
                        }

                        //以下try要修改
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> recMap=(Map<String, Object>)JsonUtils.jsonToObj(revMsgStr, Map.class);
                            if (recMap!=null&&recMap.size()>0) {
                                Map<String, Object> retM = MobileUtils.dealMobileLinked(recMap, 1);
                                if ((""+retM.get("ReturnType")).equals("2003")) {
                                    String outStr="[{\"MsgId\":\""+SequenceUUID.getUUIDSubSegment(4)+"\",\"ReMsgId\":\""+recMap.get("MsgId")+"\",\"BizType\":\"NOLOG\"}]";//空，无内容包括已经收到
                                    synchronized(socketSendLock) {
                                        out=new PrintWriter(new BufferedWriter(new OutputStreamWriter(SocketHandle.this.socket.getOutputStream(), "UTF-8")), true);
                                        out.println(outStr);
                                        out.flush();
                                        try { sleep(10); } catch (InterruptedException e) {};//给10毫秒的延迟
                                    }
                                } else {
                                    mk=MobileUtils.getMobileKey(recMap);
                                    if (mk!=null) { //存入接收队列
                                        SocketHandle.this.mk=mk;//设置全局作用域下的移动Key
                                        //处理注册
                                        if (!(recMap.get("BizType")+"").equals("REGIST")) pmm.getReceiveMemory().addPureQueue(recMap);
                                        //处理每次的
                                    }
                                }
                                //发送回执
//                                String outStr="[{\"returnType\":\"-1\"}]";//空，无内容包括已经收到
//                                if (isAffirm) {
//                                    if (StringUtils.isNullOrEmptyOrSpace(msgId)) {
//                                        outStr="[{\"returnType\":\"-2\"}]";//错误内容
//                                    } else {
//                                        outStr="[{\"returnType\":\"0\",\"data\":{\"MsgId\":\""+msgId+"\",\"dealFlag\":\"1\"}]}";//消息Id为msgId的消息已经处理，处理环节为：已收到
//                                    }
//                                }
//                                synchronized(socketSendLock) {
//                                    out=new PrintWriter(new BufferedWriter(new OutputStreamWriter(SocketHandle.this.socket.getOutputStream(), "UTF-8")), true);
//                                    out.println(outStr);
//                                    out.flush();
//                                    try { sleep(10); } catch (InterruptedException e) {};//给10毫秒的延迟
//                                }
                            }
                        } catch(Exception e) {
                            System.out.println("==============================================================");
                            System.out.println("EXCEPTIOIN::"+e.getClass().getName()+"/t"+e.getMessage());
                            System.out.println("JSONERROR::"+revMsgStr);
                            System.out.println("==============================================================");
                        }
                        continueErrCodunt=0;
                    } catch(Exception e) {
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
                }//end while
            } catch(Exception e) {
                long t=System.currentTimeMillis();
                System.out.println("<{"+t+"}"+DateUtils.convert2LocalStr("yyyy-MM-dd HH:mm:ss:SSS", new Date(t))+">"+socketDesc+"接收消息线程出现异常:" + e.getMessage());
            } finally {
                try {
                    if (in!=null) try {in.close();in=null;} catch(Exception e) {in=null;};
                    if (out!=null) try {out.close();out=null;} catch(Exception e) {out=null;};
                } catch(Exception e) {
                    e.printStackTrace();
                }
                this.isRunning=false;
            }
        }
    }
}