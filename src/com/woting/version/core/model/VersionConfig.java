package com.woting.version.core.model;

import com.spiritdata.framework.core.model.BaseObject;

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
}