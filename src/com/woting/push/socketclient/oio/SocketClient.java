package com.woting.push.socketclient.oio;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
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
    private volatile boolean isClose=false;
    private volatile boolean isRegister=false;//是否注册了
    private volatile long lastReceiveTime; //最后收到服务器消息时间
    private volatile Object socketSendLock=new Object();//发送锁

//以下对外接口：begin
    public SocketClient(SocketClientConfig scc) throws Exception {
        nextReConnIndex=0;
        this.scc=scc;
        if (StringUtils.isNullOrEmptyOrSpace(scc.getServerType())||StringUtils.isNullOrEmptyOrSpace(scc.getServerType())) throw new Exception("未能获得服务器标识");
        sendMsgQueue=new ConcurrentLinkedQueue<byte[]>();//初始化传送队列
    }
    /**
     * 设置当前重连策略的Index，通过这个方法提供一个更灵活的设置重连策略
     * @param index 序号
     */
    public void setNextReConnIndex(int index) {
        nextReConnIndex=index;
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
                ((MsgNormal)msg).setPCDType(0);
                ((MsgNormal)msg).setUserId(scc.getServerType());
                ((MsgNormal)msg).setDeviceId(scc.getServerName());//获得本机信息（CPU号）
            }
            msg.setSendTime(System.currentTimeMillis());
            sendMsgQueue.offer(msg.toBytes());
        } catch (Exception e) {
            logger.debug(StringUtils.getAllMessage(e));
        }
    }

    /**
     * 开始工作：
     * 包括创建检测线程，并启动Socet连接
     */
    public void workStart() {
        lastReceiveTime=System.currentTimeMillis(); //最后收到服务器消息时间
        //连接
        healthWatch=new HealthWatch("Socket客户端长连接监控");
        healthWatch.start();
    }

    /**
     * 结束工作：包括关闭所有线程，但消息仍然存在
     * @throws IOException 
     */
    public void workStop() throws IOException {
        toBeStop=true;
        long beginStopT=System.currentTimeMillis();
        while (System.currentTimeMillis()-beginStopT<scc.getStopDelay()) {
            if ((healthWatch!=null&&!healthWatch.isAlive())
              &&(reConn!=null&&!reConn.isAlive())) break;
        }
        if (healthWatch!=null) try { healthWatch.interrupt(); } catch (Exception e) {} finally{ healthWatch=null; };
        if (reConn!=null) try { reConn.interrupt(); } catch (Exception e) {} finally{ reConn=null; };
        closeSocketAll();
    }
