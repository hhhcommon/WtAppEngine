package com.woting.cm.core.media.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class SeqMaRefPo extends BaseObject {
    private static final long serialVersionUID = 6218513953585488836L;

    private String id;  //uuid(主键)0
    private String SId;  //系列Id,主表Id
    private String MId;  //媒体Id
    private int columnNum; //卷集号，也是排序号
    private String descn; //关联说明
    private Timestamp CTime; //创建时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getSId() {
        return SId;
    }
    public void setSId(String sId) {
        SId = sId;
    }
    public String getMId() {
        return MId;
    }
    public void setMId(String mId) {
        MId = mId;
    }
    public int getColumnNum() {
        return columnNum;
    }
    public void setColumnNum(int columnNum) {
        this.columnNum = columnNum;
    }
    public String getDescn() {
        return descn;
    }
    public void setDescn(String descn) {
        this.descn = descn;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime = cTime;
    }
}