package com.woting.searchword.monitor;

import com.woting.searchword.service.WordService;

public class DealOnlineQueue extends Thread {
    private WordService wordService;

    /**
     * 构造函数
     */
    public DealOnlineQueue(WordService wordService) {
        super("在线搜索词队列监控");
        this.wordService=wordService;
    }

    @Override
    public void run() {
        if (wordService!=null) {
            System.out.println(this.getName()+"开始执行");
            while (true) {//每隔一段时间处理一次检索词
                try { sleep(50); } catch (InterruptedException e) {};
                wordService.dealWordOnlineQueue();
            }
        }
    }
}