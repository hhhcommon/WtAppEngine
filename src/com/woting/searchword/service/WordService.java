package com.woting.searchword.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.common.model.Owner;
import com.woting.searchword.mem.SearchWordMemory;
import com.woting.searchword.model.OwnerWord;
import com.woting.searchword.model.Word;

@Lazy(true)
@Service
public class WordService {

    SearchWordMemory swm=SearchWordMemory.getInstance();

    //======================================================================================================
    //以下为内存管理部分
    /**
     * 加入属于某一用户的一个搜索词
     * @param oneWord 搜索词的字符串
     * @param o 所属用户
     */
    public void addWord2Online(String oneWord, Owner o) {
        swm.addWord2OnlineQueue(o.getOwnerId()+"::"+o.getOwnerType()+"::"+oneWord);
    }

    /**
     * 加入属于某一用户的一个搜索词。此方法用于加载已统计的敏感词
     * @param oneWord 搜索词对象
     * @param o 所属用户
     */
    public void addWord2LoadQueue(Word oneWord, Owner o) {
        swm.addWord2LoadQueue(o.getOwnerId()+"::"+o.getOwnerType()+"::"+oneWord.getWord()+"::"+oneWord.getCount());
    }

    /**
     * 处理在线搜索词，加载入内存，并写入持久化的数据库
     */
    public void dealWordOnlineQueue() {
        String _oneWord=swm.pollFromOnlineQueue();
        if (!StringUtils.isNullOrEmptyOrSpace(_oneWord)) {
            String[] _split=_oneWord.split("::");
            if (_split.length==3) {
                Owner o=new Owner(Integer.parseInt(_split[1]), _split[0]);
                Owner sysO=new Owner(100, "cm");

                OwnerWord ow=swm.getOwnerWord(o);
                if (ow==null) swm.putOwnerWord(new OwnerWord(o, 5));
                ow.addWord(_split[2]);
                if (!o.equals(sysO)) {
                    ow=swm.getOwnerWord(sysO);
                    if (ow==null) swm.putOwnerWord(new OwnerWord(sysO, 5));
                    ow.addWord(_split[2]);
                }
                //数据库处理
            }
        }
    }

    /**
     * 处理加载搜索词，加载入内存
     */
    public void dealWordLoadQueue() {
        String _oneWord=swm.pollFromLoadQueue();
        if (!StringUtils.isNullOrEmptyOrSpace(_oneWord)) {
            String[] _split=_oneWord.split("::");
            if (_split.length==4) {
                Owner o=new Owner(Integer.parseInt(_split[1]), _split[0]);
                Owner sysO=new Owner(100, "cm");

                //总要处理一次系统
                OwnerWord ow=swm.getOwnerWord(sysO);
                if (ow==null) swm.putOwnerWord(new OwnerWord(sysO, 5));
                ow.loadWord(new Word(_split[2], Integer.parseInt(_split[3])));
                //再处理一次自己
                if (!o.equals(sysO)) {
                    ow=swm.getOwnerWord(o);
                    if (ow==null) swm.putOwnerWord(new OwnerWord(o, 5));
                    ow.loadWord(new Word(_split[2], Integer.parseInt(_split[3])));
                }
            }
        }
    }

    //======================================================================================================
    //以下为业务功能服务
    /**
     * 得到某一用户的搜索热词
     * 有历史记录功能
     * @param o 所属用户，可以为空
     * @param rType 返回类型：1=默认值，按照一个列表返回(混合公共和私有)；2=按照分类返回，私有和公共在两个列表中
     * @param topSize 返回多少个搜索词，若rType=1，则返回的所有数据的个数；若rType=2，则返回的两个列表，每个列表的搜索词个数；若结果不足size，则返回所有结果
     * @return 排好序的搜索词列表：<pre>
     *  rType=1 (List<String>)[0]是所有搜索词列表，(List<String>)[0].size()<={参数size}
     *  rType=2 (List<String>)[0]是公共搜索词列表，(List<String>)[1].size()<={参数size}
     *          (List<String>)[1]是私有搜索词列表，(List<String>)[0].size()<={参数size}
     * </pre>
     */
    public List<String>[] getHotWords( Owner o, int rType, int topSize) {
        List<Word> sysWords=null;//系统热词
        List<Word> ownerWords=null;//用户热词
        List<Word> allWords=null;//所有热词，只有当rType==0时才有用

        //1-获得热词
        Owner sysO=new Owner(100, "cm");
        sysWords=swm.getTopWordList(sysO, topSize);
        if (o!=null&&!o.equals(sysO)) ownerWords=swm.getTopWordList(o, topSize);

        //2-整理数据
        if (ownerWords==null&&sysWords!=null) ownerWords=new ArrayList<Word>();

        //3-若需要合并
        if (rType==1&&ownerWords!=null&&sysWords!=null) {//合并merge
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
            if (sysWords!=null&&!sysWords.isEmpty()) {
                List<String> words=new ArrayList<String>();
                for (Word w: sysWords) words.add(w.getWord());
                wordsList[0]=words;
            }
            if (ownerWords!=null&&!ownerWords.isEmpty()) {
                List<String> words=new ArrayList<String>();
                for (Word w: ownerWords) words.add(w.getWord());
                wordsList[1]=words;
            }
            return wordsList;
        }
    }

    /**
     * 根据中间次查找某一用户的搜索热词
     * 有历史记录功能
     * @param middleWord 搜索词
     * @param o 所属用户，可以为空
     * @param rType 返回类型：1=默认值，按照一个列表返回(混合公共和私有)；2=按照分类返回，私有和公共在两个列表中
     * @param topSize 返回多少个搜索词，若rType=1，则返回的所有数据的个数；若rType=2，则返回的两个列表，每个列表的搜索词个数；若结果不足size，则返回所有结果
     * @return 排好序的搜索词列表：<pre>
     *  rType=1 (List<String>)[0]是所有搜索词列表，(List<String>)[0].size()<={参数size}
     *  rType=2 (List<String>)[0]是公共搜索词列表，(List<String>)[1].size()<={参数size}
     *          (List<String>)[1]是私有搜索词列表，(List<String>)[0].size()<={参数size}
     * </pre>
     */
    public List<String>[] searchHotWords(String middleWord, Owner o, int rType, int topSize) {
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
        if (rType==1&&ownerWords!=null&&sysWords!=null) {//合并merge
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
            if (sysWords!=null&&!sysWords.isEmpty()) {
                List<String> words=new ArrayList<String>();
                for (Word w: sysWords) words.add(w.getWord());
                wordsList[0]=words;
            }
            if (ownerWords!=null&&!ownerWords.isEmpty()) {
                List<String> words=new ArrayList<String>();
                for (Word w: ownerWords) words.add(w.getWord());
                wordsList[1]=words;
            }
            return wordsList;
        }
    }

    //======================================================================================================
    //以下为私有功能
    private void insertWord2OwnerWordList(List<Word> ownerWordList, Word word) {
        //先检查是否已经存在
        for (int i=0; i<ownerWordList.size(); i++) {
            if (ownerWordList.get(i).getWord().equals(word.getWord())) return;
        }
        int insertIndex=-1, flag=0;
        if (ownerWordList.size()==0) insertIndex=0;
        else {
            for (int i=ownerWordList.size()-1; i>=0; i--) {
                if (flag>1) break;
                Word _w=ownerWordList.get(i);
                if (_w.getCount()*100<word.getCount()) {
                    insertIndex=i;
                } else {
                    insertIndex=i+1;
                    break;
                }
            }
        }
        if (insertIndex!=-1) {
            ownerWordList.add(insertIndex, word);
        }
    }

    public void loadUserWord() {
        // TODO Auto-generated method stub
        
    }

}