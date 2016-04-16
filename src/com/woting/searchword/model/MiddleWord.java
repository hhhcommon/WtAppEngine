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
    public MiddleWord(String middleWord) {
        super();
        this.middleWord=middleWord;
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

    /**
     * 获取前topNum个最终搜索词
     * @param topNum
     * @return 前topNum个最终搜索词列表
     */
    public List<Word> getTopWords(int topSize) {
        if (topSize<1) return null;
        List<Word> ret=new ArrayList<Word>();
        for (int i=0; i<(topSize>searchWordSortList.size()?searchWordSortList.size():topSize); i++) {
            ret.add(searchWordSortList.get(i));
        }
        return ret;
    }

    /**
     * 在增加访问数后，调整此顺序
     * @param w
     */
    public void sortAfterIncream(Word w) {
        //先找到:二查法
        if (searchWordSortList!=null&&!searchWordSortList.isEmpty()) return;
        int findIndex=_find(w, 0, searchWordSortList.size());
        if (findIndex!=-1) { //查到了，排序
            _sortIndex(w, findIndex);
        }
    }
    private int _find(Word w, int beginPos, int endPos) {
        if (beginPos==endPos) {
            if (searchWordMap.get(endPos).equals(w)) return endPos;
            return -1;
        }
        if (searchWordMap.get(beginPos).equals(w)) return beginPos;
        if (searchWordMap.get(endPos).equals(w)) return endPos;
        int halfIndex=(endPos+beginPos)/2;
        if (halfIndex==beginPos||halfIndex==endPos) return -1;

        Word halfWord=searchWordMap.get(halfIndex);
        if (halfWord.equals(w)) return halfIndex;
        if (halfWord.getCount()>w.getCount()) return _find(w, halfIndex, endPos);
        else return _find(w, beginPos, halfIndex);
    }
    private void _sortIndex(Word w, int pos) {
        int upperIndex=pos-1;
        if (upperIndex<0) return;
        Word priorW=searchWordSortList.get(upperIndex);
        if (w.getCount()>priorW.getCount()) {//交换
            searchWordSortList.set(upperIndex, w);
            searchWordSortList.set(pos, priorW);
            _sortIndex(w, upperIndex);
        }
    }
}