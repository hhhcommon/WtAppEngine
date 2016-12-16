package com.woting.appengine.common.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.greenpineyu.fel.FelEngine;
import com.greenpineyu.fel.FelEngineImpl;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.jsonconf.JsonConfig;
import com.spiritdata.framework.util.StringUtils;
import com.woting.WtAppEngineConstants;
import com.woting.appengine.common.cache.CacheRefreshListener;
import com.woting.dataanal.gather.API.ApiGatherListener;
import com.woting.searchword.SearchWordListener;
import com.woting.push.socketclient.SocketClientConfig;
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
            SocketClient sc=new SocketClient(loadSocketConfig());
            sc.workStart();
            SystemCache.setCache(new CacheEle<SocketClient>(WtAppEngineConstants.SOCKET_OBJ, "模块", sc));//注册到内存
        } catch(Exception e) {
            logger.error("Web运行时监听启动异常：",e);
        }
    }

    @Override
    //销毁
    public void contextDestroyed(ServletContextEvent arg0) {
    }

    @SuppressWarnings("unchecked")
    private SocketClientConfig loadSocketConfig() {
        //生成默认
        SocketClientConfig scc=new SocketClientConfig();
        scc.setIp("localhost");
        scc.setPort(16789);
        scc.setIntervalBeat(23*1000);
        scc.setIntervalCheckSocket(19*1000);
        scc.setExpireTime(30*60*1000);
        scc.setStopDelay(10*1000);
        List<String> reConnStrategy=new ArrayList<String>();
        reConnStrategy.add("INTE::500");
        reConnStrategy.add("INTE::1000");
        reConnStrategy.add("INTE::3000");
        reConnStrategy.add("INTE::6000");
        reConnStrategy.add("GOTO::0");
        scc.setReConnStrategy(reConnStrategy);
        scc.setLogPath(null);

        //读取配置文件
        JsonConfig jc=null;
        try {
            String configFileName=(SystemCache.getCache(FConstants.APPOSPATH)==null?"":((CacheEle<String>)(SystemCache.getCache(FConstants.APPOSPATH))).getContent());
            configFileName+="\\WEB-INF\\app.jconf";
            jc=new JsonConfig(configFileName);
            logger.info("配置文件信息={}", jc.getAllConfInfo());
        } catch(Exception e) {
            jc=null;
            e.printStackTrace();
        }
        if (jc!=null) {
            FelEngine fel=new FelEngineImpl();
            String tmpStr="";
            int tmpInt=-1;

            try {
                tmpStr=jc.getString("socketClient.ip");
            } catch(Exception e) {tmpStr="";}
            if (!StringUtils.isNullOrEmptyOrSpace(tmpStr)) scc.setIp(tmpStr);
            tmpStr="";

            try {
                tmpInt=(int)fel.eval(jc.getString("socketClient.port"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) scc.setPort(tmpInt);
            tmpInt=-1;

            try {
                tmpInt=(int)fel.eval(jc.getString("socketClient.intervalBeat"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) scc.setIntervalBeat(tmpInt);
            tmpInt=-1;
            
            try {
                tmpInt=(int)fel.eval(jc.getString("socketClient.intervalCheckSocket"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) scc.setIntervalCheckSocket(tmpInt);
            tmpInt=-1;

            try {
                tmpInt=(int)fel.eval(jc.getString("socketClient.expireTime"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) scc.setExpireTime(tmpInt);
            tmpInt=-1;

            try {
                tmpInt=(int)fel.eval(jc.getString("socketClient.stopDelay"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) scc.setStopDelay(tmpInt);
            tmpInt=-1;

            try {
                tmpInt=(int)fel.eval(jc.getString("socketClient.reConnStrategy#size"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) {
                List<String> _reConnStrategy=new ArrayList<String>();
                for (int i=0; i<tmpInt; i++) {
                    try {
                        tmpStr=jc.getString("socketClient.reConnStrategy["+i+"]");
                    } catch(Exception e) {tmpStr="";}
                    if (!StringUtils.isNullOrEmptyOrSpace(tmpStr)) _reConnStrategy.add(tmpStr);
                }
                if (!_reConnStrategy.isEmpty()) {
                    scc.setReConnStrategy(_reConnStrategy);
                }
            }
            tmpStr="";

            try {
                tmpStr=jc.getString("logPath");
            } catch(Exception e) {tmpStr="";}
            if (!StringUtils.isNullOrEmptyOrSpace(tmpStr)) scc.setLogPath(tmpStr);
        }

        return scc;
    }
}