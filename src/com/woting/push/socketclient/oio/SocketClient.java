package com.woting.push.socketclient.oio;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spiritdata.framework.util.DateUtils;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.push.core.message.Message;
import com.woting.push.core.message.MessageUtils;
import com.woting.push.core.message.MsgNormal;
import com.woting.push.socketclient.SocketClientConfig;

public class SocketClient {
    private Logger logger=LoggerFactory.getLogger(this.getClass());

    private SocketClientConfig scc; //客户端配置

    private volatile Socket socket=null;
    private BufferedInputStream socketIn=null;
    private BufferedOutputStream socketOut=null;
    FileOutputStream sendLogFile=null;
    FileOutputStream recvLogFile=null;

    private ConcurrentLinkedQueue<byte[]> sendMsgQueue; //要发送的消息队列

    private int nextReConnIndex; //重连策略下一个执行序列;

    private volatile boolean toBeStop=false;
    private volatile boolean isRegister=false;//是否注册了
    private volatile long lastReceiveTime; //最后收到服务器消息时间
    private volatile Object socketSendLock=new Object();//发送锁

//以下对外接口：begin
    public SocketClient(SocketClientConfig scc) {
        nextReConnIndex=0;
        this.scc=scc;
        sendMsgQueue=new ConcurrentLinkedQueue<byte[]>();//初始化传送队列
    }
    /**
     * 设置当前重连策略的Index，通过这个方法提供一个更灵活的设置重连策略
     * @param index 序号
     */
    public void setNextReConnIndex(int index) {
        this.nextReConnIndex=index;
    }
    /**
     * 修改Beat的发送频率
     * @param intervalTime 新的发送频率
     */
    public void changeBeatCycle(long intervalTime) {
        scc.setIntervalBeat(intervalTime);
    }

    /**
     * 向消息发送队列增加一条要发送的消息
     * @param msg 要发送的消息
     * @throws Exception 
     */
    public void addSendMsg(Message msg) {
        try {
            if (msg instanceof MsgNormal) {
                ((MsgNormal)msg).setPCDType(3);
                ((MsgNormal)msg).setUserId("AppEngin");
                ((MsgNormal)msg).setIMEI("AppEngin001");//获得本机信息（CPU号）
            }
            sendMsgQueue.offer(msg.toBytes());
        } catch (Exception e) {
            logger.debug(StringUtils.getAllMessage(e));
        }
    }
//以上对外接口：end
    private boolean socketOk() {
        return socket!=null&&socket.isBound()&&socket.isConnected()&&!socket.isClosed();
    }

    private HealthWatch healthWatch; //健康检查线程
    private ReConn reConn; //重新连接线程
    private SendBeat sendBeat; //发送心跳线程
    private SendMsg sendMsg; //发送消息线程
    private ReceiveMsg receiveMsg; //结束消息线程

    /**
     * 开始工作：
     * 包括创建检测线程，并启动Socet连接
     */
    public void workStart() {
        toBeStop=false;
        lastReceiveTime=System.currentTimeMillis(); //最后收到服务器消息时间
        //连接
        healthWatch=new HealthWatch("Socket客户端长连接监控");
        healthWatch.setDaemon(true);
        healthWatch.start();
    }

    /**
     * 结束工作：包括关闭所有线程，但消息仍然存在
     */
    public void workStop() {
        toBeStop=true;
        long beginStopT=System.currentTimeMillis();
        while (System.currentTimeMillis()-beginStopT<scc.getStopDelay()) {
            if ((healthWatch!=null&&!healthWatch.isAlive())
              &&(reConn!=null&&!reConn.isAlive())
              &&(sendBeat!=null&&!sendBeat.isAlive())
              &&(sendMsg!=null&&!sendMsg.isAlive())
              &&(receiveMsg!=null&&!receiveMsg.isAlive())) break;
        }

        if (healthWatch!=null&&healthWatch.isAlive()) {
            healthWatch.interrupt();
            healthWatch=null;
        }
        if (reConn!=null&&reConn.isAlive()) {
            reConn.interrupt();
            reConn=null;
        }
        if (sendBeat!=null&&sendBeat.isAlive()) {
            sendBeat.interrupt();
            sendBeat=null;
        }
        if (sendMsg!=null&&sendMsg.isAlive()) {
            sendMsg.interrupt();
            sendMsg=null;
        }
        if (receiveMsg!=null&&receiveMsg.isAlive()) {
            receiveMsg.interrupt();
            receiveMsg=null;
        }

        try { socket.shutdownInput(); } catch (Exception e) {};
        try { socket.shutdownOutput(); } catch (Exception e) {};
        try { socket.close(); } catch (Exception e) {};

        if (sendLogFile!=null) try { sendLogFile.close(); } catch (Exception e) {} finally{ sendLogFile=null; };
        if (recvLogFile!=null) try { recvLogFile.close(); } catch (Exception e) {} finally{ recvLogFile=null; };

        socket=null;
        isRegister=false;
    }

