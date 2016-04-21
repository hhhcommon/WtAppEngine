package com.woting.appengine.searchcrawler.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.stereotype.Service;
import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;
import com.woting.appengine.searchcrawler.utils.DataTransform;

@Service
public class ThreadService {

	/**
	 * 使用多线程在三平台查询内容
	 * 
	 * @param constr三平台查询的内容
	 * @return
	 */
	public Map<String, Object> threadService(String constr, int resultType, int pageType) {
		Map<String, Object> map = new HashMap<String, Object>();
		ExecutorService pool = Executors.newFixedThreadPool(3); // 线程池大小4
		QingTingService qt = new QingTingService(constr); // 创建蜻蜓搜索线程
//		KaoLaService kl = new KaoLaService(constr); // 创建考拉搜索线程
		XiMaLaYaService xm = new XiMaLaYaService(constr); // 创建喜马拉雅搜索线程
		LocalSearch ls = new LocalSearch(constr, resultType, pageType);
	//	Future<Map<String, Object>> kl_s = pool.submit(kl); // 开启考拉线程
		Future<Map<String, Object>> qt_s = pool.submit(qt); // 开启蜻蜓线程
		Future<Map<String, Object>> xm_s = pool.submit(xm); // 开启喜马拉雅线程
		Future<Map<String, Object>> ls_s = pool.submit(ls); // 开启本地搜索
		try {
	//		List<Festival> list_kl_festival = (List<Festival>) kl_s.get().get("KL_F"); // 取出考拉搜索的节目信息
	//		List<Station> list_kl_station = (List<Station>) kl_s.get().get("KL_S"); // 取出考拉搜索的频道信息
			List<Festival> list_qt_festival = new ArrayList<Festival>();
			List<Station> list_qt_station = new ArrayList<Station>();
			List<Festival> list_xmly_festival = new ArrayList<Festival>();
			List<Station> list_xmly_station = new ArrayList<Station>();
			if(qt_s.get()==null){
				list_qt_festival = null;
				list_qt_station = null;
			}else{
				list_qt_festival = (List<Festival>) qt_s.get().get("QT_F"); // 取出蜻蜓搜索的节目信息
				list_qt_station = (List<Station>) qt_s.get().get("QT_S"); // 取出蜻蜓搜索的频道信息
			}
			if(xm_s.get()==null){
				list_xmly_festival = null;
				list_xmly_station = null;
			}else{
				list_xmly_festival = (List<Festival>) xm_s.get().get("XMLY_F"); // 取出喜马拉雅搜索的节目信息
				list_xmly_station = (List<Station>) xm_s.get().get("XMLY_S"); // 取出喜马拉雅搜索的频道信息
			}
			
			List<Map<String, Object>> list_local_festival = new ArrayList<Map<String, Object>>(); // 取出本地搜索的信息
			if (ls_s.get() == null) {
				list_local_festival = null;
			} else {
				list_local_festival = (List<Map<String, Object>>) ls_s.get().get("List");
			}
			List<Station> liststation = new ArrayList<Station>(); // 合并搜索到的频道信息
			List<Festival> listfestival = new ArrayList<Festival>(); // 合并搜索到的节目信息
		/*	if (list_kl_station != null)
				liststation.addAll(list_kl_station);
			if (list_kl_festival != null)
				listfestival.addAll(list_kl_festival);*/
			if (list_qt_station != null)
				liststation.addAll(list_qt_station);
			if (list_qt_festival != null)
				listfestival.addAll(list_qt_festival);
			if (list_xmly_station != null)
				liststation.addAll(list_xmly_station);
			if (list_xmly_festival != null)
				listfestival.addAll(list_xmly_festival);
			List<Map<String, Object>> listall = DataTransform.datas2Audio(listfestival, liststation, 0); // 数据信息转换
			if (list_local_festival != null) {
				for (Map<String, Object> map2 : list_local_festival) {
					listall.add(map2);
				}
			}
			map.put("AllCount", listall.size());
			map.put("List", listall);
			map.put("ResultType", resultType);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			e1.printStackTrace();
		}
		pool.shutdown(); // 线程关闭
		return map;
	}

	/**
	 * 考拉，蜻蜓和喜马拉雅三平台搜索
	 * 
	 * @param searchStr
	 * @param resultType
	 * @param pageType
	 * @return
	 */
	public Map<String, Object> searchWebAndLocal(String searchStr, int resultType, int pageType) {
		Map<String, Object> maps = new HashMap<String, Object>();
		String __s[] = searchStr.split(",");
		String _s[] = new String[__s.length];
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> listall = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < __s.length; i++) {
			_s[i] = __s[i].trim();
			Map<String, Object> map = threadService(_s[i], resultType, pageType);
			list.add(map);
		}
		for (Map<String, Object> map : list) {
			List<Map<String, Object>> list2 = (List<Map<String, Object>>) map.get("List");
			if (list2 != null) {
				for (Map<String, Object> map2 : list2) {
					listall.add(map2);
				}
			}
		}
		maps.put("AllCount", listall.size());
		maps.put("List", listall);
		maps.put("ResultType", resultType);
		return maps;
	}
}
