package com.woting.cm.core.dict.persis.po;

import java.sql.Timestamp;
import com.spiritdata.framework.core.model.BaseObject;

public class DictMasterPo extends BaseObject {
    private static final long serialVersionUID = -5935730569262158194L;

    private String id; //字典组id
    private String ownerId; //所有者标识（可能是用户id，也可能是SessionID）
    private int ownerType; //方案所对应的所有者类型（1=注册用户;2=非注册用户(session)）
    private String dmName; //字典组名称
    private String NPy; //字典组名称拼音
    private int sort; //排序号，越大越靠前
    private int isValidate; //字典组是否可用 1可用，2不可用
    private int MType; //字典组类型：1系统保留；2系统；3定义；
    private String MRef; //字典组引用，当mType=3
    private String desc; //说明
    private Timestamp CTime; //记录创建时间
    private Timestamp lmTime; //最后修改时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    public int getOwnerType() {
        return ownerType;
    }
    public void setOwnerType(int ownerType) {
        this.ownerType = ownerType;
    }
    public String getDmName() {
        return dmName;
    }
    public void setDmName(String dmName) {
        this.dmName = dmName;
    }
    public String getNPy() {
        return NPy;
    }
    public void setNPy(String nPy) {
        NPy = nPy;
    }
    public int getSort() {
        return sort;
    }
    public void setSort(int sort) {
        this.sort = sort;
    }
    public int getIsValidate() {
        return isValidate;
    }
    public void setIsValidate(int isValidate) {
        this.isValidate = isValidate;
    }
    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }
    public int getMType() {
        return MType;
    }
    public void setMType(int mType) {
        MType = mType;
    }
    public String getMRef() {
        return MRef;
    }
    public void setMRef(String mRef) {
        MRef = mRef;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime = cTime;
    }
    public Timestamp getLmTime() {
        return lmTime;
    }
    public void setLmTime(Timestamp lmTime) {
        this.lmTime = lmTime;
    }
}