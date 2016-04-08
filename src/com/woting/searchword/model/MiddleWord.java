package com.woting.searchword.model;

import java.io.Serializable;
import java.util.List;

/**
 * 中间查找词
 * @author wanghui
 */
public class MiddleWord implements Serializable {
    private static final long serialVersionUID = -2872599545375755094L;
    private Word word;//中间过度词
    private List<Word> searchWords; //最终的查找词列表，词列表是按照词的频度进行排列的
    public Word getWord() {
        return word;
    }
    public void setWord(Word word) {
        this.word = word;
    }
    public List<Word> getSearchWords() {
        return searchWords;
    }
    public void setSearchWords(List<Word> searchWords) {
        this.searchWords = searchWords;
    }
}