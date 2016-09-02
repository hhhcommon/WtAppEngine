package com.woting.appengine.common.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.woting.appengine.calling.CallingConfig;
import com.woting.appengine.calling.CallingListener;
import com.woting.appengine.common.cache.CacheRefreshListener;
import com.woting.appengine.intercom.InterComConfig;
import com.woting.appengine.intercom.InterComListener;
import com.woting.appengine.mobile.mediaflow.MfConfig;
import com.woting.appengine.mobile.mediaflow.MfListener;
import com.woting.appengine.mobile.push.PushConfig;
import com.woting.appengine.mobile.push.PushListener;
import com.woting.searchword.SearchWordListener;

public class AppRunningListener implements ServletContextListener {
    private Logger logger=LoggerFactory.getLogger(this.getClass());

    @Override
    //初始化
    public void contextInitialized(ServletContextEvent arg0) {
        try {
            //启动搜索词服务
            SearchWordListener.begin();
            //启动对讲处理服务
            InterComListener.begin(new InterComConfig());
            //启动电话处理服务
            CallingListener.begin(new CallingConfig());
            //启动流数据处理服务
            MfListener.begin(new MfConfig());
            //启动推送服务
            PushListener.begin(new PushConfig());
            //启动缓存刷新服务
            CacheRefreshListener.begin();
        } catch(Exception e) {
            logger.error("Web运行时监听启动异常：",e);
        }
    }

    @Override
    //销毁
    public void contextDestroyed(ServletContextEvent arg0) {
    }
}