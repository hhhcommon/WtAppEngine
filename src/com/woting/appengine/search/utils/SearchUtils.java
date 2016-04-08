package com.woting.appengine.search.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.spiritdata.framework.util.JsonUtils;

public class SearchUtils {
	
	private int T = 3000;	 //默认超时时间为3秒
	
	public SearchUtils(int T) {
		this.T = T;
	}
	
	public SearchUtils() {
	}
	
	/**
	 *搜索内容中文转url编码
	 * @param s 搜索的中文内容
	 * @return 返回转成的url编码
	 */
	public String utf8TOurl(String s) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= 0 && c <= 255) {
				sb.append(c);
			} else {
				byte[] b;
				try {
					b = String.valueOf(c).getBytes("utf-8");
				} catch (Exception ex) {
					System.out.println(ex);
					b = new byte[0];
				}
				for (int j = 0; j < b.length; j++) {
					int k = b[j];
					if (k < 0)
						k += 256;
					sb.append("%" + Integer.toHexString(k).toUpperCase());
				}
			}
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	/**
	 * json解析工具
	 * @param jsonstr 字符串形式的json数据
	 * @param strings	解析json数据各层的属性名，可多个，也可为空
	 * @return 返回已解析好的json数据
	 */
	public List<Map<String, Object>> jsonTOlist(String jsonstr,String... strings){
		if(strings.length==0 || jsonstr.isEmpty() ||jsonstr.equals("")){
			return null;
		}else{
			Map<String, Object> testmap =  (Map<String, Object>) JsonUtils.jsonToObj(jsonstr, Map.class);
			for(int i=0;i<strings.length-1;i++){
				testmap = (Map<String, Object>) testmap.get(strings[i]);
			}
			List<Map<String, Object>> list_href=(List<Map<String, Object>>)testmap.get(strings[strings.length-1]);
			return list_href;
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> jsonTOmap(String jsonstr,String... strings){
		if(strings.length==0 || jsonstr.isEmpty() ||jsonstr.equals("")){
			return null;
		}else{
			Map<String, Object> testmap =  (Map<String, Object>) JsonUtils.jsonToObj(jsonstr, Map.class);
			for(int i=0;i<strings.length;i++){
				testmap = (Map<String, Object>) testmap.get(strings[i]);
			}
			
			return testmap;
		}
	}
	
	/**
	 * jsoup解析网页信息
	 * @param url 链接地址
	 * @return 得到的json格式数据或者html格式文本
	 */
	public String jsoupTOstr(String url){
		Document doc = null;
		String str = null;
		try {
			doc = Jsoup.connect(url).ignoreContentType(true).timeout(T).get();
			//获取频道json数据
			str = doc.select("body").html().toString();
			str=str.replaceAll("\n", "");
			str=str.replaceAll("\r", "");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return str;
	}
	
	/**
	 * 检查需要再加的属性值是否为空
	 * @param obj map返回的属性对应的值
	 * @return obj为空时返回null，不为空时返回字符串
	 */
	public String ObjIsEmpty(Object obj){
		if(obj == null)
		{
			return "null";
		}else{
			return obj.toString();
		}
	}

	/**
	 * 检查对象是否为空
	 * @param obj map返回的属性对应的值
	 * @return obj为空时返回null，不为空时返回字符串
	 */
	public boolean isNull(Object obj){
		return obj==null;
	}
}
