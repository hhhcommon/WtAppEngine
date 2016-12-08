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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
//import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.SequenceUUID;
//import com.spiritdata.framework.util.SequenceUUID;
import com.woting.push.core.message.Message;
import com.woting.push.core.message.MessageUtils;
import com.woting.push.core.message.MsgMedia;
//import com.woting.push.core.message.content.MapContent;
import com.woting.push.core.message.MsgNormal;

import com.woting.push.core.message.content.MapContent;
import com.woting.push.socketclient.SocketClientConfig;

public class SocketClient {
/**
 * 给小辛，这段注释可以删除
 * 其中的System.out.println，需要你自己处理
 * 其中Context context; //android 上下文，这个要自己恢复
 * 其中ReceiveMsg的具体处理要自己处理
 */
    private SocketClientConfig scc; //客户端配置
//  private Context context; //android 上下文，这个要自己恢复

    private int nextReConnIndex; //重连策略下一个执行序列;

    private volatile Socket socket=null;
    public int getSocketCode() {
        if (socket!=null) return this.socket.hashCode();
        else return 0;
    }

    private volatile boolean toBeStop=false;
    private volatile boolean isRunning=false;
    private volatile long lastReceiveTime; //最后收到服务器消息时间
    private volatile Object socketSendLock=new Object();//发送锁
    private volatile Object socketRecvLock=new Object();//接收锁

    private HealthWatch healthWatch; //健康检查线程
    private ReConn reConn; //重新连接线程

    private SendBeat sendBeat; //发送心跳线程
    private SendMsg sendMsg; //发送消息线程
    private ReceiveMsg receiveMsg; //结束消息线程

    private ConcurrentLinkedQueue<byte[]> sendMsgQueue; //要发送的消息队列

    //以下对外接口：begin
    public SocketClient(SocketClientConfig scc/*, Content context*/) {
        //this.context=context
        this.nextReConnIndex=0;

        this.scc=scc;
        //以下设置参数的方式，应该从参数scc中获取，scc应该读取一个配置文件
        this.scc=new SocketClientConfig();
        this.scc.setIp("localhost");
        //this.scc.ip="123.56.254.75";
        this.scc.setPort(16789);
//        this.scc.port=9966;

        this.scc.setIntervalBeat(30*1000);//20秒1次心跳
        this.scc.setExpireTime(60*1000);//20秒未收到服务器连接状态，则认为连接失败，这个数要大于intervalBeat
        this.scc.setIntervalCheckSocket(10*1000);//2秒检查一次Socket连接状态

        List<String> _l=new ArrayList<String>();//其中每个间隔要是0.5秒的倍数
        _l.add("INTE::500");  //第1次检测到未连接成功，隔5秒重连
        _l.add("INTE::1000"); //第2次检测到未连接成功，隔10秒重连
        _l.add("INTE::3000"); //第3次检测到未连接成功，隔30秒重连
        _l.add("INTE::6000"); //第4次检测到未连接成功，隔1分钟重连
        _l.add("GOTO::0");    //之后，调到第7步处理
        this.scc.setReConnectWays(_l);
        //以上设置结束

        sendMsgQueue=new ConcurrentLinkedQueue<byte[]>();//初始化传送队列
    }

    /**
     * 开始工作：
     * 包括创建检测线程，并启动Socet连接
     */
    public void workStart() {
        if (!isRunning) {
            toBeStop=false;
            isRunning=true;
            lastReceiveTime=System.currentTimeMillis(); //最后收到服务器消息时间
            //连接
            healthWatch=new HealthWatch("Socket客户端长连接监控");
            healthWatch.start();
        } else {
            this.workStop();
            this.workStart();//循环了，可能死掉
        }
    }

    /**
     * 结束工作：包括关闭所有线程，但消息仍然存在
     */
    public void workStop() {
        toBeStop=true;
        int i=0, limitCount=6000;//一分钟后退出
        while (this.healthWatch.isAlive()||this.reConn.isAlive()||this.sendBeat.isAlive()||this.sendMsg.isAlive()||this.receiveMsg.isAlive()) {
            try { Thread.sleep(10); } catch (InterruptedException e) {};
            if (i++>limitCount) break;
        }
        this.healthWatch=null;
        this.reConn=null;
        this.sendBeat=null;
        this.sendMsg=null;
        this.receiveMsg=null;

        try { socket.shutdownInput(); } catch (Exception e) {};
        try { socket.shutdownOutput(); } catch (Exception e) {};
        try { socket.close(); } catch (Exception e) {};
        socket=null;

        isRunning=false;
    }

