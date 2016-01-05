package com.woting.appengine.common.cache;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import com.spiritdata.framework.component.UGA.cache.FrameworkUgaCLU;
import com.spiritdata.framework.core.cache.AbstractCacheLifecycleUnit;
import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.woting.WtAppEngineConstants;
import com.woting.dictionary.model._CacheDictionary;
import com.woting.dictionary.service.DictCacheService;
import com.woting.exceptionC.Wtcm1000CException;

public class AppEngineCLU extends AbstractCacheLifecycleUnit {
    private Logger logger = Logger.getLogger(FrameworkUgaCLU.class);

    @Resource
    private DictCacheService dictCacheService;
    
    @Override
    public void init() {
        try {
            //装载数据字典
            loadDict();
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
            _CacheDictionary _cd = dictCacheService.loader();
            SystemCache.remove(WtAppEngineConstants.CACHE_DICT);
            SystemCache.setCache(new CacheEle<_CacheDictionary>(WtAppEngineConstants.CACHE_DICT, "系统字典", _cd));
        } catch(Exception e) {
            throw new Wtcm1000CException("缓存[系统字典]失败", e);
        }
        
    }
}