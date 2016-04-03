package com.woting.cm.core.media.model;

import java.io.Serializable;
import java.sql.Timestamp;

import com.spiritdata.framework.core.model.ModelSwapPo;
import com.spiritdata.framework.exceptionC.Plat0006CException;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.media.persis.po.MaSourcePo;
import com.woting.cm.core.perimeter.model.Organize;

public class MaSource implements Serializable, ModelSwapPo {
    private static final long serialVersionUID=-8410951324168523124L;

    private String id; //uuid(主键)
    private int maSrcType; //来源，类型：1-组织表；2-文本
    private String maSrcId; //当maSrcType=1,来源Id，对应表wt_Organize内容；当maSrcType=2,来源名称
    private String maSource; //来源描述或名称
    private int smType; //来源媒体分类:1-文件;2-直播流
    private String playURI; //直播流URL
    private int isMain; //是否主播放地址；1是主播放
    private String descn; //说明
    private Timestamp CTime; //记录创建时间

    private Organize src; //来源组织，当maSrcType=1是，这个不能为空
    private MediaAsset ma; //本声音源所对应的单体节目

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id=id;
    }
    public int getMaSrcType() {
        return maSrcType;
    }
    public void setMaSrcType(int maSrcType) {
        this.maSrcType=maSrcType;
    }
    public String getMaSrcId() {
        return maSrcId;
    }
    public void setMaSrcId(String maSrcId) {
        this.maSrcId=maSrcId;
    }
    public String getMaSource() {
        return maSource;
    }
    public void setMaSource(String maSource) {
        this.maSource=maSource;
    }
    public int getSmType() {
        return smType;
    }
    public void setSmType(int smType) {
        this.smType=smType;
    }
    public String getPlayURI() {
        return playURI;
    }
    public void setPlayURI(String playURI) {
        this.playURI=playURI;
    }
    public int getIsMain() {
        return isMain;
    }
    public void setIsMain(int isMain) {
        this.isMain=isMain;
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
    public MediaAsset getMa() {
        return ma;
    }
    public void setMa(MediaAsset ma) {
        this.ma=ma;
    }
    public Organize getSrc() {
        return src;
    }
    public void setSrc(Organize src) {
        if (src==null) return;
        this.src=src;
        maSrcType=1;
        maSrcId=src.getId();
        maSource=src.getOrgName();
    }

    public boolean isMain() {
        return isMain==1;
    }

    @Override
    public MaSourcePo convert2Po() {
        MaSourcePo ret=new MaSourcePo();
        //id处理，没有id，自动生成一个
        if (StringUtils.isNullOrEmptyOrSpace(this.id)) ret.setId(SequenceUUID.getUUIDSubSegment(4));
        else ret.setId(this.id);

        if (ma!=null) ret.setMaId(ma.getId());
        if (src!=null) {
            ret.setMaSrcType(1);
            ret.setMaSrcId(src.getId());
            ret.setMaSource(src.getOrgName());
        } else {
            ret.setMaSrcType(2);
            ret.setMaSrcId(maSrcId);
            ret.setMaSource(maSource);
        }
        ret.setSmType(smType);
        ret.setPlayURI(playURI);
        ret.setIsMain(isMain);
        ret.setDescn(descn);
        ret.setCTime(this.CTime);

        return ret;
    }

    @Override
    public void buildFromPo(Object po) {
        if (po==null) throw new Plat0006CException("Po对象为空，无法从空对象得到概念/逻辑对象！");
        if (!(po instanceof MaSourcePo)) throw new Plat0006CException("Po对象不是MaSourcePo的实例，无法从此对象构建声音来源对象！");
        MaSourcePo _po=(MaSourcePo)po;

        this.id=_po.getId();
        this.smType=_po.getSmType();
        this.playURI=_po.getPlayURI();
        this.isMain=_po.getIsMain();
        this.descn=_po.getDescn();
        this.CTime=_po.getCTime();

        //对应单体节目——没有办法现在直接处理，先只把值记录下来
        this.ma=new MediaAsset();
        this.ma.setId(_po.getMaId());

        //对应组织——没有办法现在直接处理，先只把值记录下来
        this.maSrcType=_po.getMaSrcType();
        if (this.maSrcType==1) {
            this.src=new Organize();
            this.src.setId(_po.getMaSrcId());
            this.src.setOrgName(_po.getMaSource());
        } else {
            this.maSrcId=_po.getMaSrcId();
            this.maSource=_po.getMaSource();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this==o) return true;
        if (o==null||!(o instanceof MaSource)) return false;

        MaSource _o=(MaSource)o;
        if (_o.getId().equals(id)) return true;
        if (_o.getMa().equals(ma)&&_o.getPlayURI().equals(playURI)&&_o.getMaSrcType()==maSrcType&&_o.getMaSrcId().equals(maSrcId)) return true;
        return false;
    }
}