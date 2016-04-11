package com.woting.appengine.searchcrawler.test;

import org.junit.Test;

import com.woting.appengine.searchcrawler.service.qt_s_service;

public class qt_test {

	qt_s_service qt_s = new qt_s_service();
	
	@Test
	public void test(){
		qt_s.qt_S("周杰伦");
//		qt_s.festivalS("http://www.qingting.fm/s/vchannels/124882/programs/3659487");
	}
}
