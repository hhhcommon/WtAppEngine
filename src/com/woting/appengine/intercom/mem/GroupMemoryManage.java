package com.woting.appengine.intercom.mem;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.appengine.intercom.model.GroupInterCom;
import com.woting.appengine.mobile.model.MobileKey;
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

    private boolean inited=false;

    /**
     * 从数据库中读取信息，并初始化内存结构
     */
    public synchronized void initMemory() {
        if (this.inited) return;
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
        this.inited=true;
    }

    //把一个用户组对象加入gicMap
    public void addOneGroup(Group g) {
        GroupInterCom gic = new GroupInterCom(g);
        this.gm.gicMap.put(g.getGroupId(), gic);
    }

    //删除一个组
    public void delOneGroup(Group g) {
        this.gm.gicMap.remove(g.getGroupId());
    }

    /**
     * 根据用户组id，获得用户组对讲模型
     */
    public GroupInterCom getGroupInterCom(String groupId) {
        return this.gm.gicMap.get(groupId);
    }

    /**
     * 获取内存中所有数据
     */
    public Map<String, GroupInterCom> getAllGroup() {
        return this.gm.gicMap;
    }

    /**
     * 清除对话过期的对讲者
     * @param expireTime 过期时间
     */
    public void cleanSpeaker(long expireTime) {
        GroupInterCom gic=null;
        long b=System.currentTimeMillis();
        for (String k: this.gm.gicMap.keySet()) {
            gic=this.gm.gicMap.get(k);
            if (gic==null||gic.getSpeaker()==null||gic.getLastTalkTime()==-1) continue;
            if (System.currentTimeMillis()-gic.getLastTalkTime()>expireTime) {
                gic.sendEndPTT();
                gic.delSpeaker(gic.getSpeaker().getUserId());
            }
        }
    }

    /*
     * 这是一个可能会被删除掉的方法，用于收到b时，根据用户信息设置对讲组最后的说话时间
     * @param mk 用户信息
     */
    public void setLastTalkTime(MobileKey mk) {
        for (String k: this.gm.gicMap.keySet()) {
            this.gm.gicMap.get(k).setLastTalkTime(mk.getUserId());
        }
    }
}