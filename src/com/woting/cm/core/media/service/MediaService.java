package com.woting.cm.core.media.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.channel.model.Channel;
import com.woting.cm.core.channel.model.ChannelAsset;
import com.woting.cm.core.channel.persis.po.ChannelAssetPo;
import com.woting.cm.core.channel.persis.po.ChannelPo;
import com.woting.cm.core.dict.model.DictRefRes;
import com.woting.cm.core.dict.persis.po.DictRefResPo;
import com.woting.cm.core.media.model.MaSource;
import com.woting.cm.core.media.model.MediaAsset;
import com.woting.cm.core.media.model.SeqMediaAsset;
import com.woting.cm.core.media.persis.po.MaSourcePo;
import com.woting.cm.core.media.persis.po.MediaAssetPo;
import com.woting.cm.core.media.persis.po.SeqMaRefPo;
import com.woting.cm.core.media.persis.po.SeqMediaAssetPo;
import com.woting.cm.core.utils.ContentUtils;
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
    @Resource(name="defaultDAO")
    private MybatisDAO<ChannelAssetPo> channelAssetDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<ChannelPo> channelDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<DictRefResPo> dictRdfDao;

    @PostConstruct
    public void initParam() {
        mediaAssetDao.setNamespace("A_MEDIA");
        maSourceDao.setNamespace("A_MEDIA");
        seqMaRefDao.setNamespace("A_MEDIA");
        seqMediaAssetDao.setNamespace("A_MEDIA");
        channelAssetDao.setNamespace("A_CHANNELASSET");
        channelDao.setNamespace("A_CHANNEL");
        dictRdfDao.setNamespace("A_DREFRES");
    }
    
    public MaSource getMasInfoByMasId(Map<String, Object> m) {
    	MaSource mas = new MaSource();
    	MaSourcePo maspo = maSourceDao.getInfoObject("getMasInfoByMaId", m);
    	mas.buildFromPo(maspo);
		return mas;
    }
    
    public SeqMaRefPo getSeqMaRefByMId(String mid){
    	SeqMaRefPo smarefpo = seqMaRefDao.getInfoObject("getS2MRefInfoByMId", mid);
		return smarefpo;
    }
    
    //根据主播id查询其所有单体资源
    public List<Map<String, Object>> getMaInfoByMaPubId(String id) {
        List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
        List<MediaAssetPo> listpo = new ArrayList<MediaAssetPo>();
        listpo = mediaAssetDao.queryForList("getMaListByMaPubId", id);
        for (MediaAssetPo mediaAssetPo : listpo) {
        	MediaAsset ma=new MediaAsset();
			ma.buildFromPo(mediaAssetPo);
			list.add(ContentUtils.convert2Ma(ma.toHashMap(), null, null, null, null));
		}
        return list;
    }
    
    //根据主播id查询其所有专辑
    public List<Map<String, Object>> getSmaInfoBySmaPubId(String id){
    	List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
    	List<SeqMediaAssetPo> listpo = new ArrayList<SeqMediaAssetPo>();
    	listpo = seqMediaAssetDao.queryForList("getSmaListBySmaPubId", id);
    	
    	String resids = "";
    	for (SeqMediaAssetPo seqMediaAssetPo : listpo) {
			resids+=",'"+seqMediaAssetPo.getId()+"'";
		}
    	resids = resids.substring(1);
    	
    	List<Map<String, Object>> catalist = getResDictRefByResId(resids, "wt_SeqMediaAsset");
    	
    	for (SeqMediaAssetPo seqMediaAssetPo : listpo) {
			SeqMediaAsset sma = new SeqMediaAsset();
			sma.buildFromPo(seqMediaAssetPo);
			list.add(ContentUtils.convert2Sma(sma.toHashMap(), catalist, null, null, null));
		}
		return list;
    }
    
    //根据专辑id得到专辑
    public SeqMediaAsset getSmaInfoById(String id) {
    	SeqMediaAsset sma = new SeqMediaAsset();
    	SeqMediaAssetPo smapo = seqMediaAssetDao.getInfoObject("getSmaInfoById", id);
    	sma.buildFromPo(smapo);
    	return sma;
	}
    
    //根据栏目id得到栏目
    public Channel getChInfoById(String id){
    	Channel ch = new Channel();
    	ChannelPo chpo = channelDao.getInfoObject("getInfoById", id);
    	ch.buildFromPo(chpo);
    	return ch;
    }
    
    //根据栏目发布表id得到栏目发布信息
    public ChannelAsset getCHAInfoById(String id){
    	ChannelAsset cha = new ChannelAsset();
    	ChannelAssetPo chapo = channelAssetDao.getInfoObject("getInfoById",id);
    	cha.buildFromPo(chapo);
		return cha;
    }
    
    //根据资源id得到资源字典项对应关系
    public List<Map<String, Object>> getResDictRefByResId(String resids, String resTableName){
    	Map<String, String> param=new HashMap<String, String>();
        param.put("resTableName", resTableName);
        param.put("resIds", resids);
        List<DictRefResPo> rcrpL = dictRdfDao.queryForList("getListByResIds", param);
        List<Map<String, Object>> catalist = new ArrayList<Map<String,Object>>();
        for (DictRefResPo dictRefResPo : rcrpL) {
			catalist.add(dictRefResPo.toHashMap());
		}
		return catalist;
    }
    
    
    public void saveCHA(ChannelAsset cha){
    	channelAssetDao.insert("insert", cha.convert2Po());
    }
    public void updateCHA(ChannelAsset cha){
    	channelAssetDao.update("update", cha.convert2Po());
    }

    public MediaAsset getMaInfoById(String id) {
        MediaAsset ma=new MediaAsset();
        ma.buildFromPo(mediaAssetDao.getInfoObject("getMaInfoById", id));
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

    public void updateSeqMaRef(SeqMaRefPo seqmapo){
    	mediaAssetDao.update("updateSeqMaRef", seqmapo);
    }
    public void updateMas(MaSource mas){
    	mediaAssetDao.update("updateMas", mas.convert2Po());
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
    
    public void saveDictRef(DictRefRes dictref) {
    	dictRdfDao.insert("insert", dictref.convert2Po());
    }
    
    public void removeMa(String id){
    	mediaAssetDao.delete("removeMaById", id);
    }
    
    public void removeSma(String id){
    	seqMediaAssetDao.delete("", id);
    }
    
    public void removeMas(String id){
    	maSourceDao.delete("multiMasByMaId", id);
    }
    
    public void removeMa2Sma(String id){
    	seqMaRefDao.delete("multiM2SRefByMId", id);
    }
    
    public void removeResDictRef(String id){
    	dictRdfDao.delete("multiDelBc", id);
    }
    
    public void removeCha(String id){
    	channelAssetDao.delete("deleteByAssetId", id);
    }
}