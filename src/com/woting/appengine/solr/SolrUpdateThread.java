package com.woting.appengine.solr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.woting.cm.core.media.persis.po.MediaAssetPo;
import com.woting.cm.core.media.persis.po.MediaPlayCountPo;
import com.woting.cm.core.media.persis.po.SeqMediaAssetPo;
import com.woting.cm.core.media.service.MediaService;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.appengine.solr.service.SolrJService;

public class SolrUpdateThread extends Thread {

	private String smaId;
	private SolrJService solrJService;
	private MediaService mediaService;

	public SolrUpdateThread(String smaId) {
		this.smaId = smaId;
	}

	@Override
	public void run() {
		updateSolr();
	}
	
	public void updateSolr() {
		try {
			ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
	        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
	            this.mediaService = (MediaService) WebApplicationContextUtils.getWebApplicationContext(sc).getBean("mediaService");
	            this.solrJService = (SolrJService) WebApplicationContextUtils.getWebApplicationContext(sc).getBean("solrJService");
	        }
	        SeqMediaAssetPo sma = mediaService.getSmaPoInfoById(smaId);
	        if (sma != null) {
				Map<String, Object> m = new HashMap<String, Object>();
				m.put("maPublisher", sma.getSmaPublisher());
				m.put("whereSql", " id in (select mId from wt_SeqMA_Ref where sId = '" + sma.getId() + "')");
				List<MediaAssetPo> mas = mediaService.getMaListBy(m);
				if ((mas != null) && (mas.size() > 0)) {
					sma.setSmaAllCount(mas.size());
					long playcount = 0L;
					m.clear();
					m.put("resId", sma.getId());
					m.put("resTableName", "wt_SeqMediaAsset");
					MediaPlayCountPo mp = mediaService.getMediaPlayCount(m);
					
					if (mp != null) solrJService.addSolrIndex(sma, null, null, null, Long.valueOf(mp.getPlayCount()));
					else solrJService.addSolrIndex(sma, null, null, null, playcount);
					for (MediaAssetPo ma : mas) {
						try {
							Thread.sleep(200);
							playcount = 0L;
							m.clear();
							m.put("resId", ma.getId());
							m.put("resTableName", "wt_MediaAsset");
							mp = mediaService.getMediaPlayCount(m);
							if (mp != null) solrJService.addSolrIndex(ma, sma.getId(), null, null, Long.valueOf(mp.getPlayCount()));
							else solrJService.addSolrIndex(ma, sma.getId(), null, null, playcount);
						} catch (Exception localException1) {
							continue;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
