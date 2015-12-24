package com.woting.intercom.mem;

import java.util.concurrent.ConcurrentHashMap;
import com.woting.intercom.model.GroupInterCom;
import com.woting.passport.UGA.model.Group;

public class GroupMemory {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static GroupMemory instance = new GroupMemory();
    }
    public static GroupMemory getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    protected ConcurrentHashMap<String, GroupInterCom> gicMap;//对讲组信息Map

    private GroupMemory() {
        this.gicMap=new ConcurrentHashMap<String, GroupInterCom>();
    }

    /**
     * 把一个组加入gicMap
     * @param g 用户组模型
     */
    public void addOneGroup(Group g) {
        GroupInterCom groupIC = new GroupInterCom(g);
        this.gicMap.put(g.getGroupId(), groupIC);
    }
}