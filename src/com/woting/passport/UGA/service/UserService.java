package com.woting.passport.UGA.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.UGA.UgaUserService;
import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.woting.passport.thirdlogin.ThirdLoginUtils;
import com.woting.passport.thirdlogin.persis.po.ThirdUserPo;

public class UserService implements UgaUserService {
    @Resource(name="defaultDAO")
    private MybatisDAO<UserPo> userDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<ThirdUserPo> thirdUserDao;

    @PostConstruct
    public void initParam() {
        userDao.setNamespace("WT_USER");
        thirdUserDao.setNamespace("THIRDUSRE");
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

    /**
     * 根据绑定手机号，获得用户信息
     * @param userNum 用户号码
     * @return 用户信息
     */
    public UserPo getUserByPhoneNum(String phoneNum) {
        try {
            return userDao.getInfoObject("getUserByPhoneNum", phoneNum);
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
     * @return 更新用户成功返回1，否则返回0，返回2说明用户号码重复
     */
    public int updateUser(UserPo user) {
        int i=0;
        try {
            //看userNum是否重复
            boolean existUserNum=false;
            if (!StringUtils.isNullOrEmptyOrSpace(user.getUserNum())) {
                UserPo _u=userDao.getInfoObject("getUserByNum", user.getUserNum());
                if (_u!=null) existUserNum=true;
            }
            if (existUserNum) user.setUserNum(null);
            userDao.update(user);
            return existUserNum?2:1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
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

    /**
     * 根据用户号码获得用户信息
     * @param userNum 用户号码
     * @return 用户信息
     */
    public UserPo getUserByNum(String userNum) {
        return userDao.getInfoObject("getUserByNum", userNum);
    }

    /**
     * 第三方登录
     * @param thirdType 第三方登录类型
     * @param tuserId 第三方中的用户唯一标识
     * @param tuserName 第三方中的用户名称
     * @param tuserImg 第三方用户的头像
     * @param tuserData 第三方用户的其他信息
     * @return Map 当成功则有如下内容：
     *   count——登录的次数
     *   userInfo——用户信息
     */
    public Map<String, Object> thirdLogin(String thirdType, String tuserId, String tuserName, String tuserImg, Map<String, Object> tuserData) {
        Map<String, Object> r=new HashMap<String, Object>();
        //查看是否已经存在了
        ThirdUserPo tuPo=thirdUserDao.getInfoObject("getInfoByThirdUserId", tuserId);
        UserPo uPo=null;

        if (tuPo==null) {
            //若没有存在：1-加入新的用户
            uPo=new UserPo();
            uPo.setUserId(SequenceUUID.getUUIDSubSegment(4));
            uPo.setLoginName(tuserName);
            uPo.setUserState(1);
            uPo.setUserType(1);
            uPo.setPortraitBig(tuserImg);
            uPo.setPortraitMini(tuserImg);
            uPo.setUserName(tuserName);
            uPo.setPassword("--"+uPo.getUserId());
            ThirdLoginUtils.fillUserInfo(uPo, thirdType, tuserData);
            //若没有存在：2-建立关联关系
            tuPo=new ThirdUserPo();
            tuPo.setId(SequenceUUID.getUUIDSubSegment(4));
            tuPo.setUserId(uPo.getUserId());
            tuPo.setThirdUserId(tuserId);
            tuPo.setThirdLoginType(thirdType);
            tuPo.setThirdLoginCount(1);
            tuPo.setThirdUserInfo(JsonUtils.objToJson(tuserData));
            //保存到数据库
            thirdUserDao.insert(tuPo);
            this.insertUser(uPo);
        } else {
            //若存在：加一次登录数
            Map<String, Object> paramM=new HashMap<String, Object>();
            paramM.put("id", tuPo.getId());
            tuPo.setThirdLoginCount(tuPo.getThirdLoginCount()+1);
            paramM.put("thirdLoginCount", tuPo.getThirdLoginCount());
            thirdUserDao.update(paramM);
            uPo=userDao.getInfoObject("getUserById", tuPo.getUserId());
        }
        if (uPo!=null) r.put("userInfo", uPo.toHashMap4Mobile());
        if (tuPo!=null) r.put("count", tuPo.getThirdLoginCount());
       
        return r;
    }
}