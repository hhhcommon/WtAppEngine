package com.woting.searchword;

import com.woting.searchword.mem.SearchWordMemoryManage;

/**
 * 本服务主要用于处理搜索词列表，并把列表中的搜索词放入内存结构
 * @author wanghui
 */
public class SearchWordListener extends Thread {

    public static void begin() {
        SearchWordListener swl = new SearchWordListener();
        swl.start();
    }

    @Override
    public void run() {
        try {
            //加载搜索词服务，初始化内存结构
            SearchWordMemoryManage swmm=SearchWordMemoryManage.getInstance();
            swmm.initMemory();
            System.out.println("启动搜索词处理监控");
            while (true) {
                sleep(50);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}