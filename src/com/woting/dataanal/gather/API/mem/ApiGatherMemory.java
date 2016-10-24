package com.woting.dataanal.gather.API.mem;

import java.util.concurrent.LinkedBlockingQueue;

import com.woting.dataanal.gather.API.persis.pojo.ApiLogPo;

import java.sql.Timestamp;
import java.util.concurrent.BlockingQueue;

/**
 * Api访问数据收集的内存结构
 * @author wh
 */
public class ApiGatherMemory {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static ApiGatherMemory instance = new ApiGatherMemory();
    }
    public static ApiGatherMemory getInstance() {
        InstanceHolder.instance.init();
        return InstanceHolder.instance;
    }

    //信息收集队列：Api访问数据收集后先放入本队列，之后再由一个线程把他写入持久化中，目前是数据库
    protected BlockingQueue<ApiLogPo> apiGatherQueue = null;

    /**
     * 参数初始化，必须首先执行这个方法
     */
    public void init() {
        if (apiGatherQueue==null) apiGatherQueue=new LinkedBlockingQueue<ApiLogPo>();
    }

    /**
     * 从队列中获得需要处理的数据
     * @throws InterruptedException 
     */
    public ApiLogPo takeQueue() throws InterruptedException {
        return this.apiGatherQueue.take();
    }

    /**
     * 存储收集到的数据信息到队列
     * @param vlp Api数据收集
     * @throws InterruptedException 
     */
    public void put2Queue(ApiLogPo alPo) throws InterruptedException {
        if (alPo.getBeginTime()==null) alPo.setBeginTime(new Timestamp(System.currentTimeMillis()));
        this.apiGatherQueue.put(alPo);
    }
}