package com.woting.appengine.searchcrawler.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadService {

	public Map<String, Object> threadService(String constr){
		Map<String, Object> maps = new HashMap<String,Object>();
		ExecutorService pool = Executors.newFixedThreadPool(3);
		QingTingService qt = new QingTingService(constr);
		KaoLaService kl = new KaoLaService(constr);
		XiMaLaYaService xm = new XiMaLaYaService(constr);
		Future<Map<String, Object>> kl_s = pool.submit(kl);
		Future<Map<String, Object>> qt_s =  pool.submit(qt);
		Future<Map<String, Object>> xm_s = pool.submit(xm);
		try {
			maps.put("KL", kl_s.get());
			maps.put("QT", qt_s.get());
			maps.put("XMLY", xm_s.get());
			System.out.println(maps);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		pool.shutdown();
		System.out.println("线程结束");
		return maps;
		
	}
}
