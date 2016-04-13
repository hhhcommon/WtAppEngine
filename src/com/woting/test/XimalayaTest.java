package com.woting.test;

import org.junit.Test;

import com.woting.appengine.searchcrawler.service.XiMaLaYaService;

public class XimalayaTest {

	XiMaLaYaService ximalaya = new XiMaLaYaService();
	@Test
	public void test(){
		ximalaya.xmlyService("周杰伦");
	}
}