    /*
     * 处理接收到的消息
     * @param msg 消息内容
     */
    private void setReceiver(Message msg) {
        //TODO 
    }

    //以下子进程=====================================================================================
    //健康监控线程
    private class HealthWatch extends Thread {
        protected HealthWatch(String name) {
            super.setName(name);
        }
        public void run() { //主线程监控连接
            logger.debug("<"+(new Date()).toString()+">"+this.getName()+"线程启动");
            try {
                while (true) {//检查线程的健康状况
                    if (toBeStop) break;
                    if (reConn==null||!reConn.isAlive()) {
                        if (!socketOk()||(System.currentTimeMillis()-lastReceiveTime>scc.getExpireTime())) {//连接失败了
                            workStop();
                            reConn=new ReConn("重连", nextReConnIndex);//此线程在健康监护线程中启动
                            reConn.setDaemon(true);
                            reConn.start();
                        }
                    }
                    try { sleep(scc.getIntervalCheckSocket()); } catch (InterruptedException e) {}
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    //重新连接线程
    private class ReConn extends Thread {
        private long curReConnIntervalTime;//当前重连间隔次数;
        private int nextReConnIndex; //当前重连策略序列;
        protected ReConn(String name, int nextReConnIndex) {
            super.setName(name);
            this.nextReConnIndex=nextReConnIndex;
            String s=scc.getReConnectIntervalTimeAndNextIndex(this.nextReConnIndex);
            String[] _s=s.split("::");
            nextReConnIndex=Integer.parseInt(_s[0]);
            curReConnIntervalTime=Integer.parseInt(_s[1]);
        }
        public void run() {
            logger.debug(this.getName()+"线程启动");
            if (sendBeat!=null) try {sendBeat.interrupt();} catch(Exception e) {}
            if (sendMsg!=null) try {sendMsg.interrupt();} catch(Exception e) {}
            if (receiveMsg!=null) try {receiveMsg.interrupt();} catch(Exception e) {}
            try {sleep(100);} catch(Exception e) {}
            sendBeat=null;
            sendMsg=null;
            receiveMsg=null;

            int i=0;
            while (true) {//重连部分
                if (toBeStop||socketOk()) break;
                try {
                    lastReceiveTime=System.currentTimeMillis(); //最后收到服务器消息时间

                    logger.info("【"+(new Date()).toString()+":"+System.currentTimeMillis()+"】连接("+(i++)+");"+nextReConnIndex+"::"+curReConnIntervalTime);
                    try {
                        socket=new Socket(scc.getIp(), scc.getPort());
                    } catch (IOException e) {
                    }
                    if (socketOk()) {//连接成功
                        socketIn=new BufferedInputStream(socket.getInputStream());
                        socketOut=new BufferedOutputStream(socket.getOutputStream());
                        if (socketIn==null) throw new NullPointerException("输入流");
                        if (socketOut==null) throw new NullPointerException("输出流");
                        if (!StringUtils.isNullOrEmptyOrSpace(scc.getLogPath())) {//处理日志
                            File dir=new File(scc.getLogPath());
                            if (!dir.isDirectory()) dir.mkdirs();
                            File f=new File(scc.getLogPath()+"\\c_"+socket.hashCode()+"_send.log");
                            try {
                                if (!f.exists()) f.createNewFile();
                                sendLogFile=new FileOutputStream(f, true);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            f=new File(scc.getLogPath()+"\\c_"+socket.hashCode()+"_recv.log");
                            try {
                                if (!f.exists()) f.createNewFile();
                                recvLogFile=new FileOutputStream(f, true);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }

                        }

                        logger.info("【"+(new Date()).toString()+":"+System.currentTimeMillis()+"】重连成功("+(i-1)+");"+nextReConnIndex+"::"+curReConnIntervalTime);
                        lastReceiveTime=System.currentTimeMillis();

                        receiveMsg=new ReceiveMsg("接收消息");
                        receiveMsg.setDaemon(true);
                        receiveMsg.start();

                        sendBeat=new SendBeat("发送心跳");
                        sendBeat.setDaemon(true);
                        sendBeat.start();

                        sendMsg=new SendMsg("发消息");
                        sendMsg.setDaemon(true);
                        sendMsg.start();

                        break;//若连接成功了，则结束此进程
                    } else {//未连接成功
                        try { sleep(curReConnIntervalTime); } catch (InterruptedException e) {};//间隔策略时间
                        socket=null;
                        String s=scc.getReConnectIntervalTimeAndNextIndex(nextReConnIndex);
                        String[] _s=s.split("::");
                        nextReConnIndex=Integer.parseInt(_s[0]);
                        curReConnIntervalTime=Integer.parseInt(_s[1]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //发送心跳
    private class SendBeat extends Thread {
        protected SendBeat(String name) {
            super.setName(name);
        }
        public void run() {
            logger.debug(this.getName()+"线程启动");
            PrintWriter out=null;
            try {
                while (true) {
                    try {
                        if (toBeStop) break;
                        if (socketOk()) {
                            synchronized (socketSendLock) {
                                if (out==null) out=new PrintWriter(new BufferedWriter(new OutputStreamWriter(socketOut, "UTF-8")), true);
                                out.println("b^^");
                                out.flush();
                                logger.debug("Socket["+socket.hashCode()+"]发心跳:b");
                                if (sendLogFile!=null) {
                                    sendLogFile.write((DateUtils.convert2LongLocalStr(new Date())+"::>>b^^").getBytes());
                                    sendLogFile.write(13);
                                    sendLogFile.write(10);
                                    sendLogFile.flush();
                                }
                            }
                        }
                        try { sleep(scc.getIntervalBeat()); } catch (InterruptedException e) {}
                    } catch(Exception e) {
                    }
                }
            } finally {
                if (out!=null) {try {out.close();} catch(Exception e){} finally{out=null;} };
            }
        }
    }

    //发送消息线程
    private class SendMsg extends Thread {
        protected SendMsg(String name) {
            super.setName(name);
        }
        public void run() {
            logger.debug(this.getName()+"线程启动");
            BufferedOutputStream out=null;
            try {
                while (true) {
                    try {
                        if (toBeStop) break;
                        if (socketOk()) {
                            byte[] msg4Send=null;
                            if (!isRegister) {
                                synchronized (socketSendLock) {
                                    //构造注册消息
                                    MsgNormal registerMsg=new MsgNormal();
                                    registerMsg.setMsgType(0);
                                    registerMsg.setAffirm(0);
                                    registerMsg.setBizType(15);
                                    registerMsg.setCmdType(0);
                                    registerMsg.setCommand(0);
                                    registerMsg.setFromType(1);
                                    registerMsg.setToType(1);
                                    registerMsg.setPCDType(3);
                                    registerMsg.setUserId("AppEngin");
                                    registerMsg.setIMEI("AppEngin001");
                                    registerMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                                    registerMsg.setSendTime(System.currentTimeMillis());

                                    if (out==null) out=new BufferedOutputStream(socketOut);
                                    msg4Send=registerMsg.toBytes();
                                    out.write(msg4Send);
                                    out.flush();
                                    if (sendLogFile!=null) {
                                        sendLogFile.write((DateUtils.convert2LongLocalStr(new Date())+"::>>").getBytes());
                                        sendLogFile.write(msg4Send);
                                        sendLogFile.write(13);
                                        sendLogFile.write(10);
                                        sendLogFile.flush();
                                    }
                                }
                                isRegister=true;
                            }
                            msg4Send=sendMsgQueue.poll();
                            if (msg4Send==null) continue;
                            synchronized (socketSendLock) {
                                if (out==null) out=new BufferedOutputStream(socketOut);
                                out.write(msg4Send);
                                out.flush();
                                if (sendLogFile!=null) {
                                    sendLogFile.write((DateUtils.convert2LongLocalStr(new Date())+"::>>").getBytes());
                                    sendLogFile.write(msg4Send);
                                    sendLogFile.write(13);
                                    sendLogFile.write(10);
                                    sendLogFile.flush();
                                }
                            }
                        }
                    } catch(Exception e) {
                    }
                }
            } finally {
                if (out!=null) {try {out.close();} catch(Exception e){} finally{out=null;} };
            }
        }
    }

    //接收消息线程
    private class ReceiveMsg extends Thread {
        private int _headLen=36;
        protected ReceiveMsg(String name) {
            super.setName(name);
        }
        public void run() {
            byte[] ba=new byte[2048];
            byte[] mba=null;
            BufferedInputStream in=null;
            int i=0;
            short _dataLen=-3;
            boolean hasBeginMsg=false; //是否开始了一个消息
            int isAck=-1;
            int isRegist=0;
            int msgType=-1;//消息类型
            byte[] endMsgFlag={0x00,0x00,0x00};
            try {
                in=new BufferedInputStream(socket.getInputStream());
                while(true) {
                    if (recvLogFile!=null) {
                        long cur=System.currentTimeMillis();
                        sendLogFile.write((DateUtils.convert2LongLocalStr(new Date(cur))+"["+cur+"]::>>").getBytes());
                    }
                    if (toBeStop) break;
                    if (in==null) in=new BufferedInputStream(socket.getInputStream());
                    int r=-1;
                    while ((r=in.read())!=-1) {
                        if (recvLogFile!=null) recvLogFile.write(r);
                        ba[i++]=(byte)r;
                        endMsgFlag[0]=endMsgFlag[1];
                        endMsgFlag[1]=endMsgFlag[2];
                        endMsgFlag[2]=(byte)r;
                        if (!hasBeginMsg) {
                            if (endMsgFlag[0]=='B'&&endMsgFlag[1]=='^'&&endMsgFlag[2]=='^') {
                                break;//是心跳消息
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
                                    if (((ba[2]&0x80)==0x80)&&((ba[2]&0x00)==0x00)&&(((ba[i-1]&0xF0)==0x00)||((ba[i-1]&0xF0)==0xF0))) isAck=1; else isAck=0;
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
                    if (recvLogFile!=null) {
                        recvLogFile.write(13);
                        recvLogFile.write(10);
                        long cur=System.currentTimeMillis();
                        recvLogFile.write(("::<<"+DateUtils.convert2LongLocalStr(new Date(cur))+"["+cur+"]").getBytes());
                        recvLogFile.flush();
                    }//一条消息读取完成

                    //
                    mba=Arrays.copyOfRange(ba, 0, i);
                    lastReceiveTime=System.currentTimeMillis();
                    if (mba==null||mba.length<3) break; //若没有得到任何内容
                    if (ba[0]=='B'&&ba[1]=='^'&&ba[2]=='^') {
                        logger.debug("B======");
                    } else {
                        try {
                            Message ms=null;
                            try {
                                ms=MessageUtils.buildMsgByBytes(mba);
                                setReceiver(ms);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            logger.debug("收到:"+JsonUtils.objToJson(ms));
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                    endMsgFlag[0]=0x00;
                    endMsgFlag[1]=0x00;
                    endMsgFlag[2]=0x00;
                    i=0;
                    _dataLen=-3;
                    hasBeginMsg=false;
                    isAck=-1;
                    isRegist=0;
                    msgType=-1;
                }
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                if (in!=null) {try {in.close();} catch(Exception e){} finally{in=null;} };
            }
        }
    }
}