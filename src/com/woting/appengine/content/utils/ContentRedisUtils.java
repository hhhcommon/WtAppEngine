package com.woting.appengine.content.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.ext.spring.redis.RedisOperService;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.StringUtils;

public abstract class ContentRedisUtils {

	/**
	 * 
	 * @param mediaType
	 * @param ContentId
	 * @param MethodNum
	 *            1:getContentInfo 2:discuss/add 3:clickFavorite
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> isOrNoToLocal(Map<String, Object> m, int methodNum) {
		Map<String, Object> retM = new HashMap<>();
		ServletContext sc = (SystemCache.getCache(FConstants.SERVLET_CONTEXT) == null ? null : (ServletContext) SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
		if (WebApplicationContextUtils.getWebApplicationContext(sc) != null) {
			JedisConnectionFactory conn = (JedisConnectionFactory) WebApplicationContextUtils.getWebApplicationContext(sc).getBean("connectionFactory");
			RedisOperService ros = new RedisOperService(conn);
			String mediaType = m.get("MediaType")+"";
			String ContentId = m.get("ContentId")+"";
			if (methodNum == 1) {
				if (mediaType.equals("AUDIO")) {
					if (ros.exist("CONTENT_AUDIO_SAVED_" + ContentId)) {
						String saved = ros.get("CONTENT_AUDIO_SAVED_" + ContentId);
						if (saved.equals("false") || saved.equals("error")) {
							retM.put("IsOrNoLocal", 0); // 未入库
							retM.put("Info", (Map<String, Object>) JsonUtils.jsonToObj(ros.get("CONTENT_AUDIO_INFO_" + ContentId), Map.class));
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
							retM.put("Info", (Map<String, Object>) JsonUtils.jsonToObj(ros.get("CONTENT_SEQU_INFO_" + ContentId), Map.class));
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
			} else if (methodNum == 2) {
				if (mediaType.equals("AUDIO")) {
					if (ros.exist("CONTENT_AUDIO_SAVED_" + ContentId)) {
						String saved = ros.get("CONTENT_AUDIO_SAVED_" + ContentId);
						if (saved.equals("false") || saved.equals("error")) {
							if (ros.exist("CONTENT_AUDIO_DISCUSS_" + ContentId)) {
								String discuss = ros.get("CONTENT_AUDIO_DISCUSS_" + ContentId);
								if (!StringUtils.isNullOrEmptyOrSpace(discuss)) {
									List<Map<String, Object>> ls = (List<Map<String, Object>>) JsonUtils.jsonToObj(discuss, List.class);
									m.put("CTime", System.currentTimeMillis());
									ls.add(m);
									ros.set("CONTENT_AUDIO_DISCUSS_" + ContentId, JsonUtils.objToJson(ls));
								}
							} else {
								List<Map<String, Object>> ls = new ArrayList<>();
								m.put("CTime", System.currentTimeMillis());
								ls.add(m);
								ros.set("CONTENT_AUDIO_DISCUSS_"+ContentId, JsonUtils.objToJson(ls));
							}
							retM.put("IsOrNoLocal", 0); // 未入库
							retM.put("Info", null);
							return retM;
						} else {
							retM.put("IsOrNoLocal", 1); // 已入库
							retM.put("Info", saved);
							return retM;
						}
					} else {
						retM.put("IsOrNoLocal", 2); // redis不存在
						retM.put("Info", null);
						return retM;
					}
				} else if (mediaType.equals("SEQU")) {
					if(ros.exist("CONTENT_SEQU_SAVED_" + ContentId)) {
						String saved = ros.get("CONTENT_SEQU_SAVED_" + ContentId);
						if (saved.equals("false") || saved.equals("error")) {
							if (ros.exist("CONTENT_SEQU_DISCUSS_" + ContentId)) {
								String discuss = ros.get("CONTENT_SEQU_DISCUSS_" + ContentId);
								if (!StringUtils.isNullOrEmptyOrSpace(discuss)) {
									List<Map<String, Object>> ls = (List<Map<String, Object>>) JsonUtils.jsonToObj(discuss, List.class);
									m.put("CTime", System.currentTimeMillis());
									ls.add(m);
									ros.set("CONTENT_SEQU_DISCUSS_" + ContentId, JsonUtils.objToJson(ls));
								}
							} else {
								List<Map<String, Object>> ls = new ArrayList<>();
								m.put("CTime", System.currentTimeMillis());
								ls.add(m);
								ros.set("CONTENT_SEQU_DISCUSS_"+ContentId, JsonUtils.objToJson(ls));
							}
							retM.put("IsOrNoLocal", 0); // 未入库
							retM.put("Info", null);
							return retM;
						} else {
							retM.put("IsOrNoLocal", 1); // 已入库
							retM.put("Info", saved);
							return retM;
						}
					} else {
						retM.put("IsOrNoLocal", 2); // redis不存在
						retM.put("Info", null);
						return retM;
					}
				}
			} else if (methodNum == 3) {
				if (mediaType.equals("AUDIO")) {
					if (ros.exist("CONTENT_AUDIO_SAVED_" + ContentId)) {
						String saved = ros.get("CONTENT_AUDIO_SAVED_" + ContentId);
						if (saved.equals("false") || saved.equals("error")) {
							if (ros.exist("CONTENT_AUDIO_FAVORITE_"+ContentId)) {
								String favorites = ros.get("CONTENT_AUDIO_FAVORITE_"+ContentId);
								if (!StringUtils.isNullOrEmptyOrSpace(favorites)) {
									List<Map<String, Object>> ls = (List<Map<String, Object>>) JsonUtils.jsonToObj(favorites, List.class);
									m.put("CTime", System.currentTimeMillis());
									ls.add(m);
									ros.set("CONTENT_AUDIO_FAVORITE_"+ContentId, JsonUtils.objToJson(ls));
								}
							} else {
								List<Map<String, Object>> ls = new ArrayList<>();
								m.put("CTime", System.currentTimeMillis());
								ls.add(m);
								ros.set("CONTENT_AUDIO_FAVORITE_"+ContentId, JsonUtils.objToJson(ls));
							}
							retM.put("IsOrNoLocal", 0); // 未入库
							retM.put("Info", true);
							return retM;
						} else {
							retM.put("IsOrNoLocal", 1); // 已入库
							retM.put("Info", saved);
							return retM;
						}
					} else {
						retM.put("IsOrNoLocal", 2); // redis不存在
						retM.put("Info", null);
						return retM;
					}
				} else if (mediaType.equals("SEQU")) {
					if(ros.exist("CONTENT_SEQU_SAVED_" + ContentId)) {
						String saved = ros.get("CONTENT_SEQU_SAVED_" + ContentId);
						if (saved.equals("false") || saved.equals("error")) {
							if (ros.exist("CONTENT_SEQU_FAVORITE_"+ContentId)) {
								String favorites = ros.get("CONTENT_SEQU_FAVORITE_"+ContentId);
								if (StringUtils.isNullOrEmptyOrSpace(favorites)) {
									List<Map<String, Object>> ls = (List<Map<String, Object>>) JsonUtils.jsonToObj(favorites, List.class);
									m.put("CTime", System.currentTimeMillis());
									ls.add(m);
									ros.set("CONTENT_SEQU_FAVORITE_"+ContentId, JsonUtils.objToJson(ls));
								}
							} else {
								List<Map<String, Object>> ls = new ArrayList<>();
								m.put("CTime", System.currentTimeMillis());
								ls.add(m);
								ros.set("CONTENT_SEQU_FAVORITE_"+ContentId, JsonUtils.objToJson(ls));
							}
							retM.put("IsOrNoLocal", 0); // 未入库
							retM.put("Info", true);
							return retM;
						} else {
							retM.put("IsOrNoLocal", 1); // 已入库
							retM.put("Info", saved);
							return retM;
						}
					} else {
						retM.put("IsOrNoLocal", 2); // redis不存在
						retM.put("Info", null);
						return retM;
					}
				}
			}
		}
		return null;

	}
}
