package com.woting.appengine.searchcrawler.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.StringUtils;
import com.woting.WtAppEngineConstants;
import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;
import com.woting.appengine.searchcrawler.service.KaoLaService;
import com.woting.appengine.searchcrawler.service.QingTingService;
import com.woting.appengine.searchcrawler.service.XiMaLaYaService;

import redis.clients.jedis.Jedis;

public abstract class SearchUtils {

	private static int T = 1000; // 默认超时时间
	private static Jedis jedis = new Jedis(WtAppEngineConstants.IPPATH);

	/**
	 * 搜索内容中文转url编码
	 * 
	 * @param s
	 *            搜索的中文内容
	 * @return 返回转成的url编码
	 */
	public static String utf8TOurl(String s) {
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
	 * 
	 * @param jsonstr
	 *            字符串形式的json数据
	 * @param strings
	 *            解析json数据各层的属性名，可多个，也可为空
	 * @return 返回已解析好的json数据
	 */
	public static List<Map<String, Object>> jsonTOlist(String jsonstr, String... strings) {
		if (strings.length == 0 || StringUtils.isNullOrEmptyOrSpace(jsonstr)) {
			return null;
		} else {
			Map<String, Object> testmap = (Map<String, Object>) JsonUtils.jsonToObj(jsonstr, Map.class);
			for (int i = 0; i < strings.length - 1; i++) {
				testmap = (Map<String, Object>) testmap.get(strings[i]);
			}
			List<Map<String, Object>> list_href = (List<Map<String, Object>>) testmap.get(strings[strings.length - 1]);
			return list_href;
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> jsonTOmap(String jsonstr, String... strings) {
		if (StringUtils.isNullOrEmptyOrSpace(jsonstr)) {
			return null;
		} else {
			Map<String, Object> testmap = (Map<String, Object>) JsonUtils.jsonToObj(jsonstr, Map.class);
			for (int i = 0; i < strings.length; i++) {
				testmap = (Map<String, Object>) testmap.get(strings[i]);
			}
			return testmap;
		}
	}

	/**
	 * jsoup解析网页信息
	 * 
	 * @param url
	 *            链接地址
	 * @return 得到的json格式数据或者html格式文本
	 */
	public static String jsoupTOstr(String url) {
		Document doc = null;
		String str = null;
		try {
			doc = Jsoup.connect(url).ignoreContentType(true).timeout(T).get();
			// 获取频道json数据
			str = doc.select("body").html().toString();
			str = str.replaceAll("\"", "'");
			str = str.replaceAll("\n", "");
			str = str.replaceAll("&quot;", "\"");
			str = str.replaceAll("\r", "");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return str;
	}

	public static int findint(String str) {
		char[] s = str.toCharArray();
		String d = "";
		for (int i = 0; i < s.length; i++) {
			if (Character.isDigit(s[i])) {
				d += s[i];
			}
		}
		return Integer.valueOf(d);
	}

	/**
	 * 得到list里节目数
	 * @param key
	 * @return
	 */
	public static long getListNum(String key) {
		if (jedis.exists(key)) {
			return jedis.llen(key);
		} else
			return 0;
	}

	/**
	 * 得到list分页
	 * @param key
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public static List<String> getListPage(String key, int page, int pageSize) {
		if (jedis.exists(key)) {
			long num = jedis.llen(key);
			num = num - (page - 1) * pageSize;
			if (num > 0) {
				if (num >= pageSize)
					return jedis.lrange(key, (page - 1) * pageSize, page * pageSize - 1);
				else if (0 < num && num < pageSize)
					return jedis.lrange(key, (page - 1) * pageSize, num - 1);
				else if (num < 0)
					return null;
			} else {
				return null;
			}
		}
		return null;
	}

	/**
	 * 添加list里节目数据
	 * @param key
	 * @param T
	 * @return
	 */
	public static <T> boolean addListInfo(String key, T T) {
		String value = "";
		String classname = T.getClass().getSimpleName();
		if (classname.equals("Festival"))
			value = JsonUtils.objToJson(DataTransform.festival2Audio((Festival) T));
		else {
			if (classname.equals("Station"))
				value = JsonUtils.objToJson(DataTransform.datas2Sequ_Audio((Station) T));
		}
		value = value.replace("\"", "'");
		if(!existSame(key, value))jedis.rpush(key, value);
		return true;
	}

	/**
	 * 在三平台搜索
	 * @param searchStr
	 * @return
	 */
	public static boolean searchContent(String searchStr) {
		lockRedisKey(searchStr);
		KaoLaService.begin(searchStr);
		XiMaLaYaService.begin(searchStr);
		QingTingService.begin(searchStr);
		return true;
	}

	/**
	 * 判断锁是否存在和等待解锁
	 * @param key
	 * @return
	 */
	public static boolean judgeLock(String key) {
		if (jedis.exists("LOCK:" + key)) {
			long a = System.currentTimeMillis(), b;
			while (jedis.exists("LOCK:" + key))
				if ((b = System.currentTimeMillis() - a) > 1500) { // 需加超时判断1.5s
					unlockRedisKey(key);
					return true;
				}
		}
		return true;
	}

	/**
	 * 查找list里是否存在相似
	 * @param key
	 * @param value
	 * @return
	 */
	public static boolean existSame(String key, String value) {
		List<String> list = jedis.lrange(key, 0, jedis.llen(key) - 1);
		if (list != null) {
			for (int i=0;i<list.size()-1;i++) {
				if(value.equals(list.get(i)))
					return true;
			}
			return false;
		}
		return false;
	}

	/**
	 * 解锁
	 * @param key
	 * @return
	 */
	public static boolean unlockRedisKey(String key) {
		if (jedis.exists(key))
			jedis.del(key);
		return false;
	}

	/**
	 * 给正在查询的内容加锁
	 * @param key
	 * @return
	 */
	private static boolean lockRedisKey(String key) {
		jedis.set("LOCK:" + key, "1");
		return false;
	}

}
