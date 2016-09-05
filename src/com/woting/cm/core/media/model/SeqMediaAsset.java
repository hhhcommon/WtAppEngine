package com.woting.cm.core.media.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.spiritdata.framework.core.model.BaseObject;
import com.spiritdata.framework.core.model.ModelSwapPo;
import com.spiritdata.framework.exceptionC.Plat0006CException;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.dict.model.DictDetail;
import com.woting.cm.core.media.persis.po.SeqMediaAssetPo;
import com.woting.cm.core.perimeter.model.Organize;

public class SeqMediaAsset extends BaseObject implements Serializable, ModelSwapPo {
    private static final long serialVersionUID=8591338480388239961L;

    private String id; //uuid(主键)
    private String smaTitle; //专辑名称
    private int smaPubType; //发布者类型：1-组织表,2-文本
    private String smaPubId; //发布所属组织Id,或名称
    private String smaPublisher; //发布者名称,或描述
    private Timestamp smaPublishTime; //发布时间
    private String smaImg; //媒体图
    private int smaAllCount; //总卷集号，可以为空，这个和总数不同，也可能一样
    private String subjectWords; //主题词
    private String keyWords; //关键词
    private String descn; //说明
    private int pubCount; //发布状态：0未发布;>0被发布到多少个栏目中（系列节目的发布，这里的单曲也要被加1）
    private int smaStatus; //资源状态：0草稿;1提交（包括发布和未发布）
    private Timestamp CTime; //记录创建时间

