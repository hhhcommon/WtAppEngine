package com.woting.appengine.accusation.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

public class ContentAccusationPo extends BaseObject {
    private static final long serialVersionUID=-8606881716350334538L;

    private String id; //用户词Id
    private String resTableName; //资源类型表名wt_MediaAsset,wt_SeqMediaAsset,wt_broadcast
    private String resId; //资源Id
    private String userId; //举报者Id,若是过客，则存入其IMEI，并在前面加入::
    private String selReasons; //选择性原因,用逗号隔开,例：3244e3234e23444352245::侵权,234::违法
    private String inputReason; //输入性原因
    private Timestamp CTime; //记录创建时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id=id;
    }
    public String getResTableName() {
        return resTableName;
    }
    public void setResTableName(String resTableName) {
        this.resTableName=resTableName;
    }
    public String getResId() {
        return resId;
    }
    public void setResId(String resId) {
        this.resId=resId;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId=userId;
    }
    public String getSelReasons() {
        return selReasons;
    }
    public void setSelReasons(String selReasons) {
        this.selReasons=selReasons;
    }
    public String getInputReason() {
        return inputReason;
    }
    public void setInputReason(String inputReason) {
        this.inputReason=inputReason;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime=cTime;
    }
}