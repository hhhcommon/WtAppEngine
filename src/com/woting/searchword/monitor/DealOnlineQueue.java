package com.woting.searchword.monitor;

import com.woting.searchword.service.WordService;

public class DealOnlineQueue extends Thread {
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