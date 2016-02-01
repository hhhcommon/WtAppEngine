package com.woting.appengine.intercom;

import java.util.Date;
import java.util.Timer;

import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.intercom.monitor.CleanSpeakerTask;
import com.woting.appengine.intercom.monitor.DealInterCom;

public class InterComListener extends Thread {
    private static InterComConfig icc=null;//对讲控制的配置参数

    public static void begin(InterComConfig icc) {
        InterComListener.icc=icc;
        InterComListener icl = new InterComListener();
        icl.start();
    }

    private void cleanSpeaker() {
        System.out.println("启动组对讲者清理任务，任务启动间隔["+icc.getCLEANSPEAKER_INTERVAL()+"]毫秒");
        Timer interCom_CleanSpeakerTimer = new Timer("组对讲者清理任务，每隔["+icc.getCLEANSPEAKER_INTERVAL()+"]毫秒执行", true);
        CleanSpeakerTask cpt = new CleanSpeakerTask(icc.getSPEAKER_EXPIRE());
        interCom_CleanSpeakerTimer.schedule(cpt, new Date(), icc.getCLEANSPEAKER_INTERVAL());
    }

    @Override
    public void run() {
        try {
            sleep(5000);//多少毫秒后启动任务处理，先让系统的其他启动任务完成，这里设置死为10秒钟
            //初始化内存结构
            GroupMemoryManage gmm=GroupMemoryManage.getInstance();
            gmm.initMemory();
            //启动处理线程
            for (int i=0;i<icc.getTHREADCOUNT_DEALINTERCOM(); i++) {
                DealInterCom dic = new DealInterCom(""+i);
                dic.setDaemon(true);
                dic.start();
            }
            //启动组对讲者清理任务
            cleanSpeaker();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}