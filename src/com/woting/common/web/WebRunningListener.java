package com.woting.common.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.woting.intercom.InterComConfig;
import com.woting.intercom.InterComListener;
import com.woting.mobile.push.PushConfig;
import com.woting.mobile.push.PushListener;
import com.woting.mobile.session.MobileSessionConfig;
import com.woting.mobile.session.SessionListener;

public class WebRunningListener implements ServletContextListener {
    private Logger logger = Logger.getLogger(this.getClass());

    @Override
    //初始化
    public void contextInitialized(ServletContextEvent arg0) {
        try {
            //移动会话Session启动
            MobileSessionConfig msc = new MobileSessionConfig();
            SessionListener.begin(msc);
            //启动推送服务
            PushConfig pc = new PushConfig();
            PushListener.begin(pc);
            //启动对讲处理服务
            InterComConfig icc = new InterComConfig();
            InterComListener.begin(icc);
        } catch(Exception e) {
            logger.error("Web运行时监听启动异常：",e);
        }
    }

    @Override
    //销毁
    public void contextDestroyed(ServletContextEvent arg0) {
    }
}