//以上对外接口：end

    private void closeSocketAll() throws IOException {
        isClose=true;

        long beginStopT=System.currentTimeMillis();
        while (System.currentTimeMillis()-beginStopT<scc.getStopDelay()) {
            if ((sendBeat!=null&&!sendBeat.isAlive())
              &&(sendMsg!=null&&!sendMsg.isAlive())
              &&(receiveMsg!=null&&!receiveMsg.isAlive())) break;
            if (sendBeat==null&&sendMsg==null&&receiveMsg==null) break;
        }

        if (receiveMsg!=null) try { receiveMsg.interrupt(); } catch (Exception e) {} finally{ receiveMsg=null; };
        if (sendBeat!=null) try { sendBeat.interrupt(); } catch (Exception e) {} finally{ sendBeat=null; };
        if (sendMsg!=null) try { sendMsg.interrupt(); } catch (Exception e) {} finally{ sendMsg=null; };

        if (sendLogFile!=null) try { sendLogFile.close(); } catch (Exception e) {} finally{ sendLogFile=null; };
        if (recvLogFile!=null) try { recvLogFile.close(); } catch (Exception e) {} finally{ recvLogFile=null; };

        if (socketIn!=null) try { socketIn.close(); } catch (Exception e) {} finally{ socketIn=null; };
        if (socketOut!=null) try { socketOut.close(); } catch (Exception e) {} finally{ socketOut=null; };

        if (socket!=null) {
            try { socket.shutdownInput(); } catch (Exception e) {};
            try { socket.shutdownOutput(); } catch (Exception e) {};
            try { socket.close(); } catch (Exception e) {};
            socket=null;
        }
        isRegister=false;

        isClose=false;
    }
    private boolean socketOk() {
        return socket!=null&&socket.isBound()&&socket.isConnected()&&!socket.isClosed();
    }

    private HealthWatch healthWatch; //健康检查线程
    private ReConn reConn; //重新连接线程
    private SendBeat sendBeat; //发送心跳线程
    private SendMsg sendMsg; //发送消息线程
    private ReceiveMsg receiveMsg; //结束消息线程

    /*
     * 处理接收到的消息
     * @param msg 消息内容
     */
    private void setReceiver(Message msg) {
        if (msg instanceof MsgNormal) {
            MsgNormal mn=(MsgNormal)msg;
            if (mn.getBizType()==15&&mn.isAck()&&mn.getReturnType()==0) {
                try {
                    this.workStop();
                    logger.debug("已有相同Id的服务器登录，关闭此服务器与消息中心的连接");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    //以下子进程=====================================================================================
    //健康监控线程
    private class HealthWatch extends Thread {
        protected HealthWatch(String name) {
            super.setName(name);
        }
        public void run() { //主线程监控连接
            logger.debug(this.getName()+"线程启动");
            while (!toBeStop) {//检查线程的健康状况
                try {
                    if (!socketOk()||(System.currentTimeMillis()-lastReceiveTime>scc.getExpireTime())) {//连接失败了
                        if (reConn==null||!reConn.isAlive()) {
                            closeSocketAll();
                            reConn=new ReConn("重连", nextReConnIndex);//此线程在健康监护线程中启动
                            reConn.setDaemon(true);
                            reConn.start();
                        }
                    }
                    try { sleep(scc.getIntervalCheckSocket()); } catch (InterruptedException e) {}
                } catch(Exception e) {
                    e.printStackTrace();
                }
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
                            File f=new File(scc.getLogPath()+File.separator+"c_"+socket.hashCode()+"_send.log");
                            try {
                                if (!f.exists()) f.createNewFile();
                                sendLogFile=new FileOutputStream(f, true);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            f=new File(scc.getLogPath()+File.separator+"c_"+socket.hashCode()+"_recv.log");
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
                    logger.debug(this.getName()+":"+StringUtils.getAllMessage(e));
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
            while (!toBeStop&&socketOk()&&!isClose) {
                try {
                    synchronized (socketSendLock) {
                        if (socketOut!=null&&!socket.isOutputShutdown()) {
                            //socketOut.write("b^^".getBytes());
                            socketOut.flush();
                        }
                        logger.debug("Socket["+socket.hashCode()+"]发心跳:b");
                        if (sendLogFile!=null) {
                            sendLogFile.write((DateUtils.convert2LongLocalStr(new Date())+"::>>b^^").getBytes());
                            sendLogFile.write(13);
                            sendLogFile.write(10);
                            sendLogFile.flush();
                        }
                    }
                    try { sleep(scc.getIntervalBeat()); } catch (InterruptedException e) {}
                } catch(Exception e) {
                    logger.debug(this.getName()+":"+StringUtils.getAllMessage(e));
                    if (e instanceof SocketException) {
                        try {
                            closeSocketAll();
                        } catch (IOException e1) {
                            break;
                        }
                    }
                }
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
            while (!toBeStop&&socketOk()&&!isClose) {
                try {
                    byte[] msg4Send=null;
                    if (socketOut!=null&&!socket.isOutputShutdown()&&!isRegister) {
                        synchronized (socketSendLock) {
                            //构造注册消息
                            MsgNormal registerMsg=new MsgNormal();
                            registerMsg.setMsgId(SequenceUUID.getPureUUID());
                            registerMsg.setMsgType(0);
                            registerMsg.setAffirm(0);
                            registerMsg.setBizType(15);
                            registerMsg.setCmdType(0);
                            registerMsg.setCommand(0);
                            registerMsg.setFromType(1);
                            registerMsg.setToType(1);
                            registerMsg.setPCDType(0);
                            registerMsg.setUserId(scc.getServerType());
                            registerMsg.setDeviceId(scc.getServerName());
                            registerMsg.setSendTime(System.currentTimeMillis());

                            msg4Send=registerMsg.toBytes();
                            socketOut.write(msg4Send);
                            socketOut.flush();
                            if (sendLogFile!=null) {
                                sendLogFile.write((DateUtils.convert2LongLocalStr(new Date())+"::>>").getBytes());
                                sendLogFile.write(msg4Send);
                                sendLogFile.write(13);
                                sendLogFile.write(10);
                                sendLogFile.flush();
                            }
                            isRegister=true;
                        }
                    }
                    msg4Send=sendMsgQueue.poll();
                    if (msg4Send==null) continue;
                    if (socketOut!=null&&!socket.isOutputShutdown()) {
                        synchronized (socketSendLock) {
                            socketOut.write(msg4Send);
                            socketOut.flush();
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
                    logger.debug(this.getName()+":"+StringUtils.getAllMessage(e));
                    if (e instanceof SocketException) {
                        try {
                            closeSocketAll();
                        } catch (IOException e1) {
                            break;
                        }
                    }
                }
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
            int i=0;
            short _dataLen=-3;
            boolean hasBeginMsg=false; //是否开始了一个消息
            int isAck=-1;
            int isRegist=0;
            int msgType=-1;//消息类型
            int isCtlAck=0, tempFlag=0, fieldFlag=0, countFlag=0;
            byte[] endMsgFlag={0x00,0x00,0x00};
            while(!toBeStop&&socketOk()&&!isClose) {
                try {
                    if (recvLogFile!=null) {
                        long cur=System.currentTimeMillis();
                        recvLogFile.write((DateUtils.convert2LongLocalStr(new Date(cur))+"["+cur+"]::>>").getBytes());
                    }
                    int r=-1;
                    while ((r=socketIn.read())!=-1) {
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
                                    tempFlag=i;
                                    if ((ba[2]&0x80)==0x80) isAck=1; else isAck=0;
                                    if (isAck==1) countFlag=1; else countFlag=0;

                                    if (((ba[i-1]>>4)&0x0F)==0x0F) isRegist=1;
                                    else
                                    if (((ba[i-1]>>4)|0x00)==0x00) isCtlAck=1;
                                }
                                if (isAck!=-1) {
                                    if (isCtlAck==1) {
                                        if (fieldFlag==0&&((endMsgFlag[1]=='|'&&endMsgFlag[2]=='|')||(i-tempFlag-countFlag)==32)) {
                                            countFlag=1;
                                            tempFlag=i;
                                            fieldFlag=1;
                                        } else
                                        if (fieldFlag==1&&(i-tempFlag)>countFlag&&(ba[i-countFlag]==0||(endMsgFlag[1]=='|'&&endMsgFlag[2]=='|')||(i-tempFlag-countFlag)==12)) {
                                            countFlag=0;
                                            tempFlag=i;
                                            fieldFlag=2;
                                        } else
                                        if (fieldFlag==2&&((endMsgFlag[1]=='|'&&endMsgFlag[2]=='|')||(i-tempFlag-countFlag)==32)) {
                                            break;//通用回复消息读取完毕
                                        }
                                    } else if (isRegist==1) {
                                        if (fieldFlag==0&&(i-tempFlag)>countFlag&&((endMsgFlag[1]=='|'&&endMsgFlag[2]=='|')||(i-tempFlag-countFlag)==32)) {
                                            countFlag=1;
                                            tempFlag=i;
                                            fieldFlag=1;
                                        } else
                                        if (fieldFlag==1&&(i-tempFlag)>countFlag&&((ba[i-countFlag]==0||(endMsgFlag[1]=='|'&&endMsgFlag[2]=='|')||(i-tempFlag-countFlag)==12))) {
                                            countFlag=0;
                                            tempFlag=i;
                                            fieldFlag=2;
                                        } else
                                        if (fieldFlag==2&&((endMsgFlag[1]=='|'&&endMsgFlag[2]=='|')||(i-tempFlag-countFlag)==32)) {
                                            break;//注册消息完成
                                        }
                                    } else { //一般消息
                                        if (fieldFlag==0&&endMsgFlag[1]=='^'&&endMsgFlag[2]=='^') {
                                            fieldFlag=1;
                                            tempFlag=0;
                                        } else
                                        if (fieldFlag==1&&(++tempFlag)==2) {
                                            _dataLen=(short)(((endMsgFlag[2]<<8)|endMsgFlag[1]&0xff));
                                            tempFlag=0;
                                            fieldFlag=2;
                                        } else
                                        if (fieldFlag==2&&(++tempFlag)==_dataLen) break;
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
                        long cur=System.currentTimeMillis();
                        recvLogFile.write(("<<::"+DateUtils.convert2LongLocalStr(new Date(cur))+"["+cur+"]").getBytes());
                        recvLogFile.write(13);
                        recvLogFile.write(10);
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
                                logger.debug("收到:"+JsonUtils.objToJson(ms));
                                setReceiver(ms);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
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
                    isCtlAck=0;tempFlag=0;fieldFlag=0;countFlag=0;
                } catch(Exception e) {
                    logger.debug(this.getName()+":"+StringUtils.getAllMessage(e));
                    if (e instanceof SocketException) {
                        try {
                            closeSocketAll();
                        } catch (IOException e1) {
                            break;
                        }
                    }
                }
            }
        }
    }
}