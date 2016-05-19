package com.woting.version.core.model;

import java.util.HashMap;
import java.util.Map;

import com.spiritdata.framework.core.model.BaseObject;
import com.spiritdata.framework.util.StringUtils;

/**
 * 版本基础配置信息，这个对象就是持久化对象的结构
 * @author wanghui
 */
public class VersionConfig extends BaseObject {
    private static final long serialVersionUID = 3910443489504642841L;

    private String pubStorePath; //最终版本发布存储目录
    private String pubFileName;  //最终版本发布Apk名称
    private String pubUrl;       //最终版本发布的Url
    private String verGoodsStorePath; //历史版本发布物存储目录

    public String getPubStorePath() {
        return pubStorePath;
    }
    public void setPubStorePath(String pubStorePath) {
        this.pubStorePath = pubStorePath;
    }
    public String getPubFileName() {
        return pubFileName;
    }
    public void setPubFileName(String pubFileName) {
        this.pubFileName = pubFileName;
    }
    public String getPubUrl() {
        return pubUrl;
    }
    public void setPubUrl(String pubUrl) {
        this.pubUrl = pubUrl;
    }
    public String getVerGoodsStorePath() {
        return verGoodsStorePath;
    }
    public void setVerGoodsStorePath(String verGoodsStorePath) {
        this.verGoodsStorePath = verGoodsStorePath;
    }

    /**
     * 转换为为显示使用的数据类型
     * @return 显示层所用的数据类型，为Map
     */
    public Map<String, Object> toHashMap4View() {
        Map<String, Object> ret=new HashMap<String, Object>();
        if (StringUtils.isNullOrEmptyOrSpace(pubStorePath)) ret.put("PubStorPath", pubStorePath);
        if (StringUtils.isNullOrEmptyOrSpace(pubFileName)) ret.put("PubFileName", pubFileName);
        if (StringUtils.isNullOrEmptyOrSpace(pubUrl)) ret.put("PubUrl", pubUrl);
        if (StringUtils.isNullOrEmptyOrSpace(verGoodsStorePath)) ret.put("VerGoodsStorePath", verGoodsStorePath);
        return ret;
    }
}