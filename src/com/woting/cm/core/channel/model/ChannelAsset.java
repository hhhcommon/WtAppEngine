package com.woting.cm.core.channel.model;

import java.io.Serializable;
import java.sql.Timestamp;

import com.spiritdata.framework.core.model.ModelSwapPo;
import com.spiritdata.framework.exceptionC.Plat0006CException;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.channel.persis.po.ChannelAssetPo;
import com.woting.cm.core.media.model.MediaAsset;
import com.woting.cm.core.media.model.SeqMediaAsset;

public class ChannelAsset implements Serializable, ModelSwapPo {
    private static final long serialVersionUID=2605319995410196247L;

    private String id; //表ID(UUID)
    private Channel ch; //发布的栏目
    private Object pubObj;
    private String publisherId; //发布者Id
    private String checkerId; //审核者Id，可以为空，若为1，则审核者为系统
    private int isValidate; //是否生效(1-生效,2-无效)
    private int sort; //栏目排序,从大到小排序，越大越靠前，既是置顶功能
    private int flowFlag; //流程状态：0入库；1在审核；2审核通过(既发布状态)；3审核未通过
    private String pubName; //发布名称，可为空，若为空，则取资源的名称
    private String pubImg; //发布图片，可为空，若为空，则取资源的Img
    private String inRuleIds; //进入该栏目的规则，0为手工/人工创建，其他未系统规则Id
    private String checkRuleIds; //审核规则，0为手工/人工创建，其他为系统规则id
    private Timestamp CTime; //创建时间
    private Timestamp pubTime; //发布时间，发布时的时间，若多次发布，则是最新的发布时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id=id;
    }
    public Channel getCh() {
        return ch;
    }
    public void setCh(Channel ch) {
        this.ch=ch;
    }
    public Object getPubObj() {
        return pubObj;
    }
    public void setPubObj(Object pubObj) {
        this.pubObj=pubObj;
    }
    public String getPublisherId() {
        return publisherId;
    }
    public void setPublisherId(String publisherId) {
        this.publisherId=publisherId;
    }
    public String getCheckerId() {
        return checkerId;
    }
    public void setCheckerId(String checkerId) {
        this.checkerId=checkerId;
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
    public int getFlowFlag() {
        return flowFlag;
    }
    public void setFlowFlag(int flowFlag) {
        this.flowFlag=flowFlag;
    }
    public String getPubName() {
        return pubName;
    }
    public void setPubName(String pubName) {
        this.pubName=pubName;
    }
    public String getPubImg() {
        return pubImg;
    }
    public void setPubImg(String pubImg) {
        this.pubImg=pubImg;
    }
    public String getInRuleIds() {
        return inRuleIds;
    }
    public void setInRuleIds(String inRuleIds) {
        this.inRuleIds=inRuleIds;
    }
    public String getCheckRuleIds() {
        return checkRuleIds;
    }
    public void setCheckRuleIds(String checkRuleIds) {
        this.checkRuleIds=checkRuleIds;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime=cTime;
    }
    public Timestamp getPubTime() {
        return pubTime;
    }
    public void setPubTime(Timestamp pubTime) {
        this.pubTime=pubTime;
    }

    @Override
    public ChannelAssetPo convert2Po() {
        ChannelAssetPo ret=new ChannelAssetPo();
        if (StringUtils.isNullOrEmptyOrSpace(this.getId())) ret.setId(SequenceUUID.getPureUUID());
        else ret.setId(this.getId());

        if (ch!=null&&!StringUtils.isNullOrEmptyOrSpace(ch.getId())) ret.setChannelId(ch.getId());
        ret.setPublisherId(publisherId);
        ret.setCheckerId(checkerId);
        ret.setInRuleIds(inRuleIds);
        ret.setCheckRuleIds(checkRuleIds);
        ret.setSort(sort);
        ret.setIsValidate(isValidate);
        ret.setPubName(pubName);
        ret.setPubImg(pubImg);
        ret.setFlowFlag(flowFlag);
        ret.setCTime(CTime);
        ret.setPubTime(pubTime);
        if (pubObj!=null) {
            if (pubObj instanceof MediaAsset) {
                ret.setAssetId(((MediaAsset)pubObj).getId());
                ret.setAssetType("wt_MediaAsset");
                if (StringUtils.isNullOrEmptyOrSpace(pubName)) ret.setPubName(((MediaAsset)pubObj).getMaTitle());
                if (StringUtils.isNullOrEmptyOrSpace(pubImg)) ret.setPubImg(((MediaAsset)pubObj).getMaImg());
            } else  if (pubObj instanceof SeqMediaAsset) {
                ret.setAssetId(((SeqMediaAsset)pubObj).getId());
                ret.setAssetType("wt_SeqMediaAsset");
                if (StringUtils.isNullOrEmptyOrSpace(pubName)) ret.setPubName(((SeqMediaAsset)pubObj).getSmaTitle());
                if (StringUtils.isNullOrEmptyOrSpace(pubImg)) ret.setPubImg(((SeqMediaAsset)pubObj).getSmaImg());
            }
        }
        return ret;
    }

    @Override
    public void buildFromPo(Object po) {
        if (po==null) throw new Plat0006CException("Po对象为空，无法从空对象得到概念/逻辑对象！");
        if (!(po instanceof ChannelAssetPo)) throw new Plat0006CException("Po对象不是ChannelAssetPo的实例，无法从此对象构建栏目资产发布对象！");
        ChannelAssetPo _po = (ChannelAssetPo)po;

        id=_po.getId();
        publisherId=_po.getPublisherId();
        checkerId=_po.getCheckerId();
        inRuleIds=_po.getInRuleIds();
        checkRuleIds=_po.getCheckRuleIds();
        sort=_po.getSort();
        isValidate=_po.getIsValidate();
        pubName=_po.getPubName();
        pubImg=_po.getPubImg();
        flowFlag=_po.getFlowFlag();
        CTime=_po.getCTime();
        pubTime=_po.getPubTime();

        //所对应的栏目和发布对象不能在这里获得，这里只是进行记录
        Channel c=new Channel();
        c.setId(_po.getChannelId());
        ch=c;
        if (_po.getAssetType().equals("wt_MediaAsset")) {
            MediaAsset ma=new MediaAsset();
            ma.setId(_po.getAssetId());
            pubObj=ma;
        } else if (_po.getAssetType().equals("wt_SeqMediaAsset")) {
            SeqMediaAsset sma=new SeqMediaAsset();
            sma.setId(_po.getAssetId());
            pubObj=sma;
        }
    }
}