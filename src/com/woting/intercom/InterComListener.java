package com.woting.intercom;

import com.woting.intercom.mem.GroupMemory;
import com.woting.mobile.push.PushSocketServer;
import com.woting.mobile.push.mem.ReceiveMemory;
import com.woting.mobile.push.monitor.DealReceivePureQueue;

public class InterComListener extends Thread {
    private static InterComConfig icc=null;//对讲控制的配置参数

    public static void begin(InterComConfig icc) {
        InterComListener.icc=icc;
        InterComListener icl = new InterComListener();
        icl.start();
    }

    @Override
    public void run() {
        try {
            sleep(5000);//多少毫秒后启动任务处理，先让系统的其他启动任务完成，这里设置死为10秒钟
            //初始化内存结构
            GroupMemory.getInstance();
            //启动服务
//            PushSocketServer pss = new PushSocketServer(pc);
//            pss.setDaemon(true);
//            pss.start();
//            //启动读取线程
//            for (int i=0;i<pc.getTHREADCOUNT_DEALRECEIVEQUEUE(); i++) {
//                DealReceivePureQueue drpq = new DealReceivePureQueue(""+i);
//                drpq.setDaemon(true);
//                drpq.start();
//            }
//            //启动清理内存服务——垃圾回收
//            startCleanMonitor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}