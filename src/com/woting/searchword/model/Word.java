package com.woting.searchword.model;

import java.io.Serializable;

/**
 * 检索的词和字，这个词和字是被拆分的：
 * 如"中国人" 被分解为"中","中国"和"中国人"三个
 * @author wanghui
 */
public class Word implements Serializable {
    private static final long serialVersionUID = 337488994588099867L;
    private String word; //被检索的词
    private long count; //被检索的次数
    public String getWord() {
        return word;
    }
    public void setWord(String word) {
        this.word = word;
    }
    public long getCount() {
        return count;
    }
    public void setCount(long count) {
        this.count = count;
    }
}