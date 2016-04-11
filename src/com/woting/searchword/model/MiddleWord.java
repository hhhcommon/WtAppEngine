package com.woting.searchword.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中间查找词
 * @author wanghui
 */
public class MiddleWord implements Serializable {
    private static final long serialVersionUID = -2872599545375755094L;

    private Object dealLock=new Object();
    private String middleWord;//中间过度词
    private List<Word> searchWordSortList; //最终的查找词列表，词列表是按照词的频度进行排列的
    private Map<String, Word> searchWordMap; //最终的查找词列表，词列表是按照词的频度进行排列的

    public String getMiddleWord() {
        return middleWord;
    }
    public void setWord(String middleWord) {
        this.middleWord=middleWord;
    }

    /**
     * 构造函数
     */
    public MiddleWord() {
        super();
        middleWord=null;
        searchWordSortList=new ArrayList<Word>();
        searchWordMap=new HashMap<String, Word>();
    }

    /**
     * 找到最终词
     * @param splitWords
     */
    public Word getWord(Word finalWord) {
        if (searchWordMap!=null) {
            return searchWordMap.get(finalWord.getWord());
        }
        return null;
    }

    /**
     * 插入最终词
     * @param w 最终词
     */
    public void addWord(Word w) {
        if (searchWordSortList==null) searchWordSortList=new ArrayList<Word>();
        if (searchWordMap==null) searchWordMap=new HashMap<String, Word>();
        synchronized (dealLock) {
            searchWordSortList.add(w);
            searchWordMap.put(w.getWord(), w);
        }
    }

    public void sortAfterIncream(Word w) {
        //先找到:二查法
        if (searchWordSortList!=null&&!searchWordSortList.isEmpty()) return;
        int findIndex=_find(w, 0, searchWordSortList.size());
        int find=-1;
        
    }

    private int _find(Word w, int begiPos, int endPos) {
        if (begiPos==endPos) {
            if (searchWordMap.get(endPos).equals(w)) return endPos;
            return -1;
        }
        if (searchWordMap.get(begiPos).equals(w)) return begiPos;
        if (searchWordMap.get(endPos).equals(w)) return endPos;
        

        halfIndex=(endPos+beginPos)/2
        Word first=searchWordSortList.get(0);
        Word edn=searchWordSortList.get(searchWordSortList.size());
        return 1;
    }
}