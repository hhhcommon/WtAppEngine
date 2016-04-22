package com.woting.searchword.monitor;

import com.woting.searchword.service.WordService;

public class LoadWord extends Thread {
    private WordService wordService;

    /**
     * 构造函数
     */
    public LoadWord(WordService wordService) {
        super("加载搜索词过程");
        this.wordService=wordService;
    }

    @Override
    public void run() {
        if (wordService!=null) {
            System.out.println(this.getName()+"开始执行");
            wordService.loadUserWord();
        }
    }
}