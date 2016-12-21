package com.woting.appengine.searchcrawler.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.spiritdata.framework.ext.spring.redis.RedisOperService;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.searchcrawler.service.CrawlerSearch;
import com.woting.appengine.searchcrawler.service.LocalSearch;
import com.woting.passport.mobile.MobileUDKey;

public abstract class SearchUtils {

	private static int T = 5000; // 默认超时时间
	private static String[] specialfield = { "扫一扫", "新闻", "关注", "微信", "好友", "分享", "朋友圈", "iphone", "客户端", "[详情]",
			"京ICP证", "京网文", "|", "┊", "｜", "上一篇：", "我的收藏", "首页", "不良信息举报：", "经营许可证编号:", "2016.", "：", "订阅", "保留所有权利",
			"@", "联系我们", "反垃圾邮件策略", "书面授权", "正北方网", "浏览本网主页", "显示屏的分辨率", "免责声明", "稿件侵权行为", "版权", "转载", "18183", "和讯网" };

	/**
	 * 搜索内容中文转url编码
	 * 
	 * @param s 搜索的中文内容
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

	/**
	 * json解析工具
	 * 
	 * @param jsonstr 字符串形式的json数据
	 * @param strings 解析json数据各层的属性名，可多个，也可为空
	 * @return 返回已解析好的json数据
	 */
	@SuppressWarnings("unchecked")
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
	 * 
	 * @param key
	 * @return
	 */
	public static long getListNum(String key, RedisOperService ros) {
		if (ros.exist("Search_" + key + "_Data")) {
			long num = ros.lLen("Search_" + key + "_Data");
			return num;
		} else {
			return 0;
		}
	}

	/**
	 * 得到list分页
	 * 
	 * @param key
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public static List<Map<String, Object>> getListPage(String key, int page, int pageSize, RedisOperService ros) {
		List<Map<String, Object>> list = null;
		String finishstr = "Search_" + key + "_Finish";
		String datastr = "Search_" + key + "_Data";
        if (ros.exist(finishstr)) { // 判断是否redis里有存储的数据
            long num = ros.lLen(datastr); // 得到已存储数据的个数
            num = num - (page - 1) * pageSize;
            if (num <= 0) {
                if (isOrNoSearchFinish(key, ros)) {
                    return null;
                } else {
                    long time = System.currentTimeMillis();
                    while ((System.currentTimeMillis()-time)<5000) {
                        num = ros.lLen(datastr)-(page-1)*pageSize;
                        if (num>=pageSize) {
                            list = convertJsonList(ros.lRange(datastr,(page-1)*pageSize,page*pageSize-1));
                            return list;
                        } else {
                            if(isOrNoSearchFinish(key, ros)) {
                                list = convertJsonList(ros.lRange(datastr,(page-1)*pageSize,(page-1)*pageSize+num-1));
                                return list;
                            } else try {Thread.sleep(50);} catch(Exception e) {}
                        }
                    }
                    //num<=0时未完成等待5s超时处理
                    if (num>=pageSize) {
                        list = convertJsonList(ros.lRange(datastr,(page-1)*pageSize,page*pageSize-1));
                        return list;
                    } else {
                        if ((num>0)&&(num<pageSize)) {
                            list = convertJsonList(ros.lRange(datastr,(page-1)*pageSize,(page-1)*pageSize+num-1));
                            return list;
                        } else {
                            return null;
                        }
                    }
                }
            } else if (num>=pageSize) {
                list = convertJsonList(ros.lRange(datastr,(page-1)*pageSize,page*pageSize-1));
                return list;
            } else if (num<pageSize) {
                if (isOrNoSearchFinish(key, ros)) {
                    list = convertJsonList(ros.lRange(datastr,(page-1)*pageSize,(page-1)*pageSize+num-1));
                    return list;
                } else {
                    long time = System.currentTimeMillis();
                    while ((System.currentTimeMillis()-time)<5000) {
                        num = ros.lLen(datastr)-(page-1)*pageSize;
                        if (num>=pageSize) {
                            list = convertJsonList(ros.lRange(datastr,(page-1)*pageSize,page*pageSize-1));
                            return list;
                        } else {
                            if(isOrNoSearchFinish(key, ros)) {
                                list = convertJsonList(ros.lRange(datastr,(page-1)*pageSize,(page-1)*pageSize+num-1));
                                return list;
                            } else try {Thread.sleep(50);} catch(Exception e) {}
                        }
                    }
                    // 0<num<pagesize时未完成等待5s超时处理
                    num = ros.lLen(datastr)-(page-1)*pageSize;
                    if(num>=pageSize){
                        list = convertJsonList(ros.lRange(datastr,(page-1)*pageSize,page*pageSize-1));
                        return list;
                    } else {
                        list = convertJsonList(ros.lRange(datastr,(page-1)*pageSize,(page-1)*pageSize+num-1));
                        return list;
                    }
                }
            }
        }
        return list;
	}
	
	public static void createNewsInfo(String contentid, String contenturi, RedisOperService ros){
        ros.set("Search_"+contentid+"_NewsInfo", contenturi, "", -1);
	}
	
	/**
	 * 得到新闻内容
	 * @param contentid
	 * @return
	 */
	public static Map<String, Object> getNewsInfo(String contentid, RedisOperService ros){
		Map<String, Object> map = new HashMap<String,Object>();
        if (ros.exist("Search_"+contentid+"_NewsInfo")) {
            if(ros.get("Search_"+contentid+"_NewsInfo")!=null) {
                map.put("ContentURI", ros.get("Search_"+contentid+"_NewsInfo"));
                map.put("ContentId", contentid);
                return map;
            }
        }
		return null;
	}

