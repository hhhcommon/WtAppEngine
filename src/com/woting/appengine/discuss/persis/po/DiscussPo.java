package com.woting.appengine.discuss.persis.po;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

/**
 * 文章评论信息<br/>
 * 对应持久化中数据库的表为wt_Discuss
 * @author wh
 */
public class DiscussPo extends BaseObject {
    private static final long serialVersionUID=219569952009222030L;

    protected String id; //评论Id
    protected String userId; //提意见用户Id
    protected String resTableName; //资源类型，就是资源的主表名：1电台=wt_Broadcast；2单体媒体资源=wt_MediaAsset；3专辑资源=wt_SeqMediaAsset',
    protected String resId; //评论文章的Id
    protected String discuss; //意见内容
    protected Timestamp CTime; //意见意见成功提交时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id=id;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId=userId;
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
    public String getDiscuss() {
        return discuss;
    }
    public void setDiscuss(String discuss) {
        this.discuss=discuss;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp cTime) {
        CTime=cTime;
    }
}