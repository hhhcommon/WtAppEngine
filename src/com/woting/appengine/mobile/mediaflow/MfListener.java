package com.woting.appengine.mobile.mediaflow;

import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.mobile.mediaflow.monitor.DealMf;

public class MfListener extends Thread {
    private static MfConfig mfc=null;//对讲控制的配置参数

    public static void begin(MfConfig mfc) {
        MfListener.mfc=mfc;
        MfListener mfl = new MfListener();
        mfl.start();
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
                DealMf dmf = new DealMf(""+i);
                dmf.setDaemon(true);
                dmf.start();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}