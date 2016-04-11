package com.woting.searchword.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.spiritdata.framework.util.ChineseCharactersUtils;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.common.model.Owner;
import com.woting.searchword.WordUtils;

/**
 * 属于某一个所有者的查找词结构
 * 词管理就是通过这个数据来完成的
 * @author wanghui
 */
public class OwnerWord implements Serializable {
    private static final long serialVersionUID = 3456689479976991230L;

    private Owner owner; //本组词管理的所有人，目前只有系统所有者
    private int splitLevel; //向下分级的层数，默认值为5
    private int lang=1; //语言类型：目前都是1，简体中文
    private Map<String, MiddleWord> middleWordMap; //汉字热词查找结构
    private Map<String, MiddleWord> pyMiddleWordMap; //汉语拼音热词查找结构
    private Map<String, Word> searchFinalMap; //最终查找词的结构

    public Owner getOwner() {
        return owner;
    }
    public void setOwner(Owner owner) {
        this.owner = owner;
    }
    public int getSplitLevel() {
        return splitLevel;
    }
    public void setSplitLevel(int splitLevel) {
        this.splitLevel = splitLevel;
    }
    public int getLang() {
        return lang;
    }
    public void setLang(int lang) {
        this.lang = lang;
    }
    public Map<String, MiddleWord> getSearchMap() {
        return middleWordMap;
    }
    public void setSearchMap(Map<String, MiddleWord> searchMap) {
        this.middleWordMap = searchMap;
    }
    public Map<String, Word> getSearchFinalMap() {
        return searchFinalMap;
    }
    public void setSearchFinalMap(Map<String, Word> searchFinalMap) {
        this.searchFinalMap = searchFinalMap;
    }

    /**
     * 构造函数，要设置分字级别
     * @param splitLevel
     */
    public OwnerWord(int splitLevel) {
        super();
        this.splitLevel = splitLevel;
        searchFinalMap=new HashMap<String, Word>();
        middleWordMap=new HashMap<String, MiddleWord>();
        pyMiddleWordMap=new HashMap<String, MiddleWord>();
    }

    /**
     * 添加一个查找词，注意是最终查找词
     * @param word 最终查找词
     */
    public void addWord(String word) {
        if (StringUtils.isNullOrEmptyOrSpace(word)) return;
        //查查看是否已经有了这个词
        Word w=searchFinalMap.get(word);
        if (w!=null) w.incream();//这里加完，所有的相关的就都修改了？？？
        else {
            w=new Word(word);
            searchFinalMap.put(word, w);
        }
        //分字处理
        int hasSplit=0;
        boolean hasChar=true;
        String notSplitWords=word;
        String splitWords="";
        while (hasChar&&hasSplit<splitLevel) {
            //从左端分词，包括中英文
            String firstWord=WordUtils.splitFirstWord(notSplitWords);
            notSplitWords=notSplitWords.substring(0+firstWord.length());

            splitWords+=firstWord;
            //把查找词放入查找结构
            MiddleWord tempMw=middleWordMap.get(splitWords);
            if (tempMw!=null) {
                Word finalWord=tempMw.getWord(w);
                if (finalWord==null) tempMw.addWord(w);
                else tempMw.sortAfterIncream(w);//进行排序：精华
            } else {
                tempMw=new MiddleWord(splitWords);
                tempMw.addWord(w);
                middleWordMap.put(splitWords, tempMw);
            }
            //处理拼音
            String swPy=ChineseCharactersUtils.getFullSpellFirstUp(splitWords);
            tempMw=pyMiddleWordMap.get(swPy);
            if (tempMw!=null) {
                Word finalWord=tempMw.getWord(w);
                if (finalWord==null) tempMw.addWord(w);
                else tempMw.sortAfterIncream(w);//进行排序：精华
            } else {
                tempMw=new MiddleWord(splitWords);
                tempMw.addWord(w);
                pyMiddleWordMap.put(splitWords, tempMw);
            }

            hasChar=(notSplitWords.length()>0);
            hasSplit++;
        }
    }

    /**
     * 根据middleWord，获得前topNum个最终搜索词
     * @param middleWord 中间搜索词
     * @param topNum 
     * @return
     */
    public List<String> findWord(String middleWord, int topNum) {
        MiddleWord tempMw=middleWordMap.get(middleWord);
        if (tempMw==null) return null;
        return tempMw.getFirstWords(topNum);
    }
}