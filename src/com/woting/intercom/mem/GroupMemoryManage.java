package com.woting.intercom.mem;

public class GroupMemoryManage {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static GroupMemoryManage instance = new GroupMemoryManage();
    }
    public static GroupMemoryManage getInstance() {
        return InstanceHolder.instance;
    }

}