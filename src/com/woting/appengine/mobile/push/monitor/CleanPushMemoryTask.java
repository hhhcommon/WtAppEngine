package com.woting.appengine.mobile.push.monitor;

import java.util.TimerTask;

import com.woting.appengine.mobile.push.mem.PushMemoryManage;

public class CleanPushMemoryTask extends TimerTask {
    @Override
    public void run() {
        try {
            PushMemoryManage ppm = PushMemoryManage.getInstance();
            ppm.clean();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
