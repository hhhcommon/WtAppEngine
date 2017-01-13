package com.woting.appengine.content.utils;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.ext.spring.redis.RedisOperService;
import com.spiritdata.framework.util.JsonUtils;

public abstract class ContentRedisUtils {

	@SuppressWarnings("unchecked")
	public static Map<String, Object> isOrNoToLocal(String mediaType, String ContentId) {
		Map<String, Object> retM = new HashMap<>();
		ServletContext sc = (SystemCache.getCache(FConstants.SERVLET_CONTEXT) == null ? null
				: (ServletContext) SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
		if (WebApplicationContextUtils.getWebApplicationContext(sc) != null) {
			JedisConnectionFactory conn = (JedisConnectionFactory) WebApplicationContextUtils
					.getWebApplicationContext(sc).getBean("connectionFactory");
			RedisOperService ros = new RedisOperService(conn);
			if (mediaType.equals("AUDIO")) {
				if (ros.exist("CONTENT_AUDIO_SAVED_" + ContentId)) {
					String saved = ros.get("CONTENT_AUDIO_SAVED_" + ContentId);
					if (saved.equals("false") || saved.equals("error")) {
						retM.put("IsOrNoLocal", 0); // 未入库
						retM.put("Info", (Map<String, Object>) JsonUtils
								.jsonToObj(ros.get("CONTENT_AUDIO_INFO_" + ContentId), Map.class));
						return retM;

					} else {
						retM.put("IsOrNoLocal", 1); // 已入库
						retM.put("Info", saved);
						return retM;
					}
				} else {
					retM.put("IsOrNoLocal", 2); // redis不存在
					retM.put("Info", null);
				}
			} else if (mediaType.equals("SEQU")) {
				if (ros.exist("CONTENT_SEQU_SAVED_" + ContentId)) {
					String saved = ros.get("CONTENT_SEQU_SAVED_" + ContentId);
					if (saved.equals("false") || saved.equals("error")) {
						retM.put("IsOrNoLocal", 0); // 未入库
						retM.put("Info", (Map<String, Object>) JsonUtils
								.jsonToObj(ros.get("CONTENT_SEQU_INFO_" + ContentId), Map.class));
						return retM;
					} else {
						retM.put("IsOrNoLocal", 1); // 已入库
						retM.put("Info", saved);
						return retM;
					}
				} else {
					retM.put("IsOrNoLocal", 2); // redis不存在
					retM.put("Info", null);
				}
			}
		}
		retM.put("IsOrNoLocal", 3);
		retM.put("Info", null);
		return retM;
	}
}
