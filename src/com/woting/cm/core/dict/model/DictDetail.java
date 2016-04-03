package com.woting.cm.core.dict.model;

import java.io.Serializable;
import java.sql.Timestamp;

import com.spiritdata.framework.core.model.ModelSwapPo;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.exceptionC.Plat0006CException;
import com.spiritdata.framework.util.ChineseCharactersUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.dict.persis.po.DictDetailPo;

/**
 * 字典项
 * 对应持久化中数据库的表为PLAT_DICTD
 * @author wh
 */
public class DictDetail extends TreeNodeBean implements Serializable, ModelSwapPo {
    private static final long serialVersionUID = -4154673243407172158L;

    //String id; //字典项ID，在TreeNodeBean中对应id
    private String MId; //字典组ID
    //String pId; //上级字典项ID，若为直接属于某字典组的字典项，则此只为0，在TreeNodeBean中对应parentId
    //int order; //排序，在TreeNodeBean中对应Order
    private int isValidate; //字典组是否可用 1可用，2不可用
    //String ddName; //字典项名称，在TreeNodeBean中对应nodeName
    private String NPy; //字典名称拼音
    private String aliasName; //字典项别名
    private String anPy; //字典项别名拼音
    private String BCode; //字典项业务编码
    private int DType; //字典项类型：1系统保留；2系统；3定义；4引用：其他字典项ID
    private String DRef; //字典项引用
    private String desc; //说明
    private Timestamp CTime; //记录创建时间
    private Timestamp lmTime; //记录最后修改时间

    public String getMId() {
        return MId;
    }
    public void setMId(String MId) {
        this.MId = MId;
    }
    public int getIsValidate() {
        return isValidate;
    }
    public void setIsValidate(int isValidate) {
        this.isValidate = isValidate;
    }

    /**
     * 改写基类的该方法，使其能够自动设置汉语拼音
     */
    public void setDdName(String ddName) {
        this.setNodeName(ddName);
        if (StringUtils.isNullOrEmptyOrSpace(this.NPy)) this.NPy=ChineseCharactersUtils.getFullSpellFirstUp(ddName);
    }

    public String getNPy() {
        if (StringUtils.isNullOrEmptyOrSpace(this.NPy)&&!StringUtils.isNullOrEmptyOrSpace(this.getNodeName())) {
            this.NPy=ChineseCharactersUtils.getFullSpellFirstUp(this.getNodeName());
        }
        return this.NPy;
    }

    public String getAliasName() {
        return aliasName;
    }
    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
        if (StringUtils.isNullOrEmptyOrSpace(this.anPy)) this.anPy=ChineseCharactersUtils.getFullSpellFirstUp(aliasName);
    }
    public String getAnPy() {
        if (StringUtils.isNullOrEmptyOrSpace(this.NPy)&&!StringUtils.isNullOrEmptyOrSpace(this.getNodeName())) {
            this.NPy=ChineseCharactersUtils.getFullSpellFirstUp(this.getNodeName());
        }
        return this.anPy;
    }

    public String getBCode() {
        return BCode;
    }
    public void setBCode(String bCode) {
        BCode = bCode;
    }
    public int getDType() {
        return DType;
    }
    public void setDType(int dType) {
        DType = dType;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime = cTime;
    }
    public String getDRef() {
        return DRef;
    }
    public void setDRef(String dRef) {
        DRef = dRef;
    }
    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }
    public Timestamp getLmTime() {
        return lmTime;
    }
    public void setLmTime(Timestamp lmTime) {
        this.lmTime = lmTime;
    }

    @Override
    public DictDetailPo convert2Po() {
        DictDetailPo ret = new DictDetailPo();
        //id处理，没有id，自动生成一个
        if (StringUtils.isNullOrEmptyOrSpace(this.getId())) ret.setId(SequenceUUID.getPureUUID());
        else ret.setId(this.getId());

        ret.setMId(this.MId);
        ret.setParentId(this.getParentId());
        ret.setSort(this.getOrder());
        ret.setIsValidate(this.isValidate);
        ret.setDdName(this.getNodeName());
        ret.setNPy(this.getNPy());
        ret.setAliasName(this.aliasName);
        ret.setAnPy(this.getAnPy());
        ret.setBCode(this.BCode);
        ret.setDType(this.DType);
        ret.setDRef(this.DRef);
        ret.setDesc(this.desc);
        ret.setCTime(this.CTime);
        ret.setLmTime(this.lmTime);

        return ret;
    }

    @Override
    public void buildFromPo(Object po) {
        if (po==null) throw new Plat0006CException("Po对象为空，无法从空对象得到概念/逻辑对象！");
        if (!(po instanceof DictDetailPo)) throw new Plat0006CException("Po对象不是DictDetailPo的实例，无法从此对象构建字典项对象！");
        DictDetailPo _po = (DictDetailPo)po;
        this.setId(_po.getId());
        this.MId=_po.getMId();
        this.setParentId(_po.getParentId());
        this.setOrder(_po.getSort());
        this.setOrderType(0);//从大到小
        this.isValidate=_po.getIsValidate();
        this.setDdName(_po.getDdName());
        this.setAliasName(_po.getAliasName());
        this.BCode=_po.getBCode();
        this.DType=_po.getDType();
        this.DRef=_po.getDRef();
        this.desc=_po.getDesc();
        this.CTime=_po.getCTime();
        this.lmTime=_po.getLmTime();
    }
}