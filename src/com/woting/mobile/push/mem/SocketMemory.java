package com.woting.mobile.push.mem;

import java.util.concurrent.ConcurrentHashMap;
import com.woting.mobile.model.MobileKey;
import com.woting.mobile.push.monitor.socket.SocketHandle;

public class SocketMemory {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static SocketMemory instance = new SocketMemory();
    }
    public static SocketMemory getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    protected ConcurrentHashMap<MobileKey, SocketHandle> socketMap;//将要发送的消息列表
}