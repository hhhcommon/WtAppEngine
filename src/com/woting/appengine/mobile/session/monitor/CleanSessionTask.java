package com.woting.appengine.mobile.session.monitor;

import java.util.TimerTask;

import com.woting.appengine.mobile.session.mem.SessionMemoryManage;

/**
 * 清除会话信息的任务线程
 * @author wh
 */
public class CleanSessionTask extends TimerTask {
    @Override
    public void run() {
        try {
            SessionMemoryManage smm = SessionMemoryManage.getInstance();
            smm.clean();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}