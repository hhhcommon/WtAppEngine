package com.woting.appengine.common.cache;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.spiritdata.framework.component.UGA.cache.FrameworkUgaCLU;
import com.spiritdata.framework.core.cache.AbstractCacheLifecycleUnit;
import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.WtAppEngineConstants;
import com.woting.cm.core.channel.mem._CacheChannel;
import com.woting.cm.core.channel.service.ChannelService;
import com.woting.cm.core.dict.mem._CacheDictionary;
import com.woting.cm.core.dict.service.DictService;
import com.woting.exceptionC.Wtcm1000CException;
import com.woting.passport.useralias.mem.UserAliasMemoryManage;
import com.woting.version.core.model.VersionConfig;
import com.woting.version.core.service.VersionService;

public class AppEngineCLU extends AbstractCacheLifecycleUnit {
    private Logger logger=LoggerFactory.getLogger(FrameworkUgaCLU.class);

    @Resource
    private DictService dictService;
    @Resource
    private ChannelService channelService;
    @Resource
    private VersionService versionService;

        
    @Override
    public void init() {
        //装载别名架构数据
        try {
            loadAlias();
        } catch (Exception e) {
            logger.info("启动时加载{Wt内容平台}缓存出错", e);
        }
        //装载数据字典
        try {
            loadDict();
        } catch (Exception e) {
            logger.info("启动时加载{Wt内容平台}缓存出错", e);
        }
        //装载栏目结构
        try {
            loadChannel();
        } catch (Exception e) {
            logger.info("启动时加载{Wt内容平台}缓存出错", e);
        }
        //装载版本配置
        try {
            loadVersion();
        } catch (Exception e) {
            logger.info("启动时加载{Wt内容平台}缓存出错", e);
        }
   }

    @Override
    public void refresh(String arg0) {
    }

    private void loadDict() {
        try {
            System.out.println("开始装载[系统字典]缓存");
            SystemCache.remove(WtAppEngineConstants.CACHE_DICT);
            SystemCache.setCache(new CacheEle<_CacheDictionary>(WtAppEngineConstants.CACHE_DICT, "系统字典", dictService.loadCache()));
        } catch(Exception e) {
            throw new Wtcm1000CException("缓存[系统字典]失败", e);
        }
    }

    private void loadChannel() {
        try {
            System.out.println("开始装载[栏目结构]缓存");
            SystemCache.remove(WtAppEngineConstants.CACHE_CHANNEL);
            SystemCache.setCache(new CacheEle<_CacheChannel>(WtAppEngineConstants.CACHE_CHANNEL, "栏目结构", channelService.loadCache()));
        } catch(Exception e) {
            throw new Wtcm1000CException("缓存[栏目结构]失败", e);
        }
    }

    private void loadAlias() {
        try {
            System.out.println("开始加载[别名]内存结构");
            //初始化内存结构
            UserAliasMemoryManage uamm=UserAliasMemoryManage.getInstance();
            uamm.initMemory();
        } catch(Exception e) {
            throw new Wtcm1000CException("加载[别名]内存结构失败", e);
        }
    }

    private void loadVersion() {
        try {
            System.out.println("开始装载[版本配置]缓存");
            SystemCache.remove(WtAppEngineConstants.APP_VERSIONCONFIG);
            SystemCache.setCache(new CacheEle<VersionConfig>(WtAppEngineConstants.APP_VERSIONCONFIG, "版本设置", versionService.getVerConfig()));
        } catch(Exception e) {
            throw new Wtcm1000CException("缓存[版本配置]失败", e);
        }
    }
}