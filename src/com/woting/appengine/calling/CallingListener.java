package com.woting.appengine.calling;

import com.woting.appengine.calling.mem.CallingMemoryManage;
import com.woting.appengine.calling.monitor.DealCalling;

/**
 * 电话处理的监听程序
 * @author wanghui
 *
 */
public class CallingListener extends Thread {
    private static CallingConfig cc=null;//电话控制的配置参数
    public static CallingConfig getCallingConfig() {
        return cc;
    }

    public static void begin(CallingConfig cc) {
        CallingListener.cc=cc;
        CallingListener cl = new CallingListener();
        cl.start();
    }

    @Override
    public void run() {
        try {
            sleep(3000);//多少毫秒后启动任务处理，先让系统的其他启动任务完成，这里设置死为10秒钟
            //初始化内存结构
            CallingMemoryManage.getInstance();
            //启动处理线程
            for (int i=0;i<cc.getTHREADCOUNT_DEALCALLING(); i++) {
                DealCalling dc = new DealCalling(""+i);
                dc.setDaemon(true);
                dc.start();
            }
            //启动组对讲者清理任务
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}