package com.woting.appengine.mobile.mediaflow;

import java.util.Date;
import java.util.Timer;

import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.mobile.mediaflow.monitor.CleanMediaflowTask;
import com.woting.appengine.mobile.mediaflow.monitor.DealMediaflow;

public class MfListener extends Thread {
    private static MfConfig mfc=null;//对讲控制的配置参数

    public static void begin(MfConfig mfc) {
        MfListener.mfc=mfc;
        MfListener mfl = new MfListener();
        mfl.start();
    }

    public void cleanTmm() {
        System.out.println("启动对讲会话清理任务，任务启动间隔["+mfc.getCLEAN_INTERVAL()+"]毫秒");
        Timer mediaflow_Timer = new Timer("对讲会话清理任务，每隔["+mfc.getCLEAN_INTERVAL()+"]毫秒执行", true);
        CleanMediaflowTask ct = new CleanMediaflowTask();
        mediaflow_Timer.schedule(ct, new Date(), mfc.getCLEAN_INTERVAL());
    }

    @Override
    public void run() {
        try {
            sleep(5000);//多少毫秒后启动任务处理，先让系统的其他启动任务完成，这里设置死为10秒钟
            //初始化内存结构
            GroupMemoryManage gmm=GroupMemoryManage.getInstance();
            gmm.initMemory();
            //加载用户组
            //启动处理线程
            for (int i=0;i<mfc.getTHREADCOUNT_DEALMF(); i++) {
                DealMediaflow dmf = new DealMediaflow(""+i);
                dmf.setDaemon(true);
                dmf.start();
            }
            //启动对讲数据清除任务
            cleanTmm();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}