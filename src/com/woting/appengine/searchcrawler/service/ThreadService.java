package com.woting.appengine.searchcrawler.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadService {

	public List<Map<String, Object>> threadService(String constr){
		List<Map<String, Object>> lists = new ArrayList<Map<String,Object>>();
		ExecutorService pool = Executors.newFixedThreadPool(3);
		QingTingService qt = new QingTingService(constr);
		KaoLaService kl = new KaoLaService(constr);
		XiMaLaYaService xm = new XiMaLaYaService(constr);
		Future<Map<String, Object>> qt_s = pool.submit(kl);
		Future<Map<String, Object>> kl_s =  pool.submit(qt);
		Future<Map<String, Object>> xm_s = pool.submit(xm);
		try {
			lists.add(kl_s.get());
			lists.add(qt_s.get());
			lists.add(xm_s.get());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pool.shutdown();
		return lists;
		
	}
}
