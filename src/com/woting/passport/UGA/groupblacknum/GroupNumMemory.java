package com.woting.passport.UGA.groupblacknum;

public class GroupNumMemory {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static GroupNumMemory instance = new GroupNumMemory();
    }
    public static GroupNumMemory getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    protected byte[] gNum=new byte[999999999];

}