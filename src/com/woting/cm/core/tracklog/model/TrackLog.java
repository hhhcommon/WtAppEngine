package com.woting.cm.core.tracklog.model;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.ModelSwapPo;
import com.spiritdata.framework.exceptionC.Plat0006CException;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.common.model.Owner;
import com.woting.cm.core.tracklog.persis.po.TrackLogPo;

public class TrackLog implements ModelSwapPo {

    private String id; //表ID
    private String tableName; //表名称
    private String objId; //表内记录Id
    private int dataClass; //内容分类：0-其他(未知);1字典-分类;2字典-关键词;3单曲节目;4系列节目(包括与单曲的关系);5栏目;6各种其他关系
    private Owner owner; //源数据所有者
    private String data; //源数据内容，Json格式
    private String dataMd5; //源数据内容的,Json格式，用于比较
    private int dealFlag; //处理状态：1=Insert;2=Update;3=Del;4=仅对比了
    private Owner oper; //操作者
    private String rules; //只有当是系统处理时，此字段才有意义，对应的规则Id,可以对应多个规则，为空则是手工处理
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
    public Owner getOwner() {
        return owner;
    }
    public void setOwner(Owner owner) {
        this.owner = owner;
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
    public Owner getOper() {
        return oper;
    }
    public void setOper(Owner oper) {
        this.oper = oper;
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

    @Override
    public TrackLogPo convert2Po() {
        TrackLogPo ret = new TrackLogPo();
        //id处理，没有id，自动生成一个
        if (StringUtils.isNullOrEmptyOrSpace(this.getId())) ret.setId(SequenceUUID.getPureUUID());
        else ret.setId(this.getId());

        ret.setTableName(tableName);
        ret.setObjId(objId);
        ret.setDataClass(dataClass);
        ret.setOwnerType(owner.getOwnerType());
        ret.setOwnerId(owner.getOwnerId());
        ret.setData(data);
        ret.setDataMd5(dataMd5);
        ret.setDealFlag(dealFlag);
        ret.setOperType(oper.getOwnerType());
        ret.setOperId(oper.getOwnerId());
        ret.setRules(rules);
        ret.setCTime(this.CTime);

        return ret;
    }

    @Override
    public void buildFromPo(Object po) {
        if (po==null) throw new Plat0006CException("Po对象为空，无法从空对象得到概念/逻辑对象！");
        if (!(po instanceof TrackLogPo)) throw new Plat0006CException("Po对象不是OperLogPo的实例，无法从此对象构建操作日志记录对象！");

        TrackLogPo _po=(TrackLogPo)po;
        this.tableName=_po.getTableName();
        this.objId=_po.getObjId();
        this.dataClass=_po.getDataClass();
        this.data=_po.getData();
        this.dataMd5=_po.getDataMd5();
        this.dealFlag=_po.getDealFlag();
        this.rules=_po.getRules();
        this.CTime=_po.getCTime();
        this.owner=new Owner(_po.getOwnerType(), _po.getOwnerId());
        this.oper=new Owner(_po.getOperType(), _po.getOperId());
    }
}