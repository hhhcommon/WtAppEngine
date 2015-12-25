package com.woting.intercom.mem;

import java.util.List;
import javax.servlet.ServletContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.intercom.model.GroupInterCom;
import com.woting.passport.UGA.model.Group;
import com.woting.passport.UGA.service.GroupService;

public class GroupMemoryManage {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static GroupMemoryManage instance = new GroupMemoryManage();
    }
    public static GroupMemoryManage getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    //数据区
    protected GroupMemory gm; //接收数据内存结构

    /*
     * 构造方法，初始化消息推送的内存结构
     */
    private GroupMemoryManage() {
        gm=GroupMemory.getInstance();
    }

    /**
     * 从数据库中读取信息，并初始化内存结构
     */
    public void initMemory() {
        ServletContext sc = (ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent();
        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
            GroupService groupService = (GroupService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("groupService");
            List<Group> groups = groupService.getAllGroup();
            if (groups!=null&&groups.size()>0) {
                for (Group g: groups) {
                    this.addOneGroup(g);
                }
            }
        }
    }
    //把一个用户组对象加入gicMap
    private void addOneGroup(Group g) {
        GroupInterCom groupIC = new GroupInterCom(g);
        this.gm.gicMap.put(g.getGroupId(), groupIC);
    }

    /**
     * 根据用户组id，获得用户组对讲模型
     */
    public GroupInterCom getGroupInterCom(String groupId) {
        return this.gm.gicMap.get(groupId);
    }
}