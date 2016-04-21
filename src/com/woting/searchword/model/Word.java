package com.woting.searchword.model;

import java.io.Serializable;

/**
 * 检索的词和字，这个词和字是被拆分的：
 * 如"中国人" 被分解为"中","中国"和"中国人"三个
 * @author wanghui
 */
public class Word implements Serializable {
    private static final long serialVersionUID=337488994588099867L;
    private String word; //被检索的词
    private long count; //被检索的次数

    public String getWord() {
        return word;
    }
    public void setWord(String word) {
        this.word=word;
    }
    public long getCount() {
        return count;
    }
    public void setCount(long count) {
        this.count=count;
    }

    /**
     * 构造函数
     * @param word
     */
    public Word(String word) {
        super();
        this.word=word;
        count=1;
    }
    public Word(String word, int count) {
        super();
        this.word=word;
        this.count=count;
    }

    /**
     * 把检索数加1
     * 注意：这里不考虑并发的情况
     */
    public void incream(long i) {
        count+=i;
    }

    @Override
    public String toString() {
        return word;
    }

    @Override
    public boolean equals(Object o) {
        if (this==o) return true;
        if (o==null||!(o instanceof Word)) return false;

        Word _o=(Word)o;
        return _o.getWord().equals(this.getWord());
    }
}