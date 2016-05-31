package com.woting.searchword;

import javax.servlet.ServletContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.searchword.monitor.DealLoadQueue;
import com.woting.searchword.monitor.DealOnlineQueue;
import com.woting.searchword.monitor.LoadWord;
import com.woting.searchword.service.WordService;

/**
 * 本服务主要用于处理搜索词列表，并把列表中的搜索词放入内存结构
 * @author wanghui
 */
public class SearchWordListener extends Thread {
    private static WordService wordService;

    public static void begin() {
        ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
            SearchWordListener.wordService=(WordService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("wordService");
        }
        SearchWordListener swl=new SearchWordListener();
        swl.start();
    }

    @Override
    public void run() {
        try {
            sleep(200);
            if (SearchWordListener.wordService==null) {
                ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
                if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                    SearchWordListener.wordService=(WordService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("wordService");
                }
            }
            if (SearchWordListener.wordService!=null) {
                //启动在线监控
                (new DealOnlineQueue(SearchWordListener.wordService)).start();
                //启动加载监控
                (new DealLoadQueue(SearchWordListener.wordService)).start();
                //向加载队列输入内容
                (new LoadWord(SearchWordListener.wordService)).start();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}