package com.woting.searchword.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.spiritdata.framework.util.ChineseCharactersUtils;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.common.model.Owner;
import com.woting.searchword.WordUtils;

/**
 * 属于某一个所有者的搜索词结构
 * 词管理就是通过这个数据来完成的
 * @author wanghui
 */
public class OwnerWord implements Serializable {
    private static final long serialVersionUID = 3456689479976991230L;

    private Owner owner; //本组词管理的所有人，目前只有系统所有者
    private int splitLevel; //向下分级的层数，默认值为5
    private int lang=1; //语言类型：目前都是1，简体中文
    private Map<String, MiddleWord> middleWordMap; //汉字热词搜索结构
    private Map<String, MiddleWord> pyMiddleWordMap; //汉语拼音热词搜索结构

    private Map<String, Word> searchWordMap; //最终搜索词的结构
    private Map<String, Integer> wordIndex;  //最终搜索词位置对象
    private List<Word> searchWordSortList;   //最终搜索词列表，此列表是按照词的频度进行排列的

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

    /**
     * 构造函数，要设置分字级别
     * @param splitLevel
     */
    public OwnerWord(Owner o, int splitLevel) {
        super();
        owner=o;
        this.splitLevel = splitLevel;
        middleWordMap=new HashMap<String, MiddleWord>();
        pyMiddleWordMap=new HashMap<String, MiddleWord>();
        searchWordMap=new HashMap<String, Word>();
        wordIndex=new HashMap<String, Integer>();
        searchWordSortList=new ArrayList<Word>();
    }

    /**
     * 添加一个搜索词，注意是最终搜索词
     * @param word 最终搜索词
     */
    public void addWord(String word) {
        if (StringUtils.isNullOrEmptyOrSpace(word)) return;
        //查查看是否已经有了这个词
        Word w=searchWordMap.get(word);
        if (w==null) {
            w=new Word(word);
            searchWordMap.put(word, w);
            searchWordSortList.add(w);
            wordIndex.put(word, searchWordSortList.size()-1);
        } else {
            w.incream(1);
            if (!searchWordSortList.isEmpty()) {
                int wordPos=wordIndex.get(word);
                searchWordSortList.remove(wordPos);
                int newPos=WordUtils.findInsertPos(w, 0, wordPos, searchWordSortList);
                if (newPos!=-1) {
                    searchWordSortList.add(newPos, w);
                    //调整位置索引
                    for (int i=wordPos; i<=newPos; i++) {
                        wordIndex.put(searchWordSortList.get(i).getWord(), i);
                    }
                }
            }
        }
        //分字处理
        splitWord(w);
    }

    /**
     * 加载一个搜索词，注意是最终搜索词，
     * @param word 最终搜索词
     */
    public void loadWord(Word word) {
        if (StringUtils.isNullOrEmptyOrSpace(word.getWord())) return;
        //查查看是否已经有了这个词
        Word w=searchWordMap.get(word.getWord());
        if (w==null) {//这个词不存在
            searchWordMap.put(word.getWord(), word);
            //排序插入
            int insertIndex=0;
            if (!searchWordSortList.isEmpty()) {
                insertIndex=WordUtils.findInsertPos(word, 0, searchWordSortList.size(), searchWordSortList);
            }
            searchWordSortList.add(insertIndex, word);
            for (int i=insertIndex; i<searchWordSortList.size(); i++) {
                wordIndex.put(searchWordSortList.get(i).getWord(), i);
            }
        } else {
            w.incream(word.getCount());
            //调整排序
            if (!searchWordSortList.isEmpty()) {
                int wordPos=wordIndex.get(word.getWord());
                searchWordSortList.remove(wordPos);
                if (wordPos!=-1) {
                    int newPos=WordUtils.findInsertPos(w, 0, wordPos, searchWordSortList);
                    if (newPos!=-1) {
                        searchWordSortList.add(newPos, w);
                        //调整位置索引
                        for (int i=newPos; i<=wordPos; i++) {
                            wordIndex.put(searchWordSortList.get(i).getWord(), i);
                        }
                    }
                }
            }
        }
        //分字处理
        splitWord(word);
    }

    /**
     * 根据middleWord，获得前topSize个最终搜索词
     * @param middleWord 中间搜索词
     * @param topSize 
     * @return
     */
    public List<Word> findWord(String middleWord, int topSize) {
        MiddleWord tempMw=middleWordMap.get(middleWord);
        if (tempMw==null) {
            //从拼音中找
            String _middleWord=ChineseCharactersUtils.getFullSpellFirstUp(middleWord);
            _middleWord=_middleWord.toLowerCase();
            tempMw=pyMiddleWordMap.get(_middleWord);
        }
        if (tempMw==null) return null;
        return tempMw.getTopWords(topSize);
    }

    /**
     * 获得前topSize个最终搜索词
     * @param topSize 
     * @return
     */
    public List<Word> getWord(int topSize) {
        if (searchWordSortList==null||searchWordSortList.isEmpty()) return null;
        if (topSize<1) return null;
        List<Word> ret=new ArrayList<Word>();
        for (int i=0; i<(topSize>searchWordSortList.size()?searchWordSortList.size():topSize); i++) {
            ret.add(searchWordSortList.get(i));
        }
        return ret;
    }

    /*
     * 分字处理
     * @param w
     */
    private void splitWord(Word w) {
        int hasSplit=0;
        boolean hasChar=true;
        String notSplitWords=w.getWord();
        String splitWords="";
        while (hasChar&&hasSplit<splitLevel) {
            //从左端分词，包括中英文
            String firstWord=WordUtils.splitFirstWord(notSplitWords);
            notSplitWords=notSplitWords.substring(0+firstWord.length());

            splitWords+=firstWord;
            //把搜索词放入搜索结构
            MiddleWord tempMw=middleWordMap.get(splitWords);
            if (tempMw==null) {
                tempMw=new MiddleWord(splitWords);
                middleWordMap.put(splitWords, tempMw);
            }
            tempMw.insertWord(w);
            //处理拼音
            String swPy=ChineseCharactersUtils.getFullSpellFirstUp(splitWords);
            swPy=swPy.toLowerCase();
            tempMw=pyMiddleWordMap.get(swPy);
            if (tempMw==null) {
                tempMw=new MiddleWord(swPy);
                pyMiddleWordMap.put(swPy, tempMw);
            }
            tempMw.insertWord(w);

            hasChar=(notSplitWords.length()>0);
            hasSplit++;
        }
    }
}