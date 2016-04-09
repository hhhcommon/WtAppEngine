package com.woting.cm.core.tracklog.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class TrackLogPo extends BaseObject {
    private static final long serialVersionUID = -8955374170572855731L;

    private String id; //表ID
    private String tableName; //表名称
    private String objId; //表内记录Id
    private int dataClass; //内容分类：1字典-分类;2字典-关键词;3单曲节目;4系列节目(包括与单曲的关系);5栏目;6各种其他关系
    private int ownerType; //源数据所有者分类，这里只有101
    private String ownerId; //源数据所有者机构Id，wt_Organize表中的Id
    private String data; //源数据内容，Json格式
    private String dataMd5; //源数据内容的,Json格式，用于比较
    private int dealFlag; //处理状态：1=Insert;2=Update;3=Del;4=仅对比了
    private int operType; //处理者分类：100 我们自己的系统;200 后台系统用户;201 前端用户，可参见Owner类型
    private String operId; //当operType=100，这里可以为空
    private String rules; //只有当是系统处理时，此字段才有意义，对应的规则Id,可以对应多个规则，为空则是不按规则处理的
    private Timestamp CTime; //记录创建时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getTableName() {
        return tableName;
    }
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    public String getObjId() {
        return objId;
    }
    public void setObjId(String objId) {
        this.objId = objId;
    }
    public int getDataClass() {
        return dataClass;
    }
    public void setDataClass(int dataClass) {
        this.dataClass = dataClass;
    }
    public int getOwnerType() {
        return ownerType;
    }
    public void setOwnerType(int ownerType) {
        this.ownerType = ownerType;
    }
    public String getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
    }
    public String getDataMd5() {
        return dataMd5;
    }
    public void setDataMd5(String dataMd5) {
        this.dataMd5 = dataMd5;
    }
    public int getDealFlag() {
        return dealFlag;
    }
    public void setDealFlag(int dealFlag) {
        this.dealFlag = dealFlag;
    }
    public int getOperType() {
        return operType;
    }
    public void setOperType(int operType) {
        this.operType = operType;
    }
    public String getOperId() {
        return operId;
    }
    public void setOperId(String operId) {
        this.operId = operId;
    }
    public String getRules() {
        return rules;
    }
    public void setRules(String rules) {
        this.rules = rules;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime = cTime;
    }
}