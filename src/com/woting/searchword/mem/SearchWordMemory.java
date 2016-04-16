package com.woting.searchword.mem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.spiritdata.framework.util.StringUtils;
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

    protected BlockingQueue<String> wordQueue;//搜索词队列，其中的字符串包括用户的信息和被搜索的词，这个队列用于记录日志和更新搜索词列表
    protected Map<String, OwnerWord> ownerWordMap;//属于某一个所有者的查找词结构的Map

    /**
     * 初始化
     */
    public void init() {
        SearchWordMemory.getInstance();
        wordQueue=new LinkedBlockingQueue<String>();
        ownerWordMap=new HashMap<String, OwnerWord>();
    }

    /**
     * 加入一个搜索词到搜搜词队列
     * @param word
     */
    public void addWord2Queue(String word) {
        wordQueue.add(word);
    }

    /**
     * 处理一个搜索词 addWord2Queue(o.getOwnerId()+"::"+o.getOwnerType()+"::"+oneWord)
     */
    public void dealOneWord() {
        String _oneWord=wordQueue.poll();
        if (StringUtils.isNullOrEmptyOrSpace(_oneWord)) {
            String[] _split=_oneWord.split("::");
            if (_split.length==3) {
                Owner o=new Owner(Integer.parseInt(_split[1]), _split[0]);
                Owner sysO=new Owner(100, "cm");
                OwnerWord ow=ownerWordMap.get(o.getKey());
                if (ow==null) {
                    ow=new OwnerWord(o, 5);
                    ownerWordMap.put(o.getKey(), ow);
                }
                ow.addWord(_split[2]);
                if (!o.equals(sysO)) {
                    ow=ownerWordMap.get(sysO.getKey());
                    if (ow==null) {
                        ow=new OwnerWord(sysO, 5);
                        ownerWordMap.put(sysO.getKey(), ow);
                    }
                    ow.addWord(_split[2]);
                }
            }
        }
    }

    /**
     * 加入一个搜索词到搜搜词队列
     * @param word
     */
    public List<Word> getTopWordList(String middleWord, Owner o, int topSize) {
        OwnerWord ow=ownerWordMap.get(o.getKey());
        if (ow==null) return null;
        return ow.findWord(middleWord, topSize);
    }
}