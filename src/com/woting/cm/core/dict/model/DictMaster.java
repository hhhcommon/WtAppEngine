package com.woting.cm.core.dict.model;

import java.io.Serializable;
import java.sql.Timestamp;

import com.spiritdata.framework.core.model.ModelSwapPo;
import com.spiritdata.framework.exceptionC.Plat0006CException;
import com.spiritdata.framework.util.ChineseCharactersUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.common.model.Owner;
import com.woting.cm.core.dict.persis.po.DictMasterPo;

/**
 * 字典组
 * 对应持久化中数据库的表为PLAT_DICTM
 * @author wh
 */
public class DictMaster implements Serializable, ModelSwapPo {
    private static final long serialVersionUID = -5935730569262158194L;

    private String id; //字典组id
    private Owner owner;
    private String dmName; //字典组名称
    private String NPy; //字典组名称拼音
    private int order; //排序号，越大越靠前
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
    public Owner getOwner() {
        return owner;
    }
    public void setOwner(Owner owner) {
        this.owner = owner;
    }
    public String getDmName() {
        return dmName;
    }
    public void setDmName(String dmName) {
        this.dmName = dmName;
        if (StringUtils.isNullOrEmptyOrSpace(this.NPy)) this.NPy=ChineseCharactersUtils.getFullSpellFirstUp(this.dmName);
    }
    public int getOrder() {
        return order;
    }
    public void setOrder(int order) {
        this.order = order;
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

    /**
     * 得到字典名称拼音
     * @return 字典名称拼音
     */
    public String getNPy() {
        if (StringUtils.isNullOrEmptyOrSpace(this.NPy)&&!StringUtils.isNullOrEmptyOrSpace(this.dmName)) {
            this.NPy=ChineseCharactersUtils.getFullSpellFirstUp(this.dmName);
        }
        return this.NPy;
    }

    @Override
    public DictMasterPo convert2Po() {
        DictMasterPo ret = new DictMasterPo();
        //id处理，没有id，自动生成一个
        if (StringUtils.isNullOrEmptyOrSpace(this.id)) ret.setId(SequenceUUID.getPureUUID());
        else ret.setId(this.id);

        ret.setOwnerId(this.owner.getOwnerId());
        ret.setOwnerType(this.owner.getOwnerType());
        ret.setDmName(this.dmName);
        ret.setNPy(this.getNPy());
        ret.setSort(this.order);
        ret.setIsValidate(this.isValidate);
        ret.setMRef(this.MRef);
        ret.setMType(this.MType);
        ret.setDesc(this.desc);
        ret.setCTime(this.CTime);
        ret.setLmTime(this.lmTime);
        return ret;
    }

    @Override
    public void buildFromPo(Object po) {
        if (po==null) throw new Plat0006CException("Po对象为空，无法从空对象得到概念/逻辑对象！");
        if (!(po instanceof DictMasterPo)) throw new Plat0006CException("Po对象不是DictMasterPo的实例，无法从此对象构建字典组对象！");
        DictMasterPo _po = (DictMasterPo)po;
        this.id =_po.getId();
        this.owner = new Owner(_po.getOwnerType(), _po.getOwnerId());
        this.setDmName(_po.getDmName());
        this.order = _po.getSort();
        this.isValidate = _po.getIsValidate();
        this.MRef = _po.getMRef();
        this.MType = _po.getMType();
        this.desc = _po.getDesc();
        this.CTime = _po.getCTime();
        this.lmTime = _po.getLmTime();
    }
}