package com.woting.passport.useralias.mem;

import java.util.concurrent.ConcurrentHashMap;

import com.woting.passport.useralias.persistence.pojo.UserAliasPo;

public class UserAliasMemory {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static UserAliasMemory instance = new UserAliasMemory();
    }
    public static UserAliasMemory getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    protected ConcurrentHashMap<String, UserAliasPo> aliasMap;//别名Map

    private UserAliasMemory() {
        this.aliasMap=new ConcurrentHashMap<String, UserAliasPo>();
    }
}