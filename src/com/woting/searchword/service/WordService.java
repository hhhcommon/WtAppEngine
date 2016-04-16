package com.woting.searchword.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.woting.cm.core.common.model.Owner;
import com.woting.searchword.mem.SearchWordMemory;
import com.woting.searchword.model.Word;

@Lazy(true)
@Service
public class WordService {

    SearchWordMemory swm=SearchWordMemory.getInstance();

    /**
     * 加入属于某一用户的一个搜索词
     * @param oneWord 搜索词
     * @param o 所属用户
     */
    public void addOneWord(String oneWord, Owner o) {
        swm.addWord2Queue(o.getOwnerId()+"::"+o.getOwnerType()+"::"+oneWord);
    }

    /**
     * 根据中间次得到某一用户的搜索热词
     * 有历史记录功能
     * @param middleWord 搜索词
     * @param o 所属用户，可以为空
     * @param rType 返回类型：0=默认值，按照一个列表返回(混合公共和私有)；1=按照分类返回，私有和公共在两个列表中
     * @param topSize 返回多少个搜索词，若rType=0，则返回的所有数据的个数；若rType=1，则返回的两个列表，每个列表的搜索词个数；若结果不足size，则返回所有结果
     * @return 排好序的搜索词列表：<pre>
     *  rType=0 (List<String>)[0]是所有搜索词列表，(List<String>)[0].size()<={参数size}
     *  rType=1 (List<String>)[0]是公共搜索词列表，(List<String>)[1].size()<={参数size}
     *          (List<String>)[1]是私有搜索词列表，(List<String>)[0].size()<={参数size}
     * </pre>
     */
    public List<String>[] getHotWords(String middleWord, Owner o, int rType, int topSize) {
        List<Word> sysWords=null;//系统热词
        List<Word> ownerWords=null;//用户热词
        List<Word> allWords=null;//所有热词，只有当rType==0时才有用

        //1-获得热词
        Owner sysO=new Owner(100, "cm");
        sysWords=swm.getTopWordList(middleWord, sysO, topSize);
        if (o!=null&&!o.equals(sysO)) ownerWords=swm.getTopWordList(middleWord, o, topSize);

        //2-整理数据
        if (ownerWords==null&&sysWords!=null) ownerWords=new ArrayList<Word>();

        //3-若需要合并
        if (rType==0&&ownerWords!=null&&sysWords!=null) {//合并merge
            for (Word w: sysWords) insertWord2OwnerWordList(ownerWords, w);
            allWords=new ArrayList<Word>();
            for (int i=0; i<(topSize>ownerWords.size()?ownerWords.size():topSize); i++) {
                allWords.add(ownerWords.get(i));
            }
        }

        //4-组织返回数据
        List<String>[] wordsList=new List[1];
        if (allWords!=null) {
            if (allWords.isEmpty()) return null;
            List<String> words=new ArrayList<String>();
            for (Word w: allWords) words.add(w.getWord());
            wordsList[0]=words;
            return wordsList;
        } else {
            if ((ownerWords==null||ownerWords.isEmpty())&&(sysWords==null||sysWords.isEmpty())) return null;
            if (sysWords!=null&&!sysWords.isEmpty()) wordsList=new List[2];
            if (ownerWords!=null&&!ownerWords.isEmpty()) {
                List<String> words=new ArrayList<String>();
                for (Word w: ownerWords) words.add(w.getWord());
                wordsList[0]=words;
            }
            if (sysWords!=null&&!sysWords.isEmpty()) {
                List<String> words=new ArrayList<String>();
                for (Word w: sysWords) words.add(w.getWord());
                wordsList[1]=words;
            }
            return wordsList;
        }
    }

    private void insertWord2OwnerWordList(List<Word> ownerWordList, Word word) {
        int insertIndex=-1, flag=0;
        for (int i=ownerWordList.size()-1; i>=0; i--) {
            if (flag>1) break;
            Word _w=ownerWordList.get(i);
            if (_w.getCount()*100<word.getCount()) {
                insertIndex=i;
            } else break;
        }
        if (insertIndex!=-1) {
            ownerWordList.add(insertIndex, word);
        }
    }
}