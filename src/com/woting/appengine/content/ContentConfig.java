package com.woting.appengine.content;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.greenpineyu.fel.FelEngine;
import com.greenpineyu.fel.FelEngineImpl;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.jsonconf.JsonConfig;
import com.spiritdata.framework.util.StringUtils;
import com.woting.push.config.Config;

/**
 * Socket连接客户端配置信息
 * @author wanghui
 */
public class ContentConfig implements Config {
    protected Logger logger=LoggerFactory.getLogger(this.getClass());

    private boolean useRedis; //是否使用Redis获取内容数据，用以提速
    public boolean isUseRedis() {
        return useRedis;
    }
    public void setUseRedis(boolean useRedis) {
        this.useRedis=useRedis;
    }

    private boolean resident; //是否使用Redis获取内容数据，用以提速
    public boolean isResident() {
        return resident;
    }
    public void setResident(boolean resident) {
        this.resident=resident;
    }

    private long expiredTime; //redis过期时间
    public long getExpiredTime() {
        return expiredTime;
    }
    public void setExpiredTime(long expiredTime) {
        this.expiredTime = expiredTime;
    }

    @SuppressWarnings("unchecked")
    public ContentConfig loadConfig() {
        //生成默认
        ContentConfig cc=new ContentConfig();
        JsonConfig jc=null;
        //设置默认值
        resident=true;
        useRedis=true;
        
        try {
            String configFileName=(SystemCache.getCache(FConstants.APPOSPATH)==null?"":((CacheEle<String>)(SystemCache.getCache(FConstants.APPOSPATH))).getContent());
            configFileName+="WEB-INF"+File.separator+"app.jconf";
            jc=new JsonConfig(configFileName);
        } catch(Exception e) {
            cc.logger.info(StringUtils.getAllMessage(e));
            jc=null;
            e.printStackTrace();
        }
        if (jc!=null) {
            FelEngine fel=new FelEngineImpl();
            int tmpInt=-1;
            try {
                tmpInt=(int)fel.eval(jc.getString("contentRedisCfg.useRedis"));
            } catch(Exception e) {tmpInt=-1;}
            setUseRedis(tmpInt==1);
            tmpInt=-1;

            try {
                tmpInt=(int)fel.eval(jc.getString("contentRedisCfg.isResident"));
            } catch(Exception e) {tmpInt=-1;}
            setResident(tmpInt==1);
            tmpInt=-1;

            try {
                tmpInt=(int)fel.eval(jc.getString("contentRedisCfg.expiredTime"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) setExpiredTime(tmpInt);
            tmpInt=-1;
        }

        return cc;
    }
}