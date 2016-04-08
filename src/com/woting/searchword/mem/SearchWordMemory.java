package com.woting.searchword.mem;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.woting.searchword.model.Word;

public class SearchWordMemory {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static SearchWordMemory instance = new SearchWordMemory();
    }
    public static SearchWordMemory getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    protected BlockingQueue<String> wordQueue;//搜索词队列

    protected Map<String, Word> finalSearchWordMap; //最终查找词的结构

    public void init() {
        SearchWordMemory swm=SearchWordMemory.getInstance();
        swm.wordQueue=new LinkedBlockingQueue<String>();
        finalSearchWordMap=new HashMap<String, Word>();
    }

    
}