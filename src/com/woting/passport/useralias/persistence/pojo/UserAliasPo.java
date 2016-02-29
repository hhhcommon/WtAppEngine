package com.woting.passport.useralias.persistence.pojo;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;
import com.woting.passport.useralias.model.UserAliasKey;

/**
 * 别名Po对象
 * @author wanghui
 */
public class UserAliasPo extends BaseObject {
    private static final long serialVersionUID = -9013738765639837898L;

    private String id; //Id，主键
    private String typeId; //组或分类ID，这个需要特别说明，当为"FRIEND"时，是好友的别名，当为12位时是组Id
    private String mainUserId; //主用户Id
    private String aliasUserId; //别名用户Id
    private String aliasName; //用户好友别名
    private String aliasDescn; //用户好友描述
    private Timestamp lastModifyTime; //最后修改时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getTypeId() {
        return typeId;
    }
    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }
    public String getMainUserId() {
        return mainUserId;
    }
    public void setMainUserId(String mainUserId) {
        this.mainUserId = mainUserId;
    }
    public String getAliasUserId() {
        return aliasUserId;
    }
    public void setAliasUserId(String aliasUserId) {
        this.aliasUserId = aliasUserId;
    }
    public String getAliasName() {
        return aliasName;
    }
    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }
    public String getAliasDescn() {
        return aliasDescn;
    }
    public void setAliasDescn(String aliasDescn) {
        this.aliasDescn = aliasDescn;
    }
    public Timestamp getLastModifyTime() {
        return lastModifyTime;
    }
    public void setLastModifyTime(Timestamp lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    public UserAliasKey getAliasKey() {
        return new UserAliasKey(this);
    }

    public boolean equals(UserAliasPo uap) {
        if (!this.typeId.equals(uap.getTypeId())) return false;
        if (!this.mainUserId.equals(uap.getMainUserId())) return false;
        if (!this.aliasUserId.equals(uap.getAliasUserId())) return false;
        if (!this.aliasName.equals(uap.getAliasName())) return false;
        if (!this.aliasDescn.equals(uap.getAliasDescn())) return false;
        return true;
    }
}