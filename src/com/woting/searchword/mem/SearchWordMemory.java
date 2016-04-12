package com.woting.searchword.mem;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.woting.searchword.model.OwnerWord;


public class SearchWordMemory {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static SearchWordMemory instance = new SearchWordMemory();
    }
    public static SearchWordMemory getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    protected BlockingQueue<String> wordQueue;//搜索词队列，其中的字符串包括用户的信息和被搜索的词，这个队列用于记录日志和更新搜索词列表
    protected Map<String, OwnerWord> ownerWordMap;//属于某一个所有者的查找词结构的Map

    /**
     * 初始化
     */
    public void init() {
        SearchWordMemory swm=SearchWordMemory.getInstance();
        swm.wordQueue=new LinkedBlockingQueue<String>();
    }

    /**
     * 加入一个搜索词到搜搜词队列
     * @param word
     */
    public void addWord2Queue(String word) {
        
        wordQueue.add(word);
    }
}