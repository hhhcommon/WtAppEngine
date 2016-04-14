package com.woting.searchword;

import com.woting.searchword.mem.SearchWordMemory;

/**
 * 本服务主要用于处理搜索词列表，并把列表中的搜索词放入内存结构
 * @author wanghui
 */
public class SearchWordListener extends Thread {

    public static void begin() {
        SearchWordListener swl=new SearchWordListener();
        swl.start();
    }

    @Override
    public void run() {
        try {
            //加载搜索词服务，初始化内存结构
            SearchWordMemory swm=SearchWordMemory.getInstance();
            swm.init();
            System.out.println("启动搜索词处理监控");

            while (true) {//每隔一段时间处理一次检索词
                sleep(50);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}