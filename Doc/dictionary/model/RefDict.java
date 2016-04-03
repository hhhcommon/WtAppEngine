package com.woting.dictionary.model;

public class RefDict {
    private String dmId; //字典组Id
    private String ddId; //字典项Id
    private String ddCode; //字典项编码
    private String ddName; //字典组名称
    private String dmName; //字典项名称
    private String allName; //全名称
    private Object refObj;  //关联对象
    public String getDmId() {
        return dmId;
    }
    public void setDmId(String dmId) {
        this.dmId = dmId;
    }
    public String getDdId() {
        return ddId;
    }
    public void setDdId(String ddId) {
        this.ddId = ddId;
    }
    public String getDdCode() {
        return ddCode;
    }
    public void setDdCode(String ddCode) {
        this.ddCode = ddCode;
    }
    public String getDdName() {
        return ddName;
    }
    public void setDdName(String ddName) {
        this.ddName = ddName;
    }
    public String getDmName() {
        return dmName;
    }
    public void setDmName(String dmName) {
        this.dmName = dmName;
    }
    public String getAllName() {
        return allName;
    }
    public void setAllName(String allName) {
        this.allName = allName;
    }
    public Object getRefObj() {
        return refObj;
    }
    public void setRefObj(Object refObj) {
        this.refObj = refObj;
    }
}