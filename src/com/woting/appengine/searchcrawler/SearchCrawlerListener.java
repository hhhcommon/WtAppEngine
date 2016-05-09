package com.woting.appengine.searchcrawler;

public class SearchCrawlerListener extends Thread {
	private static SearchCrawlerConfig scc;

	public static void begin(SearchCrawlerConfig scc){
		SearchCrawlerListener.scc = scc;
		SearchCrawlerListener scl = new SearchCrawlerListener();
		scl.start();
	}
	
	@Override
	public void run() {
		System.out.println("启动搜索抓取服务");
	}
}
