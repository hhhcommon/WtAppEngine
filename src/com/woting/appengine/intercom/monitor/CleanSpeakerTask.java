package com.woting.appengine.intercom.monitor;

import java.util.TimerTask;

import com.woting.appengine.intercom.mem.GroupMemoryManage;

public class CleanSpeakerTask extends TimerTask {
    private long expireTime;
    
    public CleanSpeakerTask(long speaker_expire) {
        this.expireTime=speaker_expire;
    }

    @Override
    public void run() {
        try {
            GroupMemoryManage gmm = GroupMemoryManage.getInstance();
            gmm.cleanSpeaker(expireTime);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}