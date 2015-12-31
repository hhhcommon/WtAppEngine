package com.woting.appengine.appopinion.persistence.pojo;

import java.sql.Timestamp;

/**
 * 反馈信息<br/>
 * 对应持久化中数据库的表为wt_AppReOpinion
 * @author wh
 */
import com.spiritdata.framework.core.model.BaseObject;

/**
 * 意见信息<br/>
 * 对应持久化中数据库的表为wt_AppOpinion
 * @author wh
 */
public class AppReOpinionPo extends BaseObject {
    private static final long serialVersionUID = 6982498844605399519L;
    private String id; //反馈Id
    private String opinionId; //意见Id，反馈是针对那一条意见的
    private String userId; //反馈录入者Id，内部员工
    private String reOpinion; //反馈内容
    private Timestamp CTime; //意见意见成功提交时间
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getOpinionId() {
        return opinionId;
    }
    public void setOpinionId(String opinionId) {
        this.opinionId = opinionId;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getReOpinion() {
        return reOpinion;
    }
    public void setReOpinion(String reOpinion) {
        this.reOpinion = reOpinion;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime = cTime;
    }

}