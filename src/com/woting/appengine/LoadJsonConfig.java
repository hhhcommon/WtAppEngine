package com.woting.appengine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.jsonconf.JsonConfig;
import com.spiritdata.framework.util.StringUtils;
import com.woting.WtAppEngineConstants;
import com.woting.push.socketclient.SocketClientConfig;

@Service
public class LoadJsonConfig {
    private Logger logger=LoggerFactory.getLogger(LoadJsonConfig.class);

    private String configFilePath;
    public String getConfigFilePath() {
        return configFilePath;
    }
    public void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    public void loadConfig() throws IOException {
        if (!StringUtils.isNullOrEmptyOrSpace(configFilePath)) {
            JsonConfig jc=new JsonConfig(configFilePath);
            logger.info("配置文件信息={}", jc.getAllConfInfo());

            SocketClientConfig scc=ConfigLoadUtils.getSocketClientConfig(jc);
            SystemCache.setCache(new CacheEle<SocketClientConfig>(WtAppEngineConstants.SOCKET_CLIENTCONF, "Socket客户端配置", scc));
        }
    }
}