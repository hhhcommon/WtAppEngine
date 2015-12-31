package com.woting.appengine.mobile.push;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.appengine.mobile.push.monitor.socket.SocketHandle;
import com.woting.appengine.mobile.push.monitor.socket.SocketMonitorConfig;

/**
 * 消息推送服务主服务
 * @author wanghui
 */
public class PushSocketServer extends Thread {
    private PushConfig pc=null;
    private static ServerSocket serverSocket=null;
    private static PushMemoryManage pmm=PushMemoryManage.getInstance();

    /*
    public static void main(String[] args) throws UnknownHostException, IOException {
        PushSocketServer pss = new PushSocketServer(new PushConfig());
        pss.start();
    }*/

    /**
     * 构造函数
     * @param pc 推送参数
     */
    public PushSocketServer(PushConfig pc) {
        super("推送服务监控进程["+pc.getPORT_PUSHSERVER()+"]");
        this.pc=pc;
    }

    /*
     * 关闭Socket的服务连接
     */
    private static void closeServerSocket() {
        if (PushSocketServer.serverSocket!=null) {
            try {
                PushSocketServer.serverSocket.close();
            } catch(Exception e) {
            }
        }
    }

    public void run() {
        if (pmm.isServerRuning()) return ;
        try {
            PushSocketServer.serverSocket=new ServerSocket(this.pc.getPORT_PUSHSERVER());
            System.out.println("地址["+InetAddress.getLocalHost().getHostAddress()+"]:端口["+pc.getPORT_PUSHSERVER()+"]启动推送服务监控进程");
            //加一个关闭jvm时可调用的方法，关闭此线程池
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        System.out.println("JVM退出时关闭推送服务监控进程");
                        PushSocketServer.closeServerSocket();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            pmm.setServerRuning(true);
            //启动监控任务
            while (true) {
                Socket client=serverSocket.accept();
                //准备参数
                SocketMonitorConfig smc=new SocketMonitorConfig();
                new Thread(new SocketHandle(client, smc),"Socket["+client.getRemoteSocketAddress()+",socketKey="+client.hashCode()+"]监控主线程").start();
            }
        } catch(Exception e) {
            pmm.setServerRuning(false);
            e.printStackTrace();
        } finally {
            PushSocketServer.closeServerSocket();
        }
    }
}