    /**
     * 设置当前重连策略的Index，通过这个方法提供一个更灵活的设置重连策略
     * @param index 序号
     */
    public void setNextReConnIndex(int index) {
        this.nextReConnIndex=index;
    }

    /**
     * 向消息发送队列增加一条要发送的消息
     * @param msg 要发送的消息
     */
    public void addSendMsg(byte[] msg) {
        this.sendMsgQueue.offer(msg);
    }

    /**
     * 修改Beat的发送频率
     * @param intervalTime 新的发送频率
     */
    public void changeBeatCycle(long intervalTime) {
        this.scc.intervalBeat=intervalTime;
        this.sendBeat.interrupt();
    }
//以上对外接口：end

    /*
     * 处理接收到的消息
     * @param msg 消息内容
     */
    //小辛你自己处理
    private void setReceiver(String msg) {
//        Intent pushintent=new Intent("push_sever");
//        Bundle bundle=new Bundle();
//        bundle.putString("outmessage",outmessage);
//        pushintent.putExtras(bundle);
//        sendBroadcast(pushintent);
    }

    private boolean socketOk() {
        return socket!=null&&socket.isBound()&&socket.isConnected()&&!socket.isClosed();//&&netConnectOk();
    }
    //以下子进程=====================================================================================
    //健康监控线程
    private class HealthWatch extends Thread {
        protected HealthWatch(String name) {
            super.setName(name);
        }
        public void run() { //主线程监控连接
            System.out.println("<"+(new Date()).toString()+">"+this.getName()+"线程启动");
            try {
                while (true) {//检查线程的健康状况
                    //System.out.println("socketOk()=="+socketOk());
                    if (toBeStop) break;
                    if (reConn==null||!reConn.isAlive()) {
                        if (!socketOk()||(System.currentTimeMillis()-lastReceiveTime>scc.getExpireTime())) {//连接失败了
                            if (socket!=null) {
                                try { socket.shutdownInput(); } catch (Exception e) {};
                                try { socket.shutdownOutput(); } catch (Exception e) {};
                                try { socket.close(); } catch (Exception e) {};
                            }
                            socket=null;
                            reConn=new ReConn("重连", nextReConnIndex);//此线程在健康监护线程中启动
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
            this.nextReConnIndex=Integer.parseInt(_s[0]);
            this.curReConnIntervalTime=Integer.parseInt(_s[1]);
        }
        public void run() {
            System.out.println("<"+(new Date()).toString()+">"+this.getName()+"线程启动");
            try {sendBeat.interrupt();} catch(Exception e) {}
            try {sendMsg.interrupt();} catch(Exception e) {}
            try {receiveMsg.interrupt();} catch(Exception e) {}
            try {sleep(100);} catch(Exception e) {}
            sendBeat=null;
            sendMsg=null;
            receiveMsg=null;
            isRunning=false;

            int i=0;
            while (true) {//重连部分
                if (toBeStop||socketOk()) break;
                if (!socketOk()) {//重新连接
                    try {
                        isRunning=true;
                        lastReceiveTime=System.currentTimeMillis(); //最后收到服务器消息时间

                        System.out.println("【"+(new Date()).toString()+":"+System.currentTimeMillis()+"】连接("+(i++)+");"+this.nextReConnIndex+"::"+this.curReConnIntervalTime);
                        try {
                            socket=new Socket(scc.getIp(), scc.getPort());
                        } catch (IOException e) {
                        }
                        if (socketOk()) {//连接成功
                            System.out.println("【"+(new Date()).toString()+":"+System.currentTimeMillis()+"】重连成功("+(i-1)+");"+this.nextReConnIndex+"::"+this.curReConnIntervalTime);
                            lastReceiveTime=System.currentTimeMillis();
                            receiveMsg=new ReceiveMsg("接收消息");
                            receiveMsg.start();
                            sendBeat=new SendBeat("发送心跳");
                            sendBeat.start();
                            sendMsg=new SendMsg("发消息");
                            sendMsg.start();
                            break;//若连接成功了，则结束此进程
                        } else {//未连接成功
                            try { sleep(this.curReConnIntervalTime); } catch (InterruptedException e) {};//间隔策略时间
                            socket=null;
                            String s=scc.getReConnectIntervalTimeAndNextIndex(this.nextReConnIndex);
                            String[] _s=s.split("::");
                            this.nextReConnIndex=Integer.parseInt(_s[0]);
                            this.curReConnIntervalTime=Integer.parseInt(_s[1]);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
            System.out.println("<"+(new Date()).toString()+">"+this.getName()+"线程启动");
            PrintWriter out=null;
            try {
                while (true) {
                    try {
                        if (toBeStop) break;
                        if (socketOk()) {
                            synchronized (socketSendLock) {
                                if (out==null) out=new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
                                out.println("b^^");
                                System.out.println("Socket["+socket.hashCode()+"]发心跳:b");
                                out.flush();
                            }
                        }
                        try { sleep(scc.getIntervalBeat()); } catch (InterruptedException e) {}
                    } catch(Exception e) {
                        //e.printStackTrace();
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
            System.out.println("<"+(new Date()).toString()+">"+this.getName()+"线程启动");
            String filePath="C:\\opt\\logs\\";
            File dir=new File(filePath);
            if (!dir.isDirectory()) dir.mkdirs();
            int scode=SocketClient.this.getSocketCode();
            while (scode==0) {
                scode=SocketClient.this.getSocketCode();
            }
            File f=new File(filePath+"\\c_"+scode+"_send.log");
            FileOutputStream fos=null;
            try {
                if (!f.exists()) f.createNewFile();
                fos=new FileOutputStream(f, true);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            BufferedOutputStream out=null;
            try {
                while (true) {
                    try {
                        if (toBeStop) break;
                        if (socketOk()) {
                            byte[] msg4Send=sendMsgQueue.poll();
                            if (msg4Send==null) continue;
                            synchronized (socketSendLock) {
                                if (out==null) out=new BufferedOutputStream(socket.getOutputStream());
                                out.write(msg4Send);
                                fos.write(msg4Send);
                                fos.flush();
                                out.flush();
                            }
                        }
                        try { sleep(20); } catch (InterruptedException e) {}//扫描消息队列，间隔20毫秒
                    } catch(Exception e) {
                        //e.printStackTrace();
                    }
                }
            } finally {
                if (out!=null) {try {out.close();} catch(Exception e){} finally{out=null;} };
                try {if (fos!=null) fos.close();} catch(Exception e) {} finally {fos=null;}
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
            System.out.println("<"+(new Date()).toString()+">"+this.getName()+"线程启动");
            String filePath="c:\\opt\\logs\\";
            File dir=new File(filePath);
            if (!dir.isDirectory()) dir.mkdirs();
            int scode=SocketClient.this.getSocketCode();
            while (scode==0) {
                scode=SocketClient.this.getSocketCode();
            }
            File f=new File(filePath+"\\c_"+scode+"_receive.log");
            FileOutputStream fos=null;
            try {
                if (!f.exists()) f.createNewFile();
                fos=new FileOutputStream(f, true);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

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
                    if (in==null) in=new BufferedInputStream(socket.getInputStream());
                    int r;
                    synchronized (socketRecvLock) {
                        while ((r=in.read())!=-1) {
                            fos.write(r);
                            ba[i++]=(byte)r;
                            endMsgFlag[0]=endMsgFlag[1];
                            endMsgFlag[1]=endMsgFlag[2];
                            endMsgFlag[2]=(byte)r;
                            if (!hasBeginMsg) {
                                if (endMsgFlag[0]=='b'&&endMsgFlag[1]=='^'&&endMsgFlag[2]=='^') {
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
                        if (fos!=null) {
                            fos.write(13);
                            fos.write(10);
                            fos.flush();
                        }//一条消息读取完成

                        //
                        mba=Arrays.copyOfRange(ba, 0, i);
                        if (mba==null||mba.length<3) break; //若没有得到任何内容
                        if (ba[0]=='B'&&ba[1]=='^'&&ba[2]=='^') {
                            System.out.println("B======");
                        } else {
                            try {
                                Message ms=null;
                                try {
                                    ms=MessageUtils.buildMsgByBytes(mba);
                                    //特殊处理
                                    if (ms instanceof MsgNormal) {
                                        
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                System.out.println("收到:"+JsonUtils.objToJson(ms));
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
                }
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                try {if (fos!=null) fos.close();} catch(Exception e) {} finally {fos=null;}
                if (in!=null) {try {in.close();} catch(Exception e){} finally{in=null;} };
            }
        }
    }
}