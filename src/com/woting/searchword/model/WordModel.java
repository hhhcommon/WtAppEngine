package com.woting.searchword.model;

import java.io.Serializable;
import java.util.List;

/**
 * 一套独立的词系统，他是一个树状结构
 * @author wanghui
 */
public class WordModel implements Serializable {
    private static final long serialVersionUID = -2872599545375755094L;
    private Word word;//本身的词,这个词一般来说不是最终的词
    private List<Word> subWords; //被分解的下一个阶段的词
    public Word getWord() {
        return word;
    }
    public void setWord(Word word) {
        this.word = word;
    }
    public List<Word> getSubWords() {
        return subWords;
    }
    public void setSubWords(List<Word> subWords) {
        this.subWords = subWords;
    }
}