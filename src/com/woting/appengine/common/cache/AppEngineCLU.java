package com.woting.appengine.common.cache;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
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

public class AppEngineCLU extends AbstractCacheLifecycleUnit {
    private Logger logger = Logger.getLogger(FrameworkUgaCLU.class);

    @Resource
    private DictService dictService;
    @Resource
    private ChannelService channelService;
    
    @Override
    public void init() {
        try {
            //装载数据字典
            loadDict();
        } catch (Exception e) {
            logger.info("启动时加载{Wt内容平台}缓存出错", e);
        }
        try {
            //装载栏目结构
            loadChannel();
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
            _CacheDictionary _cd=dictService.loadCache();
            SystemCache.remove(WtAppEngineConstants.CACHE_DICT);
            SystemCache.setCache(new CacheEle<_CacheDictionary>(WtAppEngineConstants.CACHE_DICT, "系统字典", _cd));
        } catch(Exception e) {
            throw new Wtcm1000CException("缓存[系统字典]失败", e);
        }
    }

    private void loadChannel() {
        try {
            System.out.println("开始装载[栏目结构]缓存");
            _CacheChannel _cc=channelService.loadCache();
            SystemCache.remove(WtAppEngineConstants.CACHE_CHANNEL);
            SystemCache.setCache(new CacheEle<_CacheChannel>(WtAppEngineConstants.CACHE_CHANNEL, "栏目结构", _cc));
        } catch(Exception e) {
            throw new Wtcm1000CException("缓存[栏目结构]失败", e);
        }
    }
}