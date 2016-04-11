package com.woting.appengine.searchcrawler.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;
import com.woting.appengine.searchcrawler.service.kl_s_Service;
import com.woting.appengine.searchcrawler.utils.DataTransform;

public class kl_test {

	kl_s_Service kl_s = new kl_s_Service();
	DataTransform dataT = new DataTransform();
	
	@Test
	public void test_1(){
		 //wbq 考拉搜索        
        Map<String, Object> map_kl = new HashMap<String,Object>();
        Map<String, Object> map_kl_s =  kl_s.kaolaService("成十六");
	    List<Festival> list_kl_festival = (List<Festival>) map_kl_s.get("KL_F");
	    List<Station> list_kl_station = (List<Station>) map_kl_s.get("KL_S");
	    map_kl.put("AllCount", list_kl_festival.size()+list_kl_station.size());
	    map_kl.put("List", dataT.datas2Audio(list_kl_festival,list_kl_station,0));
	    map_kl.put("ResultType", 0);
		System.out.println(map_kl.toString());
	}
}
