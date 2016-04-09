package com.woting.cm.core.dict.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class DictRefResPo extends BaseObject {
    private static final long serialVersionUID=-3984169450616227079L;

    private String id; //uuid(主键)
    private String refName; //关系名称：resType+dictMId=唯一关系名称，既相当于某类资源的一个字段
    private String resTableName; //资源类型Id：1电台；2单体媒体资源；3专辑资源
    private String resId; //资源Id
    private String dictMid; //字典组Id
    private String dictMName; //字典组名称
    private String dictDid; //字典项Id
    private String title; //字典项名称
    private String BCode; //字典项业务编码
    private String pathNames; //字典项全名称
    private String pathIds; //字典项路径Id
    private Timestamp CTime; //创建时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id=id;
    }
    public String getRefName() {
        return refName;
    }
    public void setRefName(String refName) {
        this.refName=refName;
    }
    public String getResTableName() {
        return resTableName;
    }
    public void setResTableName(String resTableName) {
        this.resTableName=resTableName;
    }
    public String getResId() {
        return resId;
    }
    public void setResId(String resId) {
        this.resId=resId;
    }
    public String getDictMid() {
        return dictMid;
    }
    public void setDictMid(String dictMid) {
        this.dictMid=dictMid;
    }
    public String getDictMName() {
        return dictMName;
    }
    public void setDictMName(String dictMName) {
        this.dictMName=dictMName;
    }
    public String getDictDid() {
        return dictDid;
    }
    public void setDictDid(String dictDid) {
        this.dictDid=dictDid;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title=title;
    }
    public String getBCode() {
        return BCode;
    }
    public void setBCode(String BCode) {
        this.BCode=BCode;
    }
    public String getPathNames() {
        return pathNames;
    }
    public void setPathNames(String pathNames) {
        this.pathNames=pathNames;
    }
    public String getPathIds() {
        return pathIds;
    }
    public void setPathIds(String pathIds) {
        this.pathIds=pathIds;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime=cTime;
    }
}