package com.woting.passport.UGA.service;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.UGA.UgaUserService;
import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.passport.UGA.persistence.pojo.UserPo;

public class UserService implements UgaUserService {
    @Resource(name="defaultDAO")
    private MybatisDAO<UserPo> userDao;

    @PostConstruct
    public void initParam() {
        userDao.setNamespace("WT_USER");
    }

    @Override
    @SuppressWarnings("unchecked")
    public UserPo getUserByLoginName(String loginName) {
        try {
            return userDao.getInfoObject("getUserByLoginName", loginName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public UserPo getUserById(String userId) {
        try {
            return userDao.getInfoObject("getUserById", userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 创建用户
     * @param user 用户信息
     * @return 创建用户成功返回1，否则返回0
     */
    public int insertUser(UserPo user) {
        int i=0;
        try {
            if (StringUtils.isNullOrEmptyOrSpace(user.getUserId())) {
                user.setUserId(SequenceUUID.getUUIDSubSegment(4));
            }
            userDao.insert("insertUser", user);
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
    public int updateUser(UserPo user) {
        int i=0;
        try {
            userDao.update(user);
            i=1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * 获得好友列表
     * @param userId 用户id
     * @return 好友列表
     */
    public List<UserPo> getFriendList(String userId) {
        try {
            List<UserPo> ul=userDao.queryForList();
            return ul;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获得组成员，为创建用户组使用
     * @param Members 组成员id，用逗号隔开
     * @return 组成员类表
     */
    public List<UserPo> getMembers4BuildGroup(String members) {
        try {
            List<UserPo> ul=userDao.queryForList("getMembers", members);
            return ul;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}