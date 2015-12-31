package com.woting.appengine.mobile.session;

import java.util.Date;
import java.util.Timer;

import com.woting.appengine.mobile.session.mem.SessionMemory;
import com.woting.appengine.mobile.session.monitor.CleanSessionTask;

/**
 * <pre>会话监听，包括：
 * 1-设置会话过期
 * 2-清空过期的会话
 * </pre>
 * @author wh
 */
//守护线程，与主进程同存亡，用户线程，自己要完成
public class SessionListener extends Thread {
    private static MobileSessionConfig msc=null;

    public static void begin(MobileSessionConfig msc) {
        SessionListener.msc=msc;
        SessionListener sl = new SessionListener();
        sl.start();
    }

    //开启会话清理任务
    private void startMonitor() {
        System.out.println("启动会话清理任务，任务启动间隔["+msc.getCLEAN_INTERVAL()+"]毫秒");
        Timer mobileSession_Timer = new Timer("移动回话清理任务，每隔["+msc.getCLEAN_INTERVAL()+"]毫秒执行", true);
        CleanSessionTask ct = new CleanSessionTask();
        mobileSession_Timer.schedule(ct, new Date(), msc.getCLEAN_INTERVAL());
    }

    @Override
    public void run() {
        try {
            sleep(5000);//多少毫秒后启动任务处理，先让系统的其他启动任务完成，这里设置死为10秒钟
            //初始化会话的内存结构
            SessionMemory.getInstance();
            startMonitor(); //启动会话监控
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}