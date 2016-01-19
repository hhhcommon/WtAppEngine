package com.woting.appengine.mobile.mediaflow.monitor;

import java.util.TimerTask;

import com.woting.appengine.mobile.mediaflow.mem.TalkMemoryManage;

public class CleanMediaflowTask extends TimerTask {
    @Override
    public void run() {
        try {
            TalkMemoryManage tmm = TalkMemoryManage.getInstance();
            tmm.clean();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}