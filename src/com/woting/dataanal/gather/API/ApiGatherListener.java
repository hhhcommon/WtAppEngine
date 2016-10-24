package com.woting.dataanal.gather.API;

import javax.servlet.ServletContext;

import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.dataanal.gather.API.mem.ApiGatherMemory;
import com.woting.dataanal.gather.API.persis.pojo.ApiLogPo;
import com.woting.dataanal.gather.API.service.ApiLogService;

public class ApiGatherListener extends Thread {

    private ApiLogService alService;

    /**
     * 启动“Api访问”数据收集监听线程
     */
    public static void begin() {
        ApiGatherListener vlr = new ApiGatherListener();
        vlr.setName("“API访问”数据收集监听");
        vlr.start();
    }

    /*
     * 写入数据库监控
     */
    private void startSave2DB(ApiGatherMemory agm) {
        while (true) {
            try {
                ApiLogPo alp=agm.takeQueue();
                if (alp!=null) {
                    alService.Save2DB(alp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 启动任务服务的处理主进程
     */
    @Override
    public void run() {
        try {
            sleep(5000);//多少毫秒后启动任务处理，先让系统的其他启动任务完成，这里设置死为10秒钟
            System.out.println("===============================");
            System.out.println(this.getName()+"线程开始执行");
            System.out.println("===============================");

            System.out.println("1-初始化日志队列");
            ApiGatherMemory agm = ApiGatherMemory.getInstance();
            System.out.println("2-启动["+this.getName()+"]过程");

            int i=0;
            while (this.alService==null&&i++<5) {
                try {
                    ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
                    if (sc!=null&&WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                        this.alService=(ApiLogService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("apiLogService");
                    }
                    sleep(500);
                } catch(Exception e) { }
            }
            if (this.alService!=null) startSave2DB(agm);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}