    private Organize publisher; //发布者类型，比如逻辑思维团队
    private DictDetail lang; //发布语言，字典项
    private List<MediaAsset> subMaList; //所包含的单体节目

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id=id;
    }
    public String getSmaTitle() {
        return smaTitle;
    }
    public void setSmaTitle(String smaTitle) {
        this.smaTitle=smaTitle;
    }
    public int getSmaPubType() {
        return smaPubType;
    }
    public void setSmaPubType(int smaPubType) {
        this.smaPubType=smaPubType;
    }
    public String getSmaPubId() {
        return smaPubId;
    }
    public void setSmaPubId(String smaPubId) {
        this.smaPubId=smaPubId;
    }
    public String getSmaPublisher() {
        return smaPublisher;
    }
    public void setSmaPublisher(String smaPublisher) {
        this.smaPublisher=smaPublisher;
    }
    public Timestamp getSmaPublishTime() {
        return smaPublishTime;
    }
    public void setSmaPublishTime(Timestamp smaPublishTime) {
        this.smaPublishTime=smaPublishTime;
    }
    public String getSmaImg() {
        return smaImg;
    }
    public void setSmaImg(String smaImg) {
        this.smaImg=smaImg;
    }
    public int getSmaAllCount() {
        return smaAllCount;
    }
    public void setSmaAllCount(int smaAllCount) {
        this.smaAllCount=smaAllCount;
    }
    public String getSubjectWords() {
        return subjectWords;
    }
    public void setSubjectWords(String subjectWords) {
        this.subjectWords=subjectWords;
    }
    public String getKeyWords() {
        return keyWords;
    }
    public void setKeyWords(String keyWords) {
        this.keyWords=keyWords;
    }
    public String getDescn() {
        return descn;
    }
    public void setDescn(String descn) {
        this.descn=descn;
    }
    public int getPubCount() {
        return pubCount;
    }
    public void setPubCount(int pubCount) {
        this.pubCount=pubCount;
    }
    
    public int getSmaStatus() {
		return smaStatus;
	}
	public void setSmaStatus(int smaStatus) {
		this.smaStatus = smaStatus;
	}
	public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime=cTime;
    }
    public Organize getPublisher() {
        return publisher;
    }
    public void setPublisher(Organize publisher) {
        this.publisher=publisher;
        smaPubType=1;
        smaPubId=publisher.getId();
        smaPublisher=publisher.getOrgName();
    }
    public DictDetail getLang() {
        return lang;
    }
    public void setLang(DictDetail lang) {
        this.lang=lang;
    }
    public List<MediaAsset> getSubMaList() {
        return subMaList;
    }
    public void setSubMaList(List<MediaAsset> subMaList) {
        this.subMaList=subMaList;
    }
    public void addMa(MediaAsset ma) {
        if (ma==null) return;
        if (subMaList==null) {
            subMaList=new ArrayList<MediaAsset>();
            subMaList.add(ma);
        } else {
            boolean canAdd=true;
            for (MediaAsset _ma: subMaList) {
                if (_ma.equals(ma)) {
                    canAdd=false;
                    break;
                }
            }
            if (canAdd) subMaList.add(ma);
        }
        ma.addSeqMa(this);
    }

    @Override
    public SeqMediaAssetPo convert2Po() {
        SeqMediaAssetPo ret=new SeqMediaAssetPo();

        //id处理，没有id，自动生成一个
        if (StringUtils.isNullOrEmptyOrSpace(this.id)) ret.setId(SequenceUUID.getUUIDSubSegment(4));
        else ret.setId(this.id);

        ret.setSmaTitle(smaTitle);
        if (publisher!=null) {
            ret.setSmaPubType(1);
            ret.setSmaPubId(publisher.getId());
            ret.setSmaPublisher(publisher.getOrgName());
        } else {
            ret.setSmaPubType(2);
            ret.setSmaPubId(smaPubId);
            ret.setSmaPublisher(smaPublisher);
        }
        ret.setSmaPublishTime(smaPublishTime);
        ret.setSmaImg(smaImg);
        ret.setSubjectWords(subjectWords);
        ret.setKeyWords(keyWords);
        if (lang!=null) {
            ret.setLangDid(lang.getId());
            ret.setLanguage(lang.getNodeName());
        }
        ret.setDescn(descn);
        ret.setCTime(CTime);
        ret.setPubCount(pubCount);
        ret.setSmaStatus(smaStatus);

        return ret;
    }

    @Override
    public void buildFromPo(Object po) {
        if (po==null) throw new Plat0006CException("Po对象为空，无法从空对象得到概念/逻辑对象！");
        if (!(po instanceof SeqMediaAssetPo)) throw new Plat0006CException("Po对象不是SeqMediaAssetPo的实例，无法从此对象构建专辑对象！");
        SeqMediaAssetPo _po=(SeqMediaAssetPo)po;

        this.id=_po.getId();
        this.smaTitle=_po.getSmaTitle();
        //对应发布者——没有办法现在直接处理，先只把值记录下来
        this.smaPubType=_po.getSmaPubType();
        if (this.smaPubType==1) {
            this.publisher=new Organize();
            this.publisher.setId(_po.getSmaPubId());
            this.publisher.setOrgName(_po.getSmaPublisher());
        } else {
            this.smaPubId=_po.getSmaPubId();
            this.smaPublisher=_po.getSmaPublisher();
        }
        this.smaPublishTime=_po.getSmaPublishTime();
        this.smaImg=_po.getSmaImg();
        this.subjectWords=_po.getSubjectWords();
        this.keyWords=_po.getKeyWords();
        //组织分类字典——没有办法现在直接处理，先只把值记录下来
        DictDetail dd=new DictDetail();
        dd.setId(_po.getLangDid());
        dd.setNodeName(_po.getLanguage());
        this.lang=dd;
        this.descn=_po.getDescn();
        this.pubCount=_po.getPubCount();
        this.smaStatus=_po.getSmaStatus();
        this.CTime=_po.getCTime();
    }

    @Override
    public boolean equals(Object o) {
        if (this==o) return true;
        if (o==null||!(o instanceof SeqMediaAsset)) return false;

        SeqMediaAsset _o=(SeqMediaAsset)o;
        if (_o.getId().equals(id)) return true;
        return false;
    }
}