package com.woting.ext.spring;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.cm.core.map2json.persis.po.Map2JsonPo;

public class MappingJackson2HttpMessageConverterFactory extends MappingJackson2HttpMessageConverter {

	@Override
	protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(object);
        //加密
        Map2JsonPo mJsonPo = null;
        ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
        	mJsonPo = (Map2JsonPo) WebApplicationContextUtils.getWebApplicationContext(sc).getBean("map2json");
        }
        
        String result = "";
        if (mJsonPo!=null) {
			Map<String, Object> map2jsonmap = mJsonPo.getReplaceMap();
			if (map2jsonmap!=null && map2jsonmap.size()>0) {
				Set<String> sets = map2jsonmap.keySet();
				for (String key : sets) {
					json = json.replace(key, map2jsonmap.get(key).toString());
				}
			}
		}
        result = json;
        //输出
        outputMessage.getBody().write(result.getBytes("UTF-8"));
	}
}
