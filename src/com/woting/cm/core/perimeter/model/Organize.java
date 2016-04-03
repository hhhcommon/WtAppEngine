package com.woting.cm.core.perimeter.model;

import java.io.Serializable;
import java.sql.Timestamp;

import com.spiritdata.framework.core.model.ModelSwapPo;
import com.spiritdata.framework.exceptionC.Plat0006CException;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.dict.model.DictDetail;
import com.woting.cm.core.perimeter.persis.po.OrganizePo;

public class Organize implements Serializable, ModelSwapPo {
    private static final long serialVersionUID=4192211700171467870L;

    private String id;  //uuid(用户id)
    private String orgName; //名称，组织名称
    private DictDetail orgType; //组织类型，字典项
    private String webPageUrl; //官网地址
    private String orgImg; //组织Logo
    private String descn; //对组织的描述
    private Timestamp CTime; //记录创建时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id=id;
    }
    public String getOrgName() {
        return orgName;
    }
    public void setOrgName(String orgName) {
        this.orgName=orgName;
    }
    public DictDetail getOrgType() {
        return orgType;
    }
    public void setOrgType(DictDetail orgType) {
        this.orgType=orgType;
    }
    public String getWebPageUrl() {
        return webPageUrl;
    }
    public void setWebPageUrl(String webPageUrl) {
        this.webPageUrl=webPageUrl;
    }
    public String getOrgImg() {
        return orgImg;
    }
    public void setOrgImg(String orgImg) {
        this.orgImg=orgImg;
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

    @Override
    public OrganizePo convert2Po() {
        OrganizePo ret=new OrganizePo();
        //id处理，没有id，自动生成一个
        if (StringUtils.isNullOrEmptyOrSpace(this.id)) ret.setId(SequenceUUID.getUUIDSubSegment(4));
        else ret.setId(this.id);

        ret.setOrgName(orgName);
        ret.setOrgTypeId(orgType.getId());
        ret.setOrgTypeName(orgType.getNodeName());
        ret.setOrgImg(orgImg);
        ret.setWebPageUrl(webPageUrl);
        ret.setDescn(descn);
        ret.setCTime(this.CTime);

        return ret;
    }

    @Override
    public void buildFromPo(Object po) {
        if (po==null) throw new Plat0006CException("Po对象为空，无法从空对象得到概念/逻辑对象！");
        if (!(po instanceof OrganizePo)) throw new Plat0006CException("Po对象不是OrganizePo的实例，无法从此对象构建组织机构对象！");
        OrganizePo _po=(OrganizePo)po;

        this.id =_po.getId();
        this.orgName=_po.getOrgName();
        this.webPageUrl=_po.getWebPageUrl();
        this.orgImg=_po.getOrgImg();
        this.descn=_po.getDescn();
        this.CTime=_po.getCTime();

        //组织分类字典——没有办法现在直接处理，先只把值记录下来
        DictDetail dd=new DictDetail();
        dd.setId(_po.getOrgTypeId());
        dd.setNodeName(_po.getOrgTypeName());
        this.orgType=dd;
    }
}