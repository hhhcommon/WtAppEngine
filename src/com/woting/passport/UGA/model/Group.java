package com.woting.passport.UGA.model;

import java.util.List;

import com.spiritdata.framework.core.model.BaseObject;
import com.woting.passport.UGA.persistence.pojo.GroupPo;
import com.woting.passport.UGA.persistence.pojo.UserPo;

/**
 * 带用户组成员的用户组信息
 * @author wanghui
 */
public class Group extends BaseObject {
    private static final long serialVersionUID = 7365795273402631290L;

    private GroupPo gp;
    public GroupPo getGp() {
        return gp;
    }
    public void setGp(GroupPo gp) {
        this.gp = gp;
    }

    private List<UserPo> userList;
    public List<UserPo> getUserList() {
        return userList;
    }
    public void setUserList(List<UserPo> userList) {
        this.userList = userList;
    }
}