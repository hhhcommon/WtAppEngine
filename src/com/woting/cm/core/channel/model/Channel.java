package com.woting.cm.core.channel.model;

import java.io.Serializable;
import java.sql.Timestamp;

import com.spiritdata.framework.core.model.ModelSwapPo;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.exceptionC.Plat0006CException;
import com.spiritdata.framework.util.ChineseCharactersUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.channel.persis.po.ChannelPo;
import com.woting.cm.core.common.model.Owner;

public class Channel extends TreeNodeBean implements Serializable, ModelSwapPo {
    private static final long serialVersionUID=-8650564185354893561L;

    //private String id;  //表ID(UUID),在TreeNodeBean中对应id
    //private String pId; //上级栏目ID，根为null，在TreeNodeBean中对应parentId
    private Owner owner;
    //private String channelName;  //栏目名称，在TreeNodeBean中对应nodeName
    private String NPy;  //名称拼音
    private int isValidate;  //是否生效(1-生效,2-无效)
    private int sort;  //栏目排序,从大到小排序，越大越靠前，根下同级别
    private String contentType;  //允许资源的类型，可以是多个，0所有；1电台；2单体媒体资源；3专辑资源；用逗号隔开，比如“1,2”，目前都是0
    private String channelImg;  //
    private String descn; //说明
    private Timestamp CTime; //记录创建时间

    public Owner getOwner() {
        return owner;
    }
    public void setOwner(Owner owner) {
        this.owner=owner;
    }
    public String getNPy() {
        if (StringUtils.isNullOrEmptyOrSpace(NPy)&&!StringUtils.isNullOrEmptyOrSpace(getNodeName())) {
            NPy=ChineseCharactersUtils.getFullSpellFirstUp(this.getNodeName());
        }
        return NPy;
    }
    public void setNPy(String nPy) {
        NPy=nPy;
    }
    public int getIsValidate() {
        return isValidate;
    }
    public void setIsValidate(int isValidate) {
        this.isValidate=isValidate;
    }
    public int getSort() {
        return sort;
    }
    public void setSort(int sort) {
        this.sort=sort;
    }
    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType=contentType;
    }
    public String getChannelImg() {
        return channelImg;
    }
    public void setChannelImg(String channelImg) {
        this.channelImg=channelImg;
    }
    public String getDescn() {
        return descn;
    }
    public void setDescn(String descn) {
        this.descn=descn;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime=cTime;
    }

    /**
     * 改写基类的该方法，使其能够自动设置汉语拼音
     */
    public void setChannelName(String cName) {
        super.setNodeName(cName);
        if (StringUtils.isNullOrEmptyOrSpace(this.NPy)) this.NPy=ChineseCharactersUtils.getFullSpellFirstUp(cName);
    }
    @Override
    public void setNodeName(String cName) {
        super.setNodeName(cName);
        NPy=ChineseCharactersUtils.getFullSpellFirstUp(cName);
    }

    @Override
    public ChannelPo convert2Po() {
        ChannelPo ret = new ChannelPo();
        //id处理，没有id，自动生成一个
        if (StringUtils.isNullOrEmptyOrSpace(this.getId())) ret.setId(SequenceUUID.getPureUUID());
        else ret.setId(this.getId());

        ret.setPcId(getParentId());
        ret.setOwnerId(owner.getOwnerId());
        ret.setOwnerType(owner.getOwnerType());
        ret.setSort(sort);
        ret.setChannelName(getNodeName());
        ret.setIsValidate(isValidate);
        ret.setNPy(getNPy());
        ret.setContentType(contentType);
        ret.setChannelImg(channelImg);
        ret.setDescn(descn);
        ret.setCTime(CTime);

        return ret;
    }

    @Override
    public void buildFromPo(Object po) {
        if (po==null) throw new Plat0006CException("Po对象为空，无法从空对象得到概念/逻辑对象！");
        if (!(po instanceof ChannelPo)) throw new Plat0006CException("Po对象不是ChannelPo的实例，无法从此对象构建栏目对象！");
        ChannelPo _po = (ChannelPo)po;

        setId(_po.getId());
        Owner o=new Owner();
        o.setOwnerType(_po.getOwnerType());
        o.setOwnerId(_po.getOwnerId());
        owner=o;
        setNodeName(_po.getChannelName());
        setParentId(_po.getPcId());
        isValidate=_po.getIsValidate();
        sort=_po.getSort();
        contentType=_po.getContentType();
        channelImg=_po.getChannelImg();
        descn=_po.getDescn();
        CTime=_po.getCTime();
    }
}