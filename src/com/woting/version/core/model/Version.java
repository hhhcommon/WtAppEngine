package com.woting.version.core.model;

import java.sql.Timestamp;
import com.spiritdata.framework.core.model.BaseObject;

public class Version extends BaseObject {
    private static final long serialVersionUID = -1670331511245273735L;

    private String id; //版本ID(UUID)
    private String appName; //应用名称，这里的App不单单值手机应用',
    private String verNum; //版本号，此版本号的规则由程序通过正则表达式进行处理',
    private String verMemo; //版本描述，可以是一段html',
    private String bugMemo; //版本bug修改情况描述，可以是一段html',
    private int pubFlag; //发布状态：1=已发布；0=未发布；此状态用于今后扩展，目前只有1',
    private String apkUrl; //版本发布物的访问Url,目前仅针对apk',
    private int apkSize; //版本发布物尺寸大小，是字节数,目前仅针对apk',
    private int isCurVer; //是否是当前版本，0不是，1是',
    private Timestamp pubTime; //发布时间',
    private Timestamp cTime; //创建时间',
    private Timestamp lmTime; //最后修改时间',

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getAppName() {
        return appName;
    }
    public void setAppName(String appName) {
        this.appName = appName;
    }
    public String getVerNum() {
        return verNum;
    }
    public void setVerNum(String verNum) {
        this.verNum = verNum;
    }
    public String getVerMemo() {
        return verMemo;
    }
    public void setVerMemo(String verMemo) {
        this.verMemo = verMemo;
    }
    public String getBugMemo() {
        return bugMemo;
    }
    public void setBugMemo(String bugMemo) {
        this.bugMemo = bugMemo;
    }
    public int getPubFlag() {
        return pubFlag;
    }
    public void setPubFlag(int pubFlag) {
        this.pubFlag = pubFlag;
    }
    public String getApkUrl() {
        return apkUrl;
    }
    public void setApkUrl(String apkUrl) {
        this.apkUrl = apkUrl;
    }
    public int getApkSize() {
        return apkSize;
    }
    public void setApkSize(int apkSize) {
        this.apkSize = apkSize;
    }
    public int getIsCurVer() {
        return isCurVer;
    }
    public void setIsCurVer(int isCurVer) {
        this.isCurVer = isCurVer;
    }
    public Timestamp getPubTime() {
        return pubTime;
    }
    public void setPubTime(Timestamp pubTime) {
        this.pubTime = pubTime;
    }
    public Timestamp getcTime() {
        return cTime;
    }
    public void setcTime(Timestamp cTime) {
        this.cTime = cTime;
    }
    public Timestamp getLmTime() {
        return lmTime;
    }
    public void setLmTime(Timestamp lmTime) {
        this.lmTime = lmTime;
    }
}