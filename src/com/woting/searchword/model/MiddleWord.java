package com.woting.searchword.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.spiritdata.framework.util.StringUtils;
import com.woting.searchword.WordUtils;

/**
 * 中间查找词
 * @author wanghui
 */
public class MiddleWord implements Serializable {
    private static final long serialVersionUID = -2872599545375755094L;

    private Object dealLock=new Object();
    private String middleWord;//中间过度词

    private Map<String, Word> searchWordMap; //最终的查找词Map，便于查找
    private List<Word> searchWordSortList;   //最终的查找词列表，此列表是按照词的频度进行排列的

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
        searchWordMap=new HashMap<String, Word>();
        searchWordSortList=new ArrayList<Word>();
    }

    /**
     * 找到最终词
     * @param finalWord
     */
    public Word getWord(Word finalWord) {
        if (searchWordMap==null) return null;
        return searchWordMap.get(finalWord.getWord());
    }

    /**
     * 获取前topSize个最终搜索词
     * @param topSize
     * @return 前topSize个最终搜索词列表
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
     * 把一个词按其顺序插入最终词结构
     * @param word
     */
    public void addWord(Word word) {
        if (StringUtils.isNullOrEmptyOrSpace(word.getWord())) return;
        //查查看是否已经有了这个词
        synchronized (dealLock) {
            Word w=searchWordMap.get(word.getWord());
            if (w==null) {
                searchWordMap.put(word.getWord(), word);
                w=word;
            }
            WordUtils.addToSortList(w, searchWordSortList);
        }
    }
}