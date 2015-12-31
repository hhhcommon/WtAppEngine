package com.woting.appengine.appopinion.persistence.pojo;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

/**
 * 意见信息<br/>
 * 对应持久化中数据库的表为wt_AppOpinion
 * @author wh
 */
public class AppOpinionPo extends BaseObject {
    private static final long serialVersionUID = 219569952009222030L;

    private String id; //意见Id
    private String imei; //手机串号，若是PC，则必须是网卡的Mac地址
    private String userId; //提意见用户Id
    private String opinion; //意见内容
    private Timestamp CTime; //意见意见成功提交时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getImei() {
        return imei;
    }
    public void setImei(String imei) {
        this.imei = imei;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getOpinion() {
        return opinion;
    }
    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime = cTime;
    }
}