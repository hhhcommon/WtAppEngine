package com.woting.searchword.mem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.woting.cm.core.common.model.Owner;
import com.woting.searchword.model.OwnerWord;
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

    protected BlockingQueue<String> wordOnlineQueue;//在线搜索词处理队列，只记录日志
    protected BlockingQueue<String> wordLoadQueue;  //历史搜索词统计结果加载队列
    protected Map<String, OwnerWord> ownerWordMap;  //属于某一个所有者的查找词结构的Map

    /**
     * 初始化
     */
    private SearchWordMemory() {
        wordOnlineQueue=new LinkedBlockingQueue<String>();
        wordLoadQueue=new LinkedBlockingQueue<String>();
        ownerWordMap=new HashMap<String, OwnerWord>();
    }

    /**
     * 加入一个搜索词到在线搜索词队列
     * @param word
     */
    public void addWord2OnlineQueue(String word) {
        wordOnlineQueue.add(word);
    }

    /**
     * 加入一个搜索词到加载搜索词队列
     * @param word
     */
    public void addWord2LoadQueue(String word) {
        wordLoadQueue.add(word);
    }

    /**
     * 从加载搜索词队列获得一个搜索词
     */
    public String pollFromLoadQueue() {
        return wordLoadQueue.poll();
    }

    /**
     * 从在线搜索词队列获得一个搜索词
     */
    public String pollFromOnlineQueue() {
        return wordOnlineQueue.poll();
    }

    /**
     * 获得所有者搜索词对象
     * @param o 所有者对象
     * @return 搜索词对象
     */
    public OwnerWord getOwnerWord(Owner o) {
        return ownerWordMap.get(o.getKey());
    }

    /**
     * 放入一个所有者搜索词对象
     * @param ow 所有者搜索词对象
     */
    public void putOwnerWord(OwnerWord ow) {
       ownerWordMap.put(ow.getOwner().getKey(), ow);
    }

    /**
     * 根据middleWord，得到属于o的前topSize排名的检索热词
     * @param middleWord 查找词
     * @param o 所属者
     * @param topSize 获得个数
     * @return 词列表
     */
    public List<Word> getTopWordList(String middleWord, Owner o, int topSize) {
        OwnerWord ow=ownerWordMap.get(o.getKey());
        if (ow==null) return null;
        return ow.findWord(middleWord, topSize);
    }

    /**
     * 得到属于o的前topSize排名的检索热词
     * @param o 所属者
     * @param topSize 获得个数
     * @return 词列表
     */
    public List<Word> getTopWordList(Owner o, int topSize) {
        OwnerWord ow=ownerWordMap.get(o.getKey());
        if (ow==null) return null;
        return ow.getWord(topSize);
    }
}