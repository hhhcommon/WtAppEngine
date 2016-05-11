package com.woting.appengine.common.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.woting.appengine.calling.CallingConfig;
import com.woting.appengine.calling.CallingListener;
import com.woting.appengine.intercom.InterComConfig;
import com.woting.appengine.intercom.InterComListener;
import com.woting.appengine.mobile.mediaflow.MfConfig;
import com.woting.appengine.mobile.mediaflow.MfListener;
import com.woting.appengine.mobile.push.PushConfig;
import com.woting.appengine.mobile.push.PushListener;
import com.woting.appengine.mobile.session.MobileSessionConfig;
import com.woting.appengine.mobile.session.SessionListener;
import com.woting.appengine.searchcrawler.SearchCrawlerConfig;
import com.woting.appengine.searchcrawler.SearchCrawlerListener;
import com.woting.searchword.SearchWordListener;

public class AppRunningListener implements ServletContextListener {
    private Logger logger=LoggerFactory.getLogger(this.getClass());

    @Override
    //初始化
    public void contextInitialized(ServletContextEvent arg0) {
        try {
            //启动搜索词服务
            SearchWordListener.begin();
            //移动会话Session启动
            SessionListener.begin(new MobileSessionConfig());
            //启动对讲处理服务
            InterComListener.begin(new InterComConfig());
            //启动电话处理服务
            CallingListener.begin(new CallingConfig());
            //启动流数据处理服务
            MfListener.begin(new MfConfig());
            //启动推送服务
            PushListener.begin(new PushConfig());
            //启动搜索抓去服务
            SearchCrawlerListener.begin(new SearchCrawlerConfig());
        } catch(Exception e) {
            logger.error("Web运行时监听启动异常：",e);
        }
    }

    @Override
    //销毁
    public void contextDestroyed(ServletContextEvent arg0) {
    }
}