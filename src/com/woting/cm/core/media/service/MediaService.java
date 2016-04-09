package com.woting.cm.core.media.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.media.model.MaSource;
import com.woting.cm.core.media.model.MediaAsset;
import com.woting.cm.core.media.model.SeqMediaAsset;
import com.woting.cm.core.media.persis.po.MaSourcePo;
import com.woting.cm.core.media.persis.po.MediaAssetPo;
import com.woting.cm.core.media.persis.po.SeqMaRefPo;
import com.woting.cm.core.media.persis.po.SeqMediaAssetPo;
import com.woting.exceptionC.Wtcm0101CException;

public class MediaService {
    @Resource(name="defaultDAO")
    private MybatisDAO<MediaAssetPo> mediaAssetDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<MaSourcePo> maSourceDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<SeqMediaAssetPo> seqMediaAssetDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<SeqMaRefPo> seqMaRefDao;

    @PostConstruct
    public void initParam() {
        mediaAssetDao.setNamespace("A_MEDIA");
        maSourceDao.setNamespace("A_MEDIA");
        seqMaRefDao.setNamespace("A_MEDIA");
        seqMediaAssetDao.setNamespace("A_MEDIA");
    }

    public MediaAsset getMaInfoById(String id) {
        MediaAsset ma=new MediaAsset();
        ma.buildFromPo(mediaAssetDao.getInfoObject("getInfoById", id));
        return ma;
    }

    public void saveMa(MediaAsset ma) {
        mediaAssetDao.insert("insertMa", ma.convert2Po());
    }

    public void saveMas(MaSource mas) {
        maSourceDao.insert("insertMas", mas.convert2Po());
    }

    public MaSource getSameMas(MaSource mas) {
        MaSourcePo masPo=maSourceDao.getInfoObject("getSameSam", mas);
        if (masPo==null) return null;
        MaSource _mas=new MaSource();
        _mas.buildFromPo(masPo);
        return _mas;
    }

    public void bindMa2Sma(MediaAsset ma, SeqMediaAsset sma) {
        SeqMaRefPo smrPo=new SeqMaRefPo();
        if (StringUtils.isNullOrEmptyOrSpace(ma.getId())||StringUtils.isNullOrEmptyOrSpace(sma.getId())) {
            throw new Wtcm0101CException("专辑和单曲的Id都不能为空");
        }
        smrPo.setId(SequenceUUID.getPureUUID());
        smrPo.setSId(sma.getId());
        smrPo.setMId(ma.getId());
        smrPo.setColumnNum(0);
        smrPo.setDescn(sma.getSmaTitle()+"--"+ma.getMaTitle());
        seqMaRefDao.insert("bindMa2Sma", smrPo);
    }

    public void updateMa(MediaAsset ma) {
        mediaAssetDao.update("updateMa", ma.convert2Po());
    }

    public void saveSma(SeqMediaAsset sma) {
        seqMediaAssetDao.insert("insertSma", sma.convert2Po());
    }

    public void updateSma(SeqMediaAsset sma) {
        seqMediaAssetDao.update("updateSma", sma.convert2Po());
    }
}