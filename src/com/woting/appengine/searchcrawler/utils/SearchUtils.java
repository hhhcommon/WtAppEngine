package com.woting.appengine.searchcrawler.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.StringUtils;
import com.woting.WtAppEngineConstants;
import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;
import com.woting.appengine.searchcrawler.service.BaiDuNewsService;
import com.woting.appengine.searchcrawler.service.KaoLaService;
import com.woting.appengine.searchcrawler.service.QingTingService;
import com.woting.appengine.searchcrawler.service.XiMaLaYaService;

import redis.clients.jedis.Jedis;

public abstract class SearchUtils {

	private static int T = 5000; // 默认超时时间
	private static String[] specialfield = { "扫一扫", "新闻", "关注", "微信", "好友", "分享", "朋友圈", "iphone", "客户端", "[详情]",
			"京ICP证", "京网文", "|", "┊", "｜", "上一篇：", "我的收藏", "首页", "不良信息举报：", "经营许可证编号:", "2016.", "：", "订阅", "保留所有权利",
			"@", "联系我们", "反垃圾邮件策略" };

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
	 * 
	 * @param key
	 * @return
	 */
	public static long getListNum(String key) {
		Jedis jedis = new Jedis(WtAppEngineConstants.IPPATH);
		if (jedis.exists("Search_"+key+"_Data")) {
			return jedis.llen("Search_"+key+"_Data");
		} else
			return 0;
	}

	/**
	 * 得到list分页
	 * 
	 * @param key
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public static List<String> getListPage(String key, int page, int pageSize) {
		Jedis jedis = new Jedis(WtAppEngineConstants.IPPATH);
		List<String> list = null;
		if (jedis.exists("Search_"+key+"_Data")) {
			long num = jedis.llen("Search_"+key+"_Data");
			num = num-(page-1)*pageSize;
			if(num<=0){
				if(isOrNoSearchFinish(key)) return null;
				else {
					try {
						long time = System.currentTimeMillis(),endtime;
						while((endtime=System.currentTimeMillis()-time)<5000){
							num = jedis.llen("Search_"+key+"_Data")-(page-1)*pageSize;
							if(isOrNoSearchFinish(key)){
								if (num>0) {list=jedis.lrange("Search_"+key+"_Data", (page-1)*pageSize, page*pageSize-1);jedis.close();return list;}
								else return null;
							}else{
								if(num>pageSize) {list=jedis.lrange("Search_"+key+"_Data", (page-1)*pageSize, page*pageSize-1);jedis.close();return list;}
							    else if(num<=0) Thread.sleep(100);
						        else if(num>0 && num<pageSize){
						        	if((num%pageSize)>pageSize/2) {list=jedis.lrange("Search_"+key+"_Data", (page-1)*pageSize, (page-1)*pageSize+num-1);jedis.close();return list;}
						        	if((num%pageSize)<pageSize/2) {
						        		if (isOrNoSearchFinish(key)) {list=jedis.lrange("Search_"+key+"_Data", (page-1)*pageSize, (page-1)*pageSize+num-1);jedis.close();return list;}
						        	}
						        }
							}
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} 
			else if (num>=pageSize) {list=jedis.lrange("Search_"+key+"_Data", (page-1)*pageSize, page*pageSize-1);jedis.close();return list;}
			else if (0<num&&num<pageSize) {list=jedis.lrange("Search_"+key+"_Data", (page-1)*pageSize, (page-1)*pageSize+num-1);jedis.close();return list;}
		}
		jedis.close();
		jedis.quit();
		return null;
	}

	/**
	 * 添加list里节目数据
	 * 
	 * @param key
	 * @param T
	 * @return
	 */
	public static <T> boolean addListInfo(String key, T T) {
//		Jedis jedis = new Jedis(WtAppEngineConstants.IPPATH);
		Jedis jedis=new Jedis("127.0.0.33", 6379);
		String value = "";
		String classname = T.getClass().getSimpleName();
		if (classname.equals("Festival"))
			value = JsonUtils.objToJson(DataTransform.festival2Audio((Festival) T));
		else if (classname.equals("Station"))
			value = JsonUtils.objToJson(DataTransform.datas2Sequ_Audio((Station) T));
		else if (classname.equals("HashMap"))
			value = JsonUtils.objToJson(T);
		value = value.replace("\"", "'");
		if (!StringUtils.isNullOrEmptyOrSpace(value))
			jedis.rpush("Search_" + key + "_Data", value);
		jedis.close();
		return true;
	}

	/**
	 * 在三平台搜索
	 * 
	 * @param searchStr
	 * @return
	 */
	public static boolean searchContent(String searchStr) {
		createSearchTime(searchStr);
		createBeginSearch(searchStr);
//		KaoLaService.begin(searchStr);
//		XiMaLaYaService.begin(searchStr);
//		QingTingService.begin(searchStr);
		BaiDuNewsService.begin(searchStr);
		return true;
	}

	/**
	 * 放入缓存搜索时间
	 * @param key
	 */
	private static void createSearchTime(String key) {
		Jedis jedis = new Jedis(WtAppEngineConstants.IPPATH);
		long time = System.currentTimeMillis();
		jedis.set("Search_"+key+"_Date", Long.toString(time));
		jedis.close();
	}
	
	/**
	 * 是否搜索完成
	 * @param key
	 * @return
	 */
	public static boolean isOrNoSearchFinish(String key){
		Jedis jedis = new Jedis(WtAppEngineConstants.IPPATH);
		if(jedis.exists("Search_"+key+"_Finish")){
			if(jedis.get("Search_"+key+"_Finish").equals("1")){
				System.out.println("key:已搜索完成 ");
				jedis.close();
				return true;
			}
		}
		jedis.close();
		return false;
	}

	/**
	 * 放入缓存开始搜索标志
	 * @param key
	 */
	public static void createBeginSearch(String key) {
		Jedis jedis = new Jedis(WtAppEngineConstants.IPPATH);
		jedis.set("Search_"+key+"_Finish", "0");
		jedis.close();
	}
	
	/**
	 * 更新缓存里
	 * @param key
	 */
	public static void updateSearchFinish(String key){
		Jedis jedis = new Jedis(WtAppEngineConstants.IPPATH);
		if(jedis.exists("Search_"+key+"_Finish")){
			String finishnum = jedis.get("Search_"+key+"_Finish");
			finishnum = String.valueOf(Integer.valueOf(finishnum)+1);
			jedis.set("Search_"+key+"_Finish", finishnum);
		}
		jedis.close();
	}

	/**
	 * 清除搜索到页面文本里标签
	 * @param str
	 * @return
	 */
	public static String cleanTag(String str) {
		while (true) {
			int tagbegin = str.indexOf("<");
			int tagend = str.indexOf(">", tagbegin);
			if (tagbegin!=-1 && tagend!=-1) {
				String constr = str.substring(tagbegin, tagend+1);
				str = str.replace(constr, "");
			} else {
				if (str.indexOf("条相同新闻") != -1)
					str = str.replace(str.substring(str.indexOf("条相同新闻") - 2, str.length()), "");
				return str.replace("\n", "").replace("　", "").replace("&nbsp;", " ").replace("百度快照", "").trim();
			}
		}
	}

	/**
	 * 是否清除需要加载的一句文本
	 * @param constr
	 * @return
	 */
	public static boolean isOrNORemove(String constr) {
		if (constr.contains("...") || constr.contains("&gt;&gt;"))
			return true;
		char[] cs = constr.toCharArray();
		int num = cs.length;
		int count = 0;
		for (char c : cs) { // 判断文本里是否包含汉字
			Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
			if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
					|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
					|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
					|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
					|| ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
					|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
					|| ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
				count++;
			}
		}
		if (count == 0) {
			return true;
		} else {
			if (num/count>=3 || (num<40&&num/count>=1))
				return true;
		}
		if (num < 100) {
			count = 0;
			for (String specialstr : specialfield) {
				if (constr.contains(specialstr)) {
					count++;
				}
				if (count == 2)
					return true;
			}
		}
		return false;
	}

	/**
	 * 得到发布信息
	 * @param contentpubstr
	 * @return 发布组织和发布时间
	 */
	public static String[] getPubInfo(String contentpubstr) {
		String[] pubinfo = new String[2];
		String[] pubs = contentpubstr.split(" +");
		String time = "";
		pubinfo[0] = pubs[0];
		if (pubs.length == 2) {
			char[] ctimes = pubs[1].toCharArray();
			for (char c : ctimes) {
				if (Character.isDigit(c)) {
					time += String.valueOf(c);
				}
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
			long ltime = Long.valueOf(time)*3600*1000;
			ltime = System.currentTimeMillis()-ltime;
			pubinfo[1] = sdf.format(new Date(ltime));
		} else {
			if (pubs.length == 3) {
				pubinfo[1] = pubs[1]+pubs[2];
			}
		}
		return pubinfo;
	}
}
