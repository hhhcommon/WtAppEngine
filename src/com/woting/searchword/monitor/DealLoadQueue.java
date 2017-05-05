package com.woting.searchword.monitor;

import com.woting.searchword.service.WordService;

public class DealLoadQueue extends Thread {
    private WordService wordService;

    /**
     * 构造函数
     */
    public DealLoadQueue(WordService wordService) {
        this.wordService=wordService;
    }

    @Override
    public void run() {
        wordService.dealWordLoadQueue();
    }
}