package com.woting.searchword.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class UserWordPo extends BaseObject {
    private static final long serialVersionUID=-8406881716350334538L;

    private String id; //用户词Id
    private int ownerType; //所有者类型
    private String ownerId; //所有者Id,可能是用户也可能是设备
    private String word; //搜索词
    private String wordLang; //搜索词语言类型，系统自动判断，可能是混合类型
    private Timestamp time1; //本词本用户首次搜索的时间
    private Timestamp time2; //本词本用户最后搜索的时间
    private int sumNum; //搜索次数
    private Timestamp CTime; //记录创建时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id=id;
    }
    public String getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(String ownerId) {
        this.ownerId=ownerId;
    }
    public int getOwnerType() {
        return ownerType;
    }
    public void setOwnerType(int ownerType) {
        this.ownerType=ownerType;
    }
    public String getWord() {
        return word;
    }
    public void setWord(String word) {
        this.word=word;
    }
    public String getWordLang() {
        return wordLang;
    }
    public void setWordLang(String wordLang) {
        this.wordLang=wordLang;
    }
    public Timestamp getTime1() {
        return time1;
    }
    public void setTime1(Timestamp time1) {
        this.time1=time1;
    }
    public Timestamp getTime2() {
        return time2;
    }
    public void setTime2(Timestamp time2) {
        this.time2=time2;
    }
    public int getSumNum() {
        return sumNum;
    }
    public void setSumNum(int sumNum) {
        this.sumNum=sumNum;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime=cTime;
    }
}