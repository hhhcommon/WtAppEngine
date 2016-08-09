package com.woting.appengine.mobile.push;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.appengine.mobile.push.monitor.socket.SocketHandle;
import com.woting.appengine.mobile.push.monitor.socket.SocketMonitorConfig;
//import com.woting.appengine.mobile.push.monitor.socket.SocketChannelHandle;
//import com.woting.appengine.mobile.push.monitor.socket.SocketMonitorConfig;

/**
 * 消息推送服务主服务
 * @author wanghui
 */
public class PushSocketServer extends Thread {
    private PushConfig pc=null;
    private static ServerSocket serverSocket=null;
    private static PushMemoryManage pmm=PushMemoryManage.getInstance();
//    private static Selector selector;
//    private static ServerSocketChannel serverChannel=null;

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
        //停止监控过程
//        if (selector!=null) {
//            try {
//                selector.wakeup();
//                selector.close();
//            } catch(Exception e) {
//                e.printStackTrace();
//            } finally {
//                selector=null;
//            }
//        }
//        if (serverChannel!=null) {
//            try {
//                serverChannel.close();
//            } catch(Exception e) {
//                e.printStackTrace();
//            } finally {
//                serverChannel=null;
//            }
//        }
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
//            try {
//                selector=Selector.open();
//
//                serverChannel=ServerSocketChannel.open();
//                serverChannel.configureBlocking(false);
//                serverChannel.socket().setReuseAddress(true);
//                serverChannel.socket().bind(new InetSocketAddress(pc.getPORT_PUSHSERVER()));
//                System.out.println("NIO TCP Connector nio 供应类: "+selector.provider().getClass().getCanonicalName());
//                serverChannel.register(selector, SelectionKey.OP_ACCEPT);
//                pmm.setServerRuning(true);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            while(pmm.isServerRuning()&&selector!=null) {
//                try {
//                    if(selector.select()>0) {
//                        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
//                        while (it.hasNext()) {
//                            SelectionKey key=it.next();
//                            it.remove();
//                            if (key.isAcceptable()) {
//                                //获得客户端连接通道
//                                SocketChannel clientChannel=((ServerSocketChannel)key.channel()).accept();
//                                clientChannel.configureBlocking(false);
//                                SocketMonitorConfig smc=new SocketMonitorConfig();
//                                new Thread(new SocketChannelHandle(clientChannel, smc), "Socket["+clientChannel.socket().getRemoteSocketAddress()+",socketKey="+clientChannel.socket().hashCode()+"]监控主线程").start();
////                                clientChannel.register(selector, SelectionKey.OP_READ);
////                                clientChannel.register(selector, SelectionKey.OP_WRITE);
//                            }
//                        }
//                    }
//                } catch(ClosedSelectorException cse) {
//                    cse.printStackTrace();
//                    break;
//                } catch(Exception e) {
//                    e.printStackTrace();
//                }
//            }
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