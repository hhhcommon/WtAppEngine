package com.woting.intercom;

import com.woting.intercom.mem.GroupMemoryManage;
import com.woting.intercom.monitor.DealInterCom;

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
            GroupMemoryManage gmm=GroupMemoryManage.getInstance();
            gmm.initMemory();
            //加载用户组
            //启动处理线程
            for (int i=0;i<icc.getTHREADCOUNT_DEALINTERCOM(); i++) {
                DealInterCom dic = new DealInterCom(""+i);
                dic.setDaemon(true);
                dic.start();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}