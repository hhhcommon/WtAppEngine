package com.woting.passport.useralias.model;

import com.mysql.jdbc.StringUtils;
import com.spiritdata.framework.core.model.BaseObject;
import com.woting.passport.useralias.persis.pojo.UserAliasPo;

/**
 * 别名Key
 * @author wanghui
 */
public class UserAliasKey extends BaseObject {
    private static final long serialVersionUID = -6228176062032997610L;

    private String typeId; //组或分类ID，这个需要特别说明，当为"FRIEND"时，是好友的别名，当为12位时是组Id
    private String mainUserId; //主用户Id
    private String aliasUserId; //别名用户Id

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

    /**
     * 构造别名key
     * @param typeId 类型或组Id
     * @param mainUserId 主用户Id
     * @param aliasUserId 别名用户Id
     */
    public UserAliasKey(String typeId, String mainUserId, String aliasUserId) {
        if (StringUtils.isEmptyOrWhitespaceOnly(typeId)
            ||StringUtils.isEmptyOrWhitespaceOnly(mainUserId)
            ||StringUtils.isEmptyOrWhitespaceOnly(aliasUserId)) {
            throw new IllegalArgumentException("所有参数不能为空");
        }
        this.typeId = typeId;
        this.mainUserId = mainUserId;
        this.aliasUserId = aliasUserId;
    }

    /**
     * 根据别名Po对象，构造别名key
     * @param aliasPo 别名Po对象
     */
    public UserAliasKey(UserAliasPo ap) {
        if (ap==null||StringUtils.isEmptyOrWhitespaceOnly(ap.getTypeId())
            ||StringUtils.isEmptyOrWhitespaceOnly(ap.getMainUserId())
            ||StringUtils.isEmptyOrWhitespaceOnly(ap.getAliasUserId())) {
            throw new IllegalArgumentException("所有参数不能为空");
        }
        this.typeId = ap.getTypeId();
        this.mainUserId = ap.getMainUserId();
        this.aliasUserId = ap.getAliasUserId();
    }

    /**
     * 转换未KeyString，用::隔开
     */
    public String toKeyString() {
        return this.typeId+"::"+this.mainUserId+"::"+this.aliasUserId;
    }
}