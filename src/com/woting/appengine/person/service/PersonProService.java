package com.woting.appengine.person.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import com.woting.cm.core.channel.persis.po.ChannelAssetPo;
import com.woting.cm.core.channel.service.ChannelService;
import com.woting.cm.core.media.model.MediaAsset;
import com.woting.cm.core.media.service.MediaService;
import com.woting.cm.core.person.persis.po.PersonPo;
import com.woting.cm.core.person.persis.po.PersonRefPo;
import com.woting.cm.core.person.service.PersonBaseService;

public class PersonProService {
	@Resource
	private PersonBaseService personBaseService;
	@Resource
	private ChannelService channelService;
	@Resource
	private MediaService mediaService;
	
	public Map<String, Object> getPersonInfo(String personId, String seqMediaAssetSize, String mediaAssetSize) {
		PersonPo po = personBaseService.getPersonPo(personId);
		if (po != null) {
			Map<String, Object> retM = new HashMap<>();
			retM.put("PersonName", po.getpName());
			retM.put("PersonImg", po.getPortrait());
			retM.put("PersonDescn", po.getDescn());
			retM.put("PersonId", po.getId());
			List<PersonRefPo> prefs = personBaseService.getPersonRefs(personId, "wt_SeqMediaAsset");
			if (prefs!=null) {
				String smaIds = "";
				for (PersonRefPo personRefPo : prefs) {
					smaIds += ",'"+personRefPo.getResId()+"'";
				}
				smaIds = smaIds.substring(1);
				Map<String, Object> m = new HashMap<>();
				m.put("assetType", "wt_SeqMediaAsset");
				m.put("flowFlag", "2");
				m.put("isValidate", "1");
				m.put("whereByClause", " assetId in ("+smaIds+")");
				m.put("sortByClause", " pubTime Desc limit "+seqMediaAssetSize);
				List<ChannelAssetPo> chas = channelService.getChannelAssetListBy(m);
				if (chas!=null) {
					List<Map<String, Object>> smas = new ArrayList<>();
					for (ChannelAssetPo cha : chas) {
						Map<String, Object> ret = new HashMap<>();
						ret.put("ContentId", cha.getAssetId());
						ret.put("MediaType", "SEQU");
						ret.put("ContentImg", cha.getPubImg());
						ret.put("ContentName", cha.getPubName());
						ret.put("PlayCount", "1234");
						m.clear();
						m.put("assetType", "wt_MediaAsset");
						m.put("whereByClause", "assetId in (select mId from wt_SeqMA_Ref where sId = '"+cha.getAssetId()+"')");
						m.put("sortByClause", "pubTime Desc limit 1");
						List<ChannelAssetPo> cs = channelService.getChannelAssetListBy(m);
						if (cs!=null && cs.size()>0) {
							ret.put("NewMedia", cs.get(0).getPubName());
							smas.add(ret);
						}
					}
					retM.put("SeqMediaList", smas);
				}
			}
			prefs = personBaseService.getPersonRefs(personId, "wt_MediaAsset");
			if (prefs!=null) {
				String maIds = "";
				for (PersonRefPo personRefPo : prefs) {
					maIds += ",'"+personRefPo.getResId()+"'";
				}
				maIds = maIds.substring(1);
				Map<String, Object> m = new HashMap<>();
				m.put("assetType", "wt_MediaAsset");
				m.put("flowFlag", "2");
				m.put("isValidate", "1");
				m.put("whereByClause", " assetId in ("+maIds+")");
				m.put("sortByClause", " pubTime Desc limit "+mediaAssetSize);
				List<ChannelAssetPo> chas = channelService.getChannelAssetListBy(m);
				if (chas!=null) {
					List<Map<String, Object>> mas = new ArrayList<>();
					for (ChannelAssetPo cha : chas) {
						Map<String, Object> ret = new HashMap<>();
						ret.put("ContentId", cha.getAssetId());
						ret.put("MediaType", "AUDIO");
						ret.put("ContentImg", cha.getPubImg());
						ret.put("ContentName", cha.getPubName());
						MediaAsset ma = mediaService.getMaInfoById(cha.getAssetId());
						if (ma.getTimeLong()!=0) {
							ret.put("ContentTime", ma.getTimeLong());
						} else {
							ret.put("ContentTime", "123400");
						}
						ret.put("ContentPubTime", cha.getPubTime());
						ret.put("PlayCount", "1234");
						mas.add(ret);
					}
					retM.put("MediaAssetList", mas);
				}
			}
			return retM;
		}
		return null;
	}

	public List<Map<String, Object>> getPersonContents(String personId, String mediaType, String page, String pageSize,
			String orderBy) {
		PersonPo po = personBaseService.getPersonPo(personId);
		if (po != null) {
			if (mediaType.equals("SEQU")) {
				List<PersonRefPo> prefs = personBaseService.getPersonRefs(personId, "wt_SeqMediaAsset");
				if (prefs!=null && prefs.size()>0) {
					String smaIds = "";
					for (PersonRefPo personRefPo : prefs) {
						smaIds += ",'"+personRefPo.getResId()+"'";
					}
					smaIds = smaIds.substring(1);
					Map<String, Object> m = new HashMap<>();
					m.put("assetType", "wt_SeqMediaAsset");
					m.put("flowFlag", "2");
					m.put("isValidate", "1");
					m.put("whereByClause", " assetId in ("+smaIds+") GROUP BY assetId");
					if (orderBy.equals("1")) {
						m.put("sortByClause", " pubTime Desc limit "+(Integer.valueOf(page)-1)*(Integer.valueOf(pageSize))+","+pageSize);
					} else {
						if (orderBy.equals("2")) {
							m.put("sortByClause", " pubTime limit "+(Integer.valueOf(page)-1)*(Integer.valueOf(pageSize))+","+pageSize);
						}
					}
					
					List<ChannelAssetPo> chas = channelService.getChannelAssetListBy(m);
					if (chas!=null) {
						List<Map<String, Object>> smas = new ArrayList<>();
						for (ChannelAssetPo cha : chas) {
							Map<String, Object> ret = new HashMap<>();
							ret.put("ContentId", cha.getAssetId());
							ret.put("MediaType", "SEQU");
							ret.put("ContentImg", cha.getPubImg());
							ret.put("ContentName", cha.getPubName());
							ret.put("PlayCount", "1234");
							m.clear();
							m.put("assetType", "wt_MediaAsset");
							m.put("whereByClause", "assetId in (select mId from wt_SeqMA_Ref where sId = '"+cha.getAssetId()+"')");
							m.put("sortByClause", "pubTime Desc limit 1");
							List<ChannelAssetPo> cs = channelService.getChannelAssetListBy(m);
							if (cs!=null && cs.size()>0) {
								ret.put("NewMedia", cs.get(0).getPubName());
								smas.add(ret);
							}
						}
						return smas;
					}
				}
			} else {
				if (mediaType.equals("AUDIO")) {
					List<PersonRefPo> prefs = personBaseService.getPersonRefs(personId, "wt_MediaAsset");
					if (prefs!=null && prefs.size()>0) {
						String maIds = "";
						for (PersonRefPo personRefPo : prefs) {
							maIds += ",'"+personRefPo.getResId()+"'";
						}
						maIds = maIds.substring(1);
						Map<String, Object> m = new HashMap<>();
						m.put("assetType", "wt_MediaAsset");
						m.put("flowFlag", "2");
						m.put("isValidate", "1");
						m.put("whereByClause", " assetId in ("+maIds+") GROUP BY assetId");
						if (orderBy.equals("1")) {
							m.put("sortByClause", " pubTime Desc limit "+(Integer.valueOf(page)-1)*(Integer.valueOf(pageSize))+","+pageSize);
						} else {
							if (orderBy.equals("2")) {
								m.put("sortByClause", " pubTime limit "+(Integer.valueOf(page)-1)*(Integer.valueOf(pageSize))+","+pageSize);
						    }
					    }
						List<ChannelAssetPo> chas = channelService.getChannelAssetListBy(m);
						if (chas!=null) {
							List<Map<String, Object>> mas = new ArrayList<>();
							for (ChannelAssetPo cha : chas) {
								Map<String, Object> ret = new HashMap<>();
								ret.put("ContentId", cha.getAssetId());
								ret.put("MediaType", "AUDIO");
								ret.put("ContentImg", cha.getPubImg());
								ret.put("ContentName", cha.getPubName());
								MediaAsset ma = mediaService.getMaInfoById(cha.getAssetId());
								if (ma.getTimeLong()!=0) {
									ret.put("ContentTime", ma.getTimeLong());
								} else {
									ret.put("ContentTime", "123400");
								}
								ret.put("ContentPubTime", cha.getPubTime());
								ret.put("PlayCount", "1234");
								mas.add(ret);
							}
							return mas;
						}
				    }
		        }
			}
		}
		return null;
	}
	
	
}
