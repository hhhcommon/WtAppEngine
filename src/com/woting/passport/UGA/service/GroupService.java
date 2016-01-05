package com.woting.passport.UGA.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.passport.UGA.model.Group;
import com.woting.passport.UGA.persistence.pojo.GroupPo;
import com.woting.passport.UGA.persistence.pojo.GroupUserPo;
import com.woting.passport.UGA.persistence.pojo.UserPo;

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
    public int insertGroup(Group group) {
        int i=0;
        try {
            group.setGroupId(SequenceUUID.getUUIDSubSegment(4));
            groupDao.insert(group);
            //插入用户
            List<UserPo> ul = group.getUserList();
            if (ul!=null&&ul.size()>0) {
                for (UserPo u: ul) this.insertGroupUser((GroupPo)group.convert2Po(), u, 0);
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
    public int insertGroupUser(GroupPo g, UserPo u, int isSelfIn) {
        GroupUserPo gu=new GroupUserPo();
        gu.setId(SequenceUUID.getUUIDSubSegment(4));
        gu.setGroupId(g.getGroupId());
        gu.setUserId(u.getUserId());
        gu.setInviter(isSelfIn==1?u.getUserId():g.getCreateUserId());
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
     * 根据用户ID,得到用户组
     * @param userId
     * @return 用户组
     */
    public List<GroupPo> getCreateGroupsByUserId(String userId) {
        try {
            return groupDao.queryForList("getCreateGroupListByUserId", userId);
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

    /**
     * 得到所有用户组信息，返回的用户组列表中的元素是用户组模型，包括该组下的用户。
     * 此方法目前用于初始化对讲用户组内存数据库。
     * @return
     */
    public List<Group> getAllGroup() {
        Map<String, String> param = new HashMap<String, String>();
        param.put("orderByClause", "id");
        List<GroupPo> gl = this.groupDao.queryForList(param);
        if (gl!=null&&gl.size()>0) {
            List<Group> ret = new ArrayList<Group>();
            Group item = null;
            List<Map<String, Object>> ul = this.userDao.queryForListAutoTranform("getListUserInGroup", null);
            if (ul!=null&&ul.size()>0) {
                int i=0;
                Map<String, Object> up=ul.get(i);
                for (GroupPo gp: gl) {
                    item=new Group();
                    item.buildFromPo(gp);
                    if (i<ul.size()) {
                        while (((String)up.get("groupId")).equals(gp.getGroupId())) {
                            UserPo _up = new UserPo();
                            _up.fromHashMap(up);
                            _up.setUserId((String)up.get("id"));
                            item.addOneUser(_up);
                            if (++i==ul.size()) break;
                            up=ul.get(i);
                        }
                    }
                    ret.add(item);
                }
            } else {
                for (GroupPo gp: gl) {
                    item=new Group();
                    item.buildFromPo(gp);
                    ret.add(item);
                }
            }
            return ret;
        }
        return null;
    }

    //以下为号码组================================
    /**
     * 获得我所创建的用户组的个数
     * @param userId 用户Id
     * @return 所创建的用户组数量
     */
    public int getCreateGroupCount(String userId) {
        try {
            return groupDao.getCount("getCreateGroupCount", userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获得我所创建的用户组的个数
     * @param userId 用户Id
     * @param lastTimeMinutes 最近的分钟数量
     * @return 所创建的用户组数量
     */
    public int getCreateGroupLimitTimeCount(String userId, int lastTimeMinutes) {
        try {
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("userId", userId);
            param.put("lastTimeMinutes", new Timestamp(System.currentTimeMillis()-(lastTimeMinutes*1000*60)));
            return groupDao.getCount("getCreateGroupLimitTimeCount", param);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 判断所给定的组号是否重复
     * @param groupNum 组号
     * @return 根据此组号查出的组的个数
     */
    public int existGroupNum(String groupNum) {
        try {
            return groupDao.getCount("existGroupNum", groupNum);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public GroupPo getGroup(Map<String, Object> m) {
        try {
            return groupDao.getInfoObject(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int existUserInGroup(String groupId, String userId) {
        try {
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("userId", userId);
            param.put("groupId", groupId);
            return groupDao.getCount("existUserInGroup", param);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}