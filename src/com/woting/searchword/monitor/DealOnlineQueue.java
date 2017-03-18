package com.woting.searchword.monitor;

import java.util.TimerTask;

import com.woting.searchword.service.WordService;

public class DealOnlineQueue extends TimerTask {
    private WordService wordService;

    /**
     * 构造函数
     */
    public DealOnlineQueue(WordService wordService) {
        this.wordService=wordService;
    }

    @Override
    public void run() {
        wordService.dealWordOnlineQueue();
    }
}