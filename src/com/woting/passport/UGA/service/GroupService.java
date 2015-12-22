package com.woting.passport.UGA.service;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.woting.passport.UGA.persistence.pojo.GroupPo;
import com.woting.passport.UGA.persistence.pojo.GroupUserPo;
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.SequenceUUID;

public class GroupService {
    @Resource(name="defaultDAO")
    private MybatisDAO<UserPo> userDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<GroupPo> groupDao;

    @PostConstruct
    public void initParam() {
        userDao.setNamespace("WT_USER");
        groupDao.setNamespace("WT_GROUP");
    }

    /**
     * 创建用户组
     * @param group 用户组信息
     * @return 创建用户成功返回1，否则返回0
     */
    public int insertGroup(GroupPo group) {
        int i=0;
        try {
            group.setGroupId(SequenceUUID.getUUIDSubSegment(4));
            groupDao.insert(group);
            //插入用户
            List<UserPo> ul = group.getGroupUsers();
            if (ul!=null&&ul.size()>0) {
                for (UserPo u: ul) this.insertGroupUser(group, u);
            }
            i=1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * 创建用户用户组关系
     * @param g 用户组
     * @param u 用户
     */
    public int insertGroupUser(GroupPo g, UserPo u) {
        GroupUserPo gu=new GroupUserPo();
        gu.setId(SequenceUUID.getUUIDSubSegment(4));
        gu.setGroupId(g.getGroupId());
        gu.setUserId(u.getUserId());
        gu.setInviter(g.getCreateUserId());
        int i=0;
        try {
            groupDao.insert("insertGroupUser", gu);
            i=1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * 更新用户
     * @param user 用户信息
     * @return 更新用户成功返回1，否则返回0
     */
    public int updateUser(GroupPo group) {
        int i=0;
        try {
            groupDao.update(group);
            i=1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * 根据用户ID,得到用户组
     * @param userId
     * @return 用户组
     */
    public List<GroupPo> getGroupsByUserId(String userId) {
        try {
            return groupDao.queryForList("getGroupListByUserId", userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获得用户组中的用户
     * @param groupId 用户组Id
     * @return 用户组中的用户
     */
    public List<UserPo> getGroupMembers(String groupId) {
        try {
            return userDao.queryForList("getGroupMembers", groupId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}