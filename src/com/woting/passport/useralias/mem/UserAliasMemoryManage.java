package com.woting.passport.useralias.mem;

import java.util.List;

import javax.servlet.ServletContext;

import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.passport.useralias.model.UserAliasKey;
import com.woting.passport.useralias.persis.pojo.UserAliasPo;
import com.woting.passport.useralias.service.UserAliasService;

public class UserAliasMemoryManage {
    //java的占位单例模式===begin
    private static class InstanceHolder {
        public static UserAliasMemoryManage instance=new UserAliasMemoryManage();
    }
    public static UserAliasMemoryManage getInstance() {
        return InstanceHolder.instance;
    }
    //java的占位单例模式===end

    private boolean inited=false;

    //数据区
    protected UserAliasMemory uam; //用户别名结构

    /*
     * 构造方法，初始化用户别名的内存结构
     */
    private UserAliasMemoryManage() {
        uam=UserAliasMemory.getInstance();
    }

    /**
     * 从数据库中读取信息，并初始化内存结构
     */
    public synchronized void initMemory() {
        if (this.inited) return;
        ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
            UserAliasService userAliasService=(UserAliasService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("userAliasService");
            List<UserAliasPo> ual=userAliasService.getAllAlias();
            if (ual!=null&&ual.size()>0) {
                for (UserAliasPo uaPo: ual) {
                    this.addOneUserAlias(uaPo);
                }
            }
            this.inited=true;
        }
    }

    /**
     * 增加一个用户别名到内存
     * @param uaPo
     */
    public void addOneUserAlias(UserAliasPo uaPo) {
        this.uam.aliasMap.put(uaPo.getAliasKey().toKeyString(), uaPo);
    }

    /**
     * 得到用户别名
     * @param uaPo
     */
    public UserAliasPo getOneUserAlias(UserAliasKey uak) {
        return this.uam.aliasMap.get(uak.toKeyString());
    }

    /**
     * 根据别名Key删除别名
     * @param uak
     */
    public void delUserAlias(UserAliasKey uak) {
        this.uam.aliasMap.remove(uak.toKeyString());
    }

    /**
     * 根据组Id，删除一组内的所有别名
     * @param groupId
     */
    public void delAliasInOneGroup(String groupId) {
        if (this.uam.aliasMap!=null&&!this.uam.aliasMap.isEmpty()) {
            for (String k: this.uam.aliasMap.keySet()) {
                if (groupId.equals(this.uam.aliasMap.get(k).getTypeId())) {
                    this.uam.aliasMap.remove(k);
                }
            }
        }
    }

    /**
     * 删除某一用户组下的某一个用户
     * @param groupId 用户组Id
     * @param userId 用户Id
     */
    public void delUserAliasInGroup(String groupId, String userId) {
        if (this.uam.aliasMap!=null&&!this.uam.aliasMap.isEmpty()) {
            for (String k: this.uam.aliasMap.keySet()) {
                UserAliasPo uap=this.uam.aliasMap.get(k);
                if (groupId.equals(uap.getTypeId())&&(uap.getMainUserId().equals(userId)||uap.getAliasUserId().equals(userId))) {
                    this.uam.aliasMap.remove(k);
                }
            }
        }
    }
}