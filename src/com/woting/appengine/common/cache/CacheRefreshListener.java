package com.woting.appengine.common.cache;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContext;

import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.WtAppEngineConstants;
import com.woting.version.core.model.VersionConfig;
import com.woting.version.core.service.VersionService;

/**
 * 缓存数据更新的的监听进程
 */
public class CacheRefreshListener extends Thread {
    private long intervalRefreshTime=60*60*1000; //刷新间隔，1小时。注意，这个的刷新间隔是对所有内存数据有效的，这种粒度太粗糙，先这样

    public static void begin() {
        CacheRefreshListener crl=new CacheRefreshListener();
        crl.start();
    }

    @Override
    public void run() {
        try {
            sleep(3000);//多少毫秒后启动任务处理，尽量等(最好能够判断)内存加载完成
            (new Timer("系统缓存定时更新任务，每隔["+intervalRefreshTime+"]毫秒执行", true))
            .schedule(new RefrshCacheTask(), new Date(), intervalRefreshTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class RefrshCacheTask extends TimerTask {
    @Override
    public void run() {
        try {
            ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
            if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                VersionService versionService=(VersionService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("versionService");
                if (versionService!=null) {
                    SystemCache.remove(WtAppEngineConstants.APP_VERSIONCONFIG);
                    SystemCache.setCache(new CacheEle<VersionConfig>(WtAppEngineConstants.APP_VERSIONCONFIG, "版本设置", versionService.getVerConfig()));
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}