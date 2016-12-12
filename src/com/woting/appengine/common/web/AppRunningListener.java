package com.woting.appengine.common.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.WtAppEngineConstants;
import com.woting.appengine.common.cache.CacheRefreshListener;
import com.woting.dataanal.gather.API.ApiGatherListener;
import com.woting.searchword.SearchWordListener;
import com.woting.push.socketclient.oio.SocketClient;

public class AppRunningListener implements ServletContextListener {
    private Logger logger=LoggerFactory.getLogger(this.getClass());

    @Override
    //初始化
    public void contextInitialized(ServletContextEvent arg0) {
        try {
            //启动搜索词服务
            SearchWordListener.begin();
            //启动缓存刷新服务
            CacheRefreshListener.begin();
            //启动数据收集数据
            ApiGatherListener.begin();
            //启动Socket
            SocketClient sc=new SocketClient(null);
            sc.workStart();
            //注册到我们的内存
            SystemCache.setCache(new CacheEle<SocketClient>(WtAppEngineConstants.SOCKET_OBJ, "模块", sc));
        } catch(Exception e) {
            logger.error("Web运行时监听启动异常：",e);
        }
    }

    @Override
    //销毁
    public void contextDestroyed(ServletContextEvent arg0) {
    }
}