	/**
	 * 把List里的字符串转化为Map对象
	 * 
	 * @param l
	 * @return
	 */
	public static List<Map<String, Object>> convertJsonList(List<String> l) {
		List<Map<String, Object>> retM = new ArrayList<Map<String,Object>>();
		if (l != null && l.size() > 0) {
			for (String josnS : l) {
				Map<String, Object> m = (Map<String, Object>) JsonUtils.jsonToObj(josnS, Map.class);
				if (m!=null) retM.add(m);
			}
			return retM;
		}
		return null;
	}

	/**
	 * 添加list里节目数据
	 * 
	 * @param key
	 * @param T
	 * @return
	 */
	public static <T> boolean addListInfo(String key, T T, RedisOperService ros) {
		String value = "";
        String classname = T.getClass().getSimpleName();
        if (classname.equals("HashMap"))
            value = JsonUtils.objToJson(T);
        if (!StringUtils.isNullOrEmptyOrSpace(value)&&!value.toLowerCase().equals("null")) {
            ros.rPush("Search_" + key + "_Data", value);
        }
		return true;
	}

	/**
	 * 在三平台搜索
	 * 
	 * @param searchStr
	 * @return
	 */
	public static boolean searchContent(String searchStr, MobileUDKey mUdk, RedisOperService ros) {
		createSearchTime(searchStr, ros);
		createBeginSearch(searchStr, ros);;
		new CrawlerSearch(searchStr, mUdk).start();
		new LocalSearch(searchStr, mUdk).start();
		return true;
	}

	/**
	 * 放入缓存搜索时间
	 * 
	 * @param key
	 */
	private static void createSearchTime(String key, RedisOperService ros) {
		long time = System.currentTimeMillis();
		ros.set("Search_" + key + "_Date", Long.toString(time));
	}

	/**
	 * 是否搜索完成
	 * 
	 * @param key
	 * @return
	 */
	public static boolean isOrNoSearchFinish(String key, RedisOperService ros) {
        if (ros.exist("Search_" + key + "_Finish")) {
            if (ros.get("Search_" + key + "_Finish").equals("3")) { // 喜马拉雅，蜻蜓，服务器数据库
                System.out.println("key:已搜索完成 ");
                return true;
            }
        }
		return false;
	}
	
	/**
	 * 放入缓存开始搜索标志
	 * 
	 * @param key
	 */
	public static void createBeginSearch(String key, RedisOperService ros) {
	    ros.set("Search_" + key + "_Finish", "0");
	}

	/**
	 * 更新缓存里搜索完成进度
	 * 
	 * @param key
	 */
	public static void updateSearchFinish(String key, RedisOperService ros) {
		if (ros.exist("Search_" + key + "_Finish")) {
			System.out.println("加载完成进度");
			String finishnum = ros.get("Search_" + key + "_Finish");
			finishnum = String.valueOf(Integer.valueOf(finishnum) + 1);
			ros.set("Search_" + key + "_Finish", finishnum);
		}
	}

	/**
	 * 清除搜索到页面文本里标签
	 * 
	 * @param str
	 * @return
	 */
	public static String cleanTag(String str) {
		if (StringUtils.isNullOrEmptyOrSpace(str)) return "";
		while (true) {
			int tagbegin = str.indexOf("<");
			int tagend = str.indexOf(">", tagbegin);
			if (tagbegin != -1 && tagend != -1) {
				String constr = str.substring(tagbegin, tagend + 1);
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
	 * 
	 * @param constr
	 * @return
	 */
	public static boolean isOrNORemove(String constr) {
		if (StringUtils.isNullOrEmptyOrSpace(constr)) return true;
		if (constr.contains("...") || constr.contains("&gt;&gt;") || constr.contains("[详细]"))
			return true;
		if(constr.length()<30){
			if (!constr.contains("，") && !constr.contains("。") && !constr.contains(",") && !constr.contains("、")) 
				return true;
		}
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
			if (num / count >= 3 || (num < 40 && num / count >= 2))
				return true;
		}
		if (num < 100) {
			count = 0;
			for (String specialstr : specialfield) {
				if (constr.contains(specialstr))
					count++;
				if (count == 2)
					return true;
			}
		}
		return false;
	}

	/**
	 * 得到发布信息
	 * 
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
				if (Character.isDigit(c))
					time += String.valueOf(c);
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
			long ltime = Long.valueOf(time) * 3600 * 1000;
			ltime = System.currentTimeMillis() - ltime;
			pubinfo[1] = sdf.format(new Date(ltime));
		} else {
			if (pubs.length == 3)
				pubinfo[1] = pubs[1] + pubs[2];
		}
		return pubinfo;
	}
	
	public static String[] readFile(String path) {
		String[] str = new String[20];
		InputStreamReader in = null;
		BufferedReader br = null;
		File file = new File(path.trim());
		try {
			in = new InputStreamReader(new FileInputStream(file));
			br = new BufferedReader(in);
			String zjstr = "";
			int i=0;
			try {
				while ((zjstr = br.readLine()) != null) {
					str[i]=zjstr;
					i++;
					if(i==20) break;
				}
				in.close();
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return str;
	}
}
