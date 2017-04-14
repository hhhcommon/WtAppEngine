package com.woting.passport.UGA.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.core.model.Page;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.push.core.message.MsgNormal;
import com.woting.push.core.message.content.MapContent;
import com.woting.push.socketclient.oio.SocketClient;
import com.woting.WtAppEngineConstants;
import com.woting.cm.core.dict.mem._CacheDictionary;
import com.woting.cm.core.dict.model.DictModel;
import com.woting.passport.UGA.model.Group;
import com.woting.passport.UGA.persis.pojo.GroupPo;
import com.woting.passport.UGA.persis.pojo.GroupUserPo;
import com.woting.passport.UGA.persis.pojo.UserPo;
import com.woting.passport.groupinvite.persis.pojo.InviteGroupPo;
import com.woting.passport.session.SessionService;
import com.woting.passport.useralias.persis.pojo.UserAliasPo;
import com.woting.passport.useralias.service.UserAliasService;

/**
 * 用户组处理，包括创建组，查询组；组邀请等信息
 * @author wanghui
 */
@Service
public class GroupService {
    @Resource(name="defaultDAO")
    private MybatisDAO<UserPo> userDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<GroupPo> groupDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<InviteGroupPo> inviteGroupDao;
    @Resource
    private UserAliasService userAliasService;
    @Resource(name="redisSessionService")
    private SessionService sessionService;

    private _CacheDictionary _cd=null;

    @SuppressWarnings("unchecked")
    @PostConstruct
    public void initParam() {
        userDao.setNamespace("WT_USER");
        groupDao.setNamespace("WT_GROUP");
        inviteGroupDao.setNamespace("WT_GROUPINVITE");
        _cd=(SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)==null?null:((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)).getContent());
    }

    /**
     * 创建用户组
     * @param group 用户组信息
     * @return 创建用户成功返回1，否则返回0
     */
    @SuppressWarnings("unchecked")
    public int insertGroup(Group group) {
        int i=0;
        try {
            group.setGroupId(SequenceUUID.getUUIDSubSegment(4));
            if (StringUtils.isNullOrEmptyOrSpace(group.getDefaultFreq())) {
                String df="409.7500,409.7625";
                if (_cd==null) _cd=(SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)==null?null:((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT)).getContent());
                if (_cd!=null) {
                    DictModel dm=_cd.getDictModelById("11");
                    if (dm!=null&&dm.dictTree!=null) {
                        TreeNode<? extends TreeNodeBean> root=dm.dictTree;
                        if (root.getChildren()!=null&&!root.getChildren().isEmpty()) {
                            df="";
                            for (int j=0; j<root.getChildren().size()&&j<2; j++) {
                                df+=","+root.getChildren().get(j).getAttributes().get("aliasName");
                            }
                            df=df.substring(1);
                        }
                    }
                }
                group.setDefaultFreq(df);
            }
            groupDao.insert(group);
            //插入用户组所属用户
            List<UserPo> ul=group.getUserList();
            if (ul!=null&&ul.size()>0) {
                for (UserPo u: ul) insertGroupUser((GroupPo)group.convert2Po(), u, 0, false, group.getCreateUserId());
            }
            group.setCTime(new Timestamp(System.currentTimeMillis()));

            SocketClient sc=((CacheEle<SocketClient>)SystemCache.getCache(WtAppEngineConstants.SOCKET_OBJ)).getContent();
            if (sc!=null) {
                //通知消息
                MsgNormal nMsg=new MsgNormal();
                nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                nMsg.setFromType(1);
                nMsg.setToType(1);
                nMsg.setMsgType(0);
                nMsg.setAffirm(1);
                nMsg.setBizType(0x04);
                nMsg.setCmdType(2);
                nMsg.setCommand(10);//组创建通知
                Map<String, Object> dataMap=new HashMap<String, Object>();
                dataMap.put("GroupInfo", group.toHashMap4View());
                dataMap.put("OperatorId", group.getCreateUserId());
                MapContent mc=new MapContent(dataMap);
                nMsg.setMsgContent(mc);

                dataMap.put("_TOGROUPS", group.getGroupId());
                dataMap.put("_NOUSERS", group.getCreateUserId());
                dataMap.put("_AFFIRMTYPE", "3");
                sc.addSendMsg(nMsg);
            }
            i=1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * 创建用户用户组关系，并向组内在线人广播消息
     * @param g 用户组
     * @param u 用户
     */
    public int insertGroupUser(GroupPo g, UserPo u, int isSelfIn, boolean isMsg, String operId) {
        GroupUserPo gu=new GroupUserPo();
        gu.setId(SequenceUUID.getUUIDSubSegment(4));
        gu.setGroupId(g.getGroupId());
        gu.setUserId(u.getUserId());
        gu.setInviter(isSelfIn==1?u.getUserId():g.getCreateUserId());
        gu.setGroupAlias(g.getGroupName());
        gu.setGroupDescn(g.getDescn());
        int i=0;
        try {
            groupDao.insert("insertGroupUser", gu);
            i=1;
            if (isMsg) {
                @SuppressWarnings("unchecked")
                SocketClient sc=((CacheEle<SocketClient>)SystemCache.getCache(WtAppEngineConstants.SOCKET_OBJ)).getContent();
                if (sc!=null) {
                    //同步消息：加入组内成员
                    MsgNormal sMsg=new MsgNormal();
                    sMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                    sMsg.setFromType(1);
                    sMsg.setToType(1);
                    sMsg.setMsgType(0);
                    sMsg.setAffirm(1);
                    sMsg.setBizType(0x08);
                    sMsg.setCmdType(2);//组
                    sMsg.setCommand(4);//加入成员
                    Map<String, Object> dataMap=new HashMap<String, Object>();
                    dataMap.put("GroupId", g.getGroupId());
                    dataMap.put("UserInfo", u.toHashMap4Mobile());
                    MapContent mc=new MapContent(dataMap);
                    sMsg.setMsgContent(mc);
                    sc.addSendMsg(sMsg);

                    //通知消息
                    MsgNormal nMsg=new MsgNormal();
                    nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                    nMsg.setFromType(1);
                    nMsg.setToType(1);
                    nMsg.setMsgType(0);
                    nMsg.setAffirm(1);
                    nMsg.setBizType(0x04);
                    nMsg.setCmdType(2);
                    nMsg.setCommand(4);
                    Map<String, Object> dataMap1=new HashMap<String, Object>();
                    dataMap1.put("GroupId", g.getGroupId());
                    dataMap1.put("OperatorId", operId);
                    dataMap1.put("UserInfo", u.toHashMap4Mobile());
                    MapContent mc1=new MapContent(dataMap1);
                    nMsg.setMsgContent(mc1);

                    dataMap1.put("_TOGROUPS", g.getGroupId());
                    dataMap1.put("_NOUSERS", u.getUserId());
                    dataMap.put("_AFFIRMTYPE", "3");
                    sc.addSendMsg(nMsg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * 根据用户ID,得到用户组
     * @param userId 我的用户Id
     * @param pageSize 每页有几条记录
     * @param pageIndex 页码，若为0,则得到所有内容
     * @return 用户组
     */
    public List<GroupPo> getGroupsByUserId(String userId, int pageSize, int pageIndex) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return null;

        List<GroupPo> ret=null;
        if (pageIndex==0) ret=groupDao.queryForList("getGroupListByUserId", userId);
        else {
            Page<GroupPo> page=groupDao.pageQuery(null, "getGroupListByUserId", userId, pageIndex, pageSize);
            if (page!=null&&page.getDataCount()>0) {
                ret=new ArrayList<GroupPo>();
                ret.addAll(page.getResult());
            }
        }
        return (ret==null||ret.isEmpty())?null:ret;
    }

    /**
     * 根据用户ID,得到用户所创建的组
     * @param userId
     * @param pageSize 每页有几条记录
     * @param pageIndex 页码，若为0,则得到所有内容
     * @return 用户组
     */
    public List<GroupPo> getCreateGroupsByUserId(String userId, int pageSize, int pageIndex) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return null;

        List<GroupPo> ret=null;
        if (pageIndex==0) ret=groupDao.queryForList("getCreateGroupListByUserId", userId);
        else {
            Page<GroupPo> page=groupDao.pageQuery(null, "getCreateGroupListByUserId", userId, pageIndex, pageSize);
            if (page!=null&&page.getDataCount()>0) {
                ret=new ArrayList<GroupPo>();
                ret.addAll(page.getResult());
            }
        }
        return (ret==null||ret.isEmpty())?null:ret;
    }

    /**
     * 根据用户ID,得到用户能管理的组
     * @param userId
     * @param pageSize 每页有几条记录
     * @param pageIndex 页码，若为0,则得到所有内容
     * @return 用户组
     */
    public List<GroupPo> getManageGroupsByUserId(String userId, int pageSize, int pageIndex) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return null;

        List<GroupPo> ret=null;
        if (pageIndex==0) ret=groupDao.queryForList("getManageGroupListByUserId", userId);
        else {
            Page<GroupPo> page=groupDao.pageQuery(null, "getManageGroupListByUserId", userId, pageIndex, pageSize);
            if (page!=null&&page.getDataCount()>0) {
                ret=new ArrayList<GroupPo>();
                ret.addAll(page.getResult());
            }
        }
        return (ret==null||ret.isEmpty())?null:ret;
    }

    /**
     * 根据用户ID,得到用户能管理的组
     * @param userId
     * @param pageSize 每页有几条记录
     * @param pageIndex 页码，若为0,则得到所有内容
     * @return 用户组
     */
    public List<GroupPo> getMasterGroupsByUserId(String userId, int pageSize, int pageIndex) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return null;

        List<GroupPo> ret=null;
        if (pageIndex==0) ret=groupDao.queryForList("getMasterGroupListByUserId", userId);
        else {
            Page<GroupPo> page=groupDao.pageQuery(null, "getMasterGroupListByUserId", userId, pageIndex, pageSize);
            if (page!=null&&page.getDataCount()>0) {
                ret=new ArrayList<GroupPo>();
                ret.addAll(page.getResult());
            }
        }
        return (ret==null||ret.isEmpty())?null:ret;
    }

    /**
     * 获得用户组中的用户
     * @param groupId 用户组Id
     * @param pageSize 每页有几条记录
     * @param pageIndex 页码，若为0,则得到所有内容
     * @return 用户组中的用户
     */
    public List<UserPo> getGroupMembers(String groupId, int pageSize, int pageIndex) {
        if (StringUtils.isNullOrEmptyOrSpace(groupId)) return null;

        List<UserPo> ret=null;
        if (pageIndex==0) ret=userDao.queryForList("getGroupMembers", groupId);
        else {
            Page<UserPo> page=userDao.pageQuery(null, "getGroupMembers", groupId, pageIndex, pageSize);
            if (page!=null&&page.getDataCount()>0) {
                ret=new ArrayList<UserPo>();
                ret.addAll(page.getResult());
            }
        }
        return (ret==null||ret.isEmpty())?null:ret;
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
     * 获得我所创建的用户组在最近个数
     * @param userId 用户Id
     * @param lastTimeMinutes 最近的分钟数量
     * @return 所创建的用户组数量
     */
    public int getCreateGroupLimitTimeCount(String userId, int lastTimeMinutes) {
        try {
            Map<String, Object> param=new HashMap<String, Object>();
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

    public GroupPo getGroupById(String groupId) {
        try {
            return groupDao.getInfoObject("getGroupById", groupId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 是否组中存在该用户
     * @param groupId 组Id
     * @param userId 用户Id
     * @return 0不存在，其他存在
     */
    public int existUserInGroup(String groupId, String userId) {
        int c=0;
        try {
            List<UserPo> ul=userDao.queryForList("getGroupMembers", groupId);
            if (ul!=null&&!ul.isEmpty()) {
                for (UserPo up: ul) {
                    if (up.getUserId().equals(userId)) {
                        c=1;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    /**
     * 更新用户的信息，不对对讲组内存结构进行广播，单要修改
     * @param user 用户信息
     * @return 更新用户成功返回1，否则返回0
     */
    public int updateGroup(GroupPo g) {
        int i=0;
        try {
            groupDao.update(g);
            i=1;
            //通知组内成员，组信息进行了修改
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }
    /**
     * 更新用户的信息
     * @param newInfo 所修改的新信息
     * @param userId 修改者id
     * @param g 所修改的组对象
     * @return 更新用户成功返回1，否则返回0
     */
    public void updateGroup(Map<String, Object> newInfo, String userId, GroupPo g) {
        boolean changed=false;
        if (g.getAdminUserIds().equals(userId)) { //修改组本身信息
            if (newInfo.get("groupDescn")!=null) g.setDescn(newInfo.get("groupDescn")+"");
            if (newInfo.get("groupName")!=null) g.setGroupName(newInfo.get("groupName")+"");
            if (newInfo.get("groupSignature")!=null) g.setGroupSignature(newInfo.get("groupSignature")+"");
            if (newInfo.get("groupFreq")!=null) g.setDefaultFreq(newInfo.get("groupFreq")+"");
            this.updateGroup(g);
            changed=true;
        }

//        if (newInfo.get("groupName")!=null) newInfo.put("groupAlias", newInfo.get("groupName"));//用户名修改了不修改别名

        if (newInfo.get("groupAlias")!=null||newInfo.get("groupDescn")!=null) {
            newInfo.put("groupId", g.getGroupId());
            newInfo.put("userId", userId);
            groupDao.update("updateGroupUserByUserIdGroupId", newInfo);
        }
        if (!changed) return ;

        @SuppressWarnings("unchecked")
        SocketClient sc=((CacheEle<SocketClient>)SystemCache.getCache(WtAppEngineConstants.SOCKET_OBJ)).getContent();
        if (sc!=null) {
            //同步消息：组信息修改
            MsgNormal sMsg=new MsgNormal();
            sMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            sMsg.setFromType(1);
            sMsg.setToType(1);
            sMsg.setMsgType(0);
            sMsg.setAffirm(1);
            sMsg.setBizType(0x08);
            sMsg.setCmdType(2);//组
            sMsg.setCommand(2);//更改
            Map<String, Object> dataMap1=new HashMap<String, Object>();
            dataMap1.put("GroupId", g.getGroupId());
            MapContent mc1=new MapContent(dataMap1);
            sMsg.setMsgContent(mc1);
            sc.addSendMsg(sMsg);

            //通知消息
            MsgNormal nMsg=new MsgNormal();
            nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            nMsg.setFromType(1);
            nMsg.setToType(1);
            nMsg.setMsgType(0);
            nMsg.setAffirm(1);
            nMsg.setBizType(0x04);
            nMsg.setCmdType(2);
            nMsg.setCommand(9);
            Map<String, Object> dataMap=new HashMap<String, Object>();
//            Map<String, Object> gMap=new HashMap<String, Object>();
//            gMap.put("GroupId", g.getGroupId());
//            gMap.put("GroupNum", g.getGroupNum());
//            gMap.put("GroupType", g.getGroupType());
//            if (!StringUtils.isNullOrEmptyOrSpace(g.getGroupSignature())) gMap.put("GroupSignature", g.getGroupSignature());
//            if (!StringUtils.isNullOrEmptyOrSpace(g.getGroupImg())) gMap.put("GroupImg", g.getGroupImg());
//            gMap.put("GroupName", g.getGroupName());
//            if (!StringUtils.isNullOrEmptyOrSpace(g.getCreateUserId())) gMap.put("GroupCreator", g.getCreateUserId());
//            if (!StringUtils.isNullOrEmptyOrSpace((String)g.getAdminUserIds())) gMap.put("GroupManager", g.getAdminUserIds());
//            gMap.put("GroupCount", g.getGroupCount());
//            if (!StringUtils.isNullOrEmptyOrSpace((String)g.getDescn())) gMap.put("GroupOriDescn", g.getDescn());
            dataMap.put("GroupInfo", g.toHashMap4View());
            dataMap.put("OperatorId", userId);
            MapContent mc=new MapContent(dataMap);
            nMsg.setMsgContent(mc);

            dataMap.put("_TOGROUPS", g.getGroupId());
            dataMap.put("_AFFIRMTYPE", "0");//不需要任何回复
            sc.addSendMsg(nMsg);
        }
    }

    /**
     * 退出用户组，并广播组消息。
     * 当用户是管理员时，用户组的管理者自动变为最先进入组的用户。
     * 当用户只剩下一个时，自动删除组
     * @param gp 相关的组
     * @param user 退出的用户
     * @return 0用户不在组，1退出组，2退出组并删除组
     */
    public int exitUserFromGroup(GroupPo gp, UserPo u) {
        String groupId=gp.getGroupId();
        List<UserPo> ul=userDao.queryForList("getGroupMembers", groupId);
        int i=-1;
        if (ul!=null&&!ul.isEmpty()) {
            for (i=0;i<ul.size(); i++) {
                UserPo up=ul.get(i);
                if (up.getUserId().equals(u.getUserId())) break;
            }
        }
        if (i==-1||i==ul.size()) return 0;
 
        ul.remove(i);
        //删除组内用户
        Map<String, String> param=new HashMap<String, String>();
        param.put("userId", u.getUserId());
        param.put("groupId", groupId);
        groupDao.delete("deleteGroupUser", param);
        //删除某一用户群组下，某一用户的别名
        userAliasService.delUserAliasInGroup(groupId, u.getUserId());

        int r=1;
        //删除组
        if (ul.size()<=1) {
            boolean canDissolve=true;
            if (ul.size()==1) {
                if (ul.get(0).getUserId().equals(gp.getCreateUserId())) canDissolve=false;
            }
            if (canDissolve) {
                //删除所有的组内人员信息
                param.clear();
                param.put("groupId", groupId);
                groupDao.delete("deleteGroupUser", param);
                //删除组
                groupDao.delete(gp.getGroupId());
                //处理组邀请信息表，把flag设置为2
                inviteGroupDao.update("setFlag2", groupId);
                //删除组内所有成员的别名
                userAliasService.delAliasInGroup(groupId);
                r=2;
            }
        } else {
            if (u.getUserId().equals(gp.getAdminUserIds())) {
                List<Object> gupl=(List<Object>)groupDao.queryForListAutoTranform("getGroupUserByGroupId", groupId);
                //移交管理员
                GroupUserPo gup=(GroupUserPo)gupl.get(0);
                for (i=1; i<gupl.size(); i++) {
                    GroupUserPo _gup=(GroupUserPo)gupl.get(i);
                    if (gup.getCTime().after(_gup.getCTime())) gup=_gup;
                }
                gp.setAdminUserIds(gup.getUserId());
                groupDao.update(gp);
                r=3;
            }
        }

        @SuppressWarnings("unchecked")
        SocketClient sc=((CacheEle<SocketClient>)SystemCache.getCache(WtAppEngineConstants.SOCKET_OBJ)).getContent();
        if (sc!=null) {
            if (r==3||r==1) {//删除组用户
                //同步消息：删除组用户
                MsgNormal sMsg=new MsgNormal();
                sMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                sMsg.setFromType(1);
                sMsg.setToType(1);
                sMsg.setMsgType(0);
                sMsg.setAffirm(1);
                sMsg.setBizType(0x08);
                sMsg.setCmdType(2);//组
                sMsg.setCommand(5);//删除组内用户
                Map<String, Object> dataMap=new HashMap<String, Object>();
                dataMap.put("GroupId", gp.getGroupId());
                dataMap.put("UserId", u.getUserId());
                dataMap.put("OperatorId", u.getUserId());
                MapContent mc=new MapContent(dataMap);
                sMsg.setMsgContent(mc);
                sc.addSendMsg(sMsg);

                //通知消息：删除组用户
                MsgNormal nMsg=new MsgNormal();
                nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                nMsg.setFromType(1);
                nMsg.setToType(1);
                nMsg.setMsgType(0);
                nMsg.setAffirm(1);
                nMsg.setBizType(0x04);
                nMsg.setCmdType(2);
                nMsg.setCommand(5);
                Map<String, Object> dataMap1=new HashMap<String, Object>();
                dataMap1.put("GroupId", gp.getGroupId());
                dataMap1.put("UserInfo", u.toHashMap4Mobile());
                MapContent mc1=new MapContent(dataMap1);
                nMsg.setMsgContent(mc1);

                dataMap1.put("_TOGROUPS", gp.getGroupId());
                dataMap.put("_AFFIRMTYPE", "3");
                sc.addSendMsg(nMsg);
            }
            if (r==3) {//更改用户
                //同步消息：组信息修改
                MsgNormal sMsg=new MsgNormal();
                sMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                sMsg.setFromType(1);
                sMsg.setToType(1);
                sMsg.setMsgType(0);
                sMsg.setAffirm(1);
                sMsg.setBizType(0x08);
                sMsg.setCmdType(2);//组
                sMsg.setCommand(2);//更改
                Map<String, Object> dataMap=new HashMap<String, Object>();
                dataMap.put("GroupId", gp.getGroupId());
                MapContent mc=new MapContent(dataMap);
                sMsg.setMsgContent(mc);
                sc.addSendMsg(sMsg);

                //通知消息：组信息修改
                MsgNormal nMsg=new MsgNormal();
                nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                nMsg.setFromType(1);
                nMsg.setToType(1);
                nMsg.setMsgType(0);
                nMsg.setAffirm(1);
                nMsg.setBizType(0x04);
                nMsg.setCmdType(2);
                nMsg.setCommand(9);
                Map<String, Object> dataMap1=new HashMap<String, Object>();
                dataMap1.put("GroupInfo", gp.toHashMap4View());
                MapContent mc1=new MapContent(dataMap1);
                nMsg.setMsgContent(mc1);

                dataMap1.put("_TOGROUPS", gp.getGroupId());
                dataMap1.put("_NOUSERS", u.getUserId());
                dataMap.put("_AFFIRMTYPE", "0");//不需要任何回复
                sc.addSendMsg(nMsg);
            }
            if (r==2) {//删除用户组
                //同步消息：组信息修改
                MsgNormal sMsg=new MsgNormal();
                sMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                sMsg.setFromType(1);
                sMsg.setToType(1);
                sMsg.setMsgType(0);
                sMsg.setAffirm(1);
                sMsg.setBizType(0x08);
                sMsg.setCmdType(2);//组
                sMsg.setCommand(3);//删除
                Map<String, Object> dataMap=new HashMap<String, Object>();
                dataMap.put("GroupId", gp.getGroupId());
                MapContent mc=new MapContent(dataMap);
                sMsg.setMsgContent(mc);
                sc.addSendMsg(sMsg);

                //通知消息：组信息修改
                String toUser="";
                if (ul!=null&&!ul.isEmpty()) {
                    for (i=0;i<ul.size(); i++) {
                        toUser+=","+ul.get(i).getUserId();
                    }
                }
                if (!StringUtils.isNullOrEmptyOrSpace(toUser)) {
                    MsgNormal nMsg=new MsgNormal();
                    nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                    nMsg.setFromType(1);
                    nMsg.setToType(1);
                    nMsg.setMsgType(0);
                    nMsg.setAffirm(1);
                    nMsg.setBizType(0x04);
                    nMsg.setCmdType(2);
                    nMsg.setCommand(6);//删除组，或解散组
                    Map<String, Object> dataMap1=new HashMap<String, Object>();
                    dataMap1.put("GroupId", gp.getGroupId());
                    dataMap1.put("OperatorId", u.getUserId());
                    dataMap1.put("DelReason", "2");//=2因为退组而删除组;=1直接删除组
                    MapContent mc1=new MapContent(dataMap1);
                    nMsg.setMsgContent(mc1);

                    dataMap1.put("_TOUSERS", toUser.substring(1));
                    dataMap.put("_AFFIRMTYPE", "3");
                    sc.addSendMsg(nMsg);
                }
            }
        }
        return r==2?2:1;
    }

    /**
     * 用户邀请
     * @param userId 邀请用户
     * @param invitedUserIds 被邀请用户，以逗号隔开
     * @param groupId 用户组Id
     * @param inviteMsg 邀请信息
     * @param isManager 是否是管理员
     * @return
     */
    public Map<String, Object> inviteGroup(String userId, String beInvitedUserIds, String groupId, String inviteMsg, int isManager) {
        Map<String, Object> m=new HashMap<String, Object>();

        //1、判断邀请人是否在组
        List<UserPo> gul=getGroupMembers(groupId, 0, 0);//获得所有组成员
        boolean find=false; //邀请者是否在组
        if (gul!=null&&!gul.isEmpty()) {
            for (UserPo up: gul) {
                if (up.getUserId().equals(userId)) {
                    find=true;
                    break;
                }
            }
        }

        if (!find) {
            m.put("ReturnType", "1006");
            m.put("RefuseMsg", "邀请人不在用户组");
        } else {
            GroupPo gp=getGroupById(groupId);
            m.put("ReturnType", "1001");
            Map<String, Object> resultM=new HashMap<String, Object>();
            m.put("Result", resultM);
            resultM.put("GroupId", groupId);

            Map<String, Object> param=new HashMap<String, Object>();
            param.put("aUserId", userId);
            param.put("groupId", groupId);
            List<InviteGroupPo> igl=inviteGroupDao.queryForList("getInvitingGroupList", param);//我的邀请列表：所有人，我邀请进入这个组的其他人，包括所有的历史

            List<Map<String, String>> resultList=new ArrayList<Map<String, String>>();
            resultM.put("ResultList", resultList);

            InviteGroupPo igp=null;
            long inviteTime=System.currentTimeMillis();
            String[] ua=beInvitedUserIds.split(",");
            for (String beInvitedUserId: ua) {
                String _beInvitedUserId=beInvitedUserId.trim();
                Map<String, String> oneResult=new HashMap<String, String>();
                oneResult.put("UserId", _beInvitedUserId);
                find=false;
                //是否已在用户组
                if (gul!=null&&!gul.isEmpty()) {
                    for (UserPo up: gul) {
                        if (up.getUserId().equals(_beInvitedUserId)) {
                            find=true;
                            break;
                        }
                    }
                }
                if (find) oneResult.put("InviteCount", "-1");
                else {
                    igp=null;
                    if (igl!=null&&!igl.isEmpty()) {
                        for (InviteGroupPo _igp: igl) {
                            if (_igp.getbUserId().equals(_beInvitedUserId)) {
                                igp=_igp;
                                break;
                            }
                        }
                    }
                    if (igp!=null) {//注意：在邀请表中，一个人可以邀请另一个人多次，但表中只记录最新的一次
                        param.clear();
                        param.put("id", igp.getId());
                        param.put("isManager", isManager);
                        inviteGroupDao.update("againInvite", param);
                        oneResult.put("InviteCount", (igp.getInviteVector()+1)+"");
                    } else {
                        igp= new InviteGroupPo();
                        igp.setId(SequenceUUID.getUUIDSubSegment(4));
                        igp.setaUserId(userId);
                        igp.setbUserId(_beInvitedUserId);
                        igp.setGroupId(groupId);
                        igp.setInviteMessage(inviteMsg);
                        igp.setInviteVector(1);
                        igp.setManagerFlag(0);
                        //如果是管理员所邀请的，就不用再被其他管理员审核了
                        if (isManager==1) igp.setManagerFlag(1);
                        inviteGroupDao.insert(igp);
                        oneResult.put("InviteCount", "1");

                        if (gp.getGroupType()!=0) {//不是验证群，直接发给被邀请者
                            @SuppressWarnings("unchecked")
                            SocketClient sc=((CacheEle<SocketClient>)SystemCache.getCache(WtAppEngineConstants.SOCKET_OBJ)).getContent();
                            if (sc!=null) {
                                MsgNormal nMsg=new MsgNormal();
                                nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                                nMsg.setFromType(1);
                                nMsg.setToType(1);
                                nMsg.setMsgType(0);
                                nMsg.setAffirm(1);
                                nMsg.setBizType(0x04);
                                nMsg.setCmdType(2);
                                nMsg.setCommand(1);//邀请入组通知

                                Map<String, Object> dataMap=new HashMap<String, Object>();
                                dataMap.put("FriendId", userId);
                                dataMap.put("OperatorId", userId);
                                if (gp!=null) dataMap.put("GroupInfo", gp.toHashMap4View());
                                dataMap.put("InviteTime", inviteTime);
                                MapContent mc=new MapContent(dataMap);
                                nMsg.setMsgContent(mc);

                                dataMap.put("_TOUSERS", _beInvitedUserId);
                                dataMap.put("_AFFIRMTYPE", "3");
                                sc.addSendMsg(nMsg);
                            }
                        }
                    }
                }
                resultList.add(oneResult);
            }

            if (gp.getGroupType()==0&&resultList.size()>0&&isManager!=1) { //验证审核群，需要先发给管理员
                String inviteSuccessUsers="";
                for (int i=0; i<resultList.size(); i++) {
                    Map<String, String> oneResult=resultList.get(i);
                    if (Integer.parseInt(oneResult.get("InviteCount"))>0) {
                        inviteSuccessUsers+=","+oneResult.get("UserId");
                    }
                }
                inviteSuccessUsers=inviteSuccessUsers.length()>0?inviteSuccessUsers.substring(1):inviteSuccessUsers;
                if (inviteSuccessUsers.length()>0) {
                    @SuppressWarnings("unchecked")
                    SocketClient sc=((CacheEle<SocketClient>)SystemCache.getCache(WtAppEngineConstants.SOCKET_OBJ)).getContent();
                    if (sc!=null) {
                        MsgNormal nMsg=new MsgNormal();
                        nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                        nMsg.setFromType(1);
                        nMsg.setToType(1);
                        nMsg.setMsgType(0);
                        nMsg.setAffirm(1);
                        nMsg.setBizType(0x04);
                        nMsg.setCmdType(2);
                        nMsg.setCommand(1);//邀请入组通知

                        Map<String, Object> dataMap=new HashMap<String, Object>();
                        dataMap.put("FriendId", userId);
                        dataMap.put("OperatorId", userId);
                        List<Map<String, Object>> beInvitedUserList=new ArrayList<Map<String, Object>>();
                        ua=beInvitedUserIds.split(",");
                        for (String beInvitedUserId: ua) {
                            UserPo u=userDao.getInfoObject("getUserById", beInvitedUserId);
                            beInvitedUserList.add(u.toHashMap4Mobile());
                        }
                        dataMap.put("BeInvitedUserList", beInvitedUserList);
                        if (gp!=null) dataMap.put("GroupInfo", gp.toHashMap4View());
                        dataMap.put("InviteTime", inviteTime);
                        MapContent mc=new MapContent(dataMap);
                        nMsg.setMsgContent(mc);

                        dataMap.put("_TOUSERS", gp.getAdminUserIds());
                        dataMap.put("_AFFIRMTYPE", "3");
                        sc.addSendMsg(nMsg);
                    }
                }
            }
        }
        return m;
    }

    /**
     * 用户申请
     * @param userId 申请用户
     * @param groupId 用户组Id
     * @param adminId 管理者Id，这个管理者已经在这个组中了
     * @param applyMsg 申请信息
     * @return
     */
    public Map<String, Object> applyGroup(String userId, String groupId, String adminIds, String applyMsg) {
        Map<String, Object> m=new HashMap<String, Object>();
        Map<String, Object> param=new HashMap<String, Object>();
        boolean canContinue=true;

        //1、判断是否已经在组
        param.put("userId", userId);
        param.put("groupId", groupId);
        boolean find=(groupDao.getCount("getUserInGroup", param)>0);
        param.clear();

        if (find) {
            m.put("ReturnType", "1005");
            m.put("RefuseMsg", "申请人已在用户组");
            canContinue=false;
        }

        GroupPo gp=this.getGroupById(groupId);
        //2、判断是否已经申请
        boolean isInDb=false;
        String inDbId=null;
        if (canContinue) {
            param.put("aUserId", gp.getCreateUserId());
            param.put("bUserId", userId);
            param.put("groupId", groupId);
            List<InviteGroupPo> igl=inviteGroupDao.queryForList("getApplyList", param);
            if (igl!=null&&igl.size()>0) {
                isInDb=true;
                for (InviteGroupPo igp: igl) {
                    inDbId=igp.getId();
                    if (igp.getAcceptFlag()==0) {
                        m.put("ReturnType", "1006");
                        m.put("RefuseMsg", "您已申请");
                        m.put("ApplyCount", Math.abs(igp.getInviteVector()-1));
                        inviteGroupDao.update("againApply", igp.getId());
                        canContinue=false;
                        break;
                    }
                }
            }
        }
        if (canContinue) {
            if (isInDb&&!StringUtils.isNullOrEmptyOrSpace(inDbId)) {
                inviteGroupDao.update("againApply", inDbId);
            } else {
                InviteGroupPo igp= new InviteGroupPo();
                igp.setId(SequenceUUID.getUUIDSubSegment(4));
                igp.setaUserId(gp.getCreateUserId());
                igp.setbUserId(userId);
                igp.setGroupId(groupId);
                igp.setInviteMessage(applyMsg);
                igp.setInviteVector(-1);
                inviteGroupDao.insert(igp);
            }
            m.put("ReturnType", "1001");

            //发送通知类消息，给所有的管理员
            @SuppressWarnings("unchecked")
            SocketClient sc=((CacheEle<SocketClient>)SystemCache.getCache(WtAppEngineConstants.SOCKET_OBJ)).getContent();
            if (sc!=null) {
                MsgNormal nMsg=new MsgNormal();
                nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                nMsg.setFromType(1);
                nMsg.setToType(1);
                nMsg.setMsgType(0);
                nMsg.setAffirm(1);
                nMsg.setBizType(0x04);
                nMsg.setCmdType(2);
                nMsg.setCommand(2);//处理组申请信息
                Map<String, Object> dataMap=new HashMap<String, Object>();
                dataMap.put("OperatorId", userId);
                UserPo u=userDao.getInfoObject("getUserById", userId);
                dataMap.put("ApplyUserInfo", u.toHashMap4Mobile());
                dataMap.put("GroupInfo", gp.toHashMap4View());
                dataMap.put("ApplyTime", System.currentTimeMillis());
                MapContent mc=new MapContent(dataMap);
                nMsg.setMsgContent(mc);

                dataMap.put("_TOUSERS", gp.getAdminUserIds());
                dataMap.put("_AFFIRMTYPE", "3");
                sc.addSendMsg(nMsg);
            }
        }
        return m;
    }

    /**
     * 搜索用户组
     * @param searchStr 搜索的字符串
     * @return 用户列表
     */
    public List<Group> searchGroup(String searchStr) {
        List<Group> ret=new ArrayList<Group>();
        Map<String, List<Group>> _tempM=new HashMap<String, List<Group>>();
        int max=0, min=0;
        List<Group> gl=getAllGroup();
        for (Group g: gl) {
            int ss=searchScore(searchStr, g);
            if (ss>0) {
                if (_tempM.get(ss+"")==null) _tempM.put(ss+"", new ArrayList<Group>());
                _tempM.get(ss+"").add(g);
                max=ss>max?ss:max;
                min=ss<min?ss:min;
            }
        }
        for (int i=max; i>=min; i--) {
            if (_tempM.get(i+"")!=null) ret.addAll(_tempM.get(i+""));
        }
        return ret;
    }
    private int searchScore(String searchStr, Group g) {
        int ret=0;
        String s[]=searchStr.split(",");
        for (int i=1; i<=s.length; i++) {
            int wordScore=oneWordScore(s[i-1].trim(), g);
            if (wordScore>0) ret+=wordScore*(1/Math.sqrt(i));
        }
        return ret;
    }
    private int oneWordScore(String oneWord, Group g) {
        int ret=0;
        if (g.getGroupName()!=null&&g.getGroupName().equals(oneWord)) ret+=30;
        if (g.getGroupNum()!=null&&g.getGroupNum().equals(oneWord)) ret+=30;
        if (g.getGroupSignature()!=null&&g.getGroupSignature().equals(oneWord)) ret+=30;
        if (g.getGroupName()!=null&&g.getGroupName().indexOf(oneWord)!=-1) ret+=10;
        if (g.getGroupNum()!=null&&g.getGroupNum().indexOf(oneWord)!=-1) ret+=10;
        if (g.getGroupSignature()!=null&&g.getGroupSignature().indexOf(oneWord)!=-1) ret+=10;
        List<UserPo> ul=g.getUserList();
        UserPo up;
        String userName;
        for (int i=0;i<ul.size(); i++) {
            up=ul.get(i);
            userName=up.getLoginName();
            if (userName!=null) {
                if (userName.equals(oneWord)) ret+=3;
                if (userName.indexOf(oneWord)!=-1) ret+=1;
            }
        }
        return ret;
    }

    /**
     * 获得邀请我的组的列表
     * @param userId
     * @return
     */
    public List<Map<String, Object>> getInviteGroupList(String userId) {
        return inviteGroupDao.queryForListAutoTranform("inviteMeGroupList", userId);
    }

    /**
     * 获得邀请我的组的列表
     * @param userId
     * @return
     */
    public List<Map<String, Object>> getNeedCheckInviteUserGroupList(String groupId) {
        return inviteGroupDao.queryForListAutoTranform("needCheckInviteUserGroupList", groupId);
    }

    /**
     * 获得某用户组的申请人列表信息
     * @param groupId
     * @return
     */
    public List<Map<String, Object>> getApplyUserList(String groupId) {
        return inviteGroupDao.queryForListAutoTranform("applyUserList", groupId);
    }


    /**
     * 得到有未处理申请人的我所管理的用户组
     * @param userId
     * @return
     */
    public List<Map<String, Object>> getExistApplyUserGroupList(String userId) {
        return inviteGroupDao.queryForListAutoTranform("existApplyUserGroupList", userId);
    }

    /**
     * 处理拒绝或接受
     * @param userId 被邀请者Id，申请者Id
     * @param inviteUserId 邀请者Id，或管理员Id
     * @param groupId 组Id
     * @param isRefuse 是否拒绝
     * @param refuseMsg 拒绝理由
     * @param type 1邀请;2申请
     * @return
     */
    public Map<String, Object> dealInvite(String userId, String inviteUserId, String groupId, boolean isRefuse, String refuseMsg, int type, String operId) {
        Map<String, Object> m=new HashMap<String, Object>();
        Map<String, Object> param=new HashMap<String, Object>();

        param.put("aUserId", inviteUserId);
        param.put("bUserId", userId);
        param.put("groupId", groupId);
        List<InviteGroupPo> igl=(type==1?inviteGroupDao.queryForList("getInvitingList", param):inviteGroupDao.queryForList("getApplyingList", param));

        if (igl==null||igl.size()==0) {
            m.put("ReturnType", "1006");
            m.put("Message", "没有邀请信息，不能处理");
        } else {
            InviteGroupPo igPo=igl.get(0);
            igPo.setAcceptTime(new Timestamp(System.currentTimeMillis()));
            GroupPo gp=getGroupById(groupId);
            if (!isRefuse) { //是接受
                igPo.setAcceptFlag(1);
                m.put("DealType", "1");
                //判断是否已是用户，若已是，则不必插入了
                if (existUserInGroup(groupId, userId)==0) {
                    //插入用户组表
                    insertGroupUser(gp, userDao.getInfoObject("getUserById", userId), 0, true, operId);
                    m.put("Message", "成功加入组");
                    m.put("ReturnType", "1001");
                } else {
                    m.put("Message", "已经加入组");
                    m.put("ReturnType", "10011");
                }
            } else { //是拒绝
                igPo.setRefuseMessage(refuseMsg);
                igPo.setAcceptFlag(2);
                m.put("DealType", "2");
                m.put("ReturnType", "1001");
            }
            //把在同一组邀请同一个人的消息一起都处理掉
            inviteGroupDao.update("sameUserInviteDeal", igPo);
            inviteGroupDao.update(igPo);//更新组邀请信息

            @SuppressWarnings("unchecked")
            SocketClient sc=((CacheEle<SocketClient>)SystemCache.getCache(WtAppEngineConstants.SOCKET_OBJ)).getContent();
            if (sc!=null) {
                //发送消息
                MsgNormal nMsg=new MsgNormal();
                nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                nMsg.setFromType(1);
                nMsg.setToType(1);
                nMsg.setMsgType(0);
                nMsg.setAffirm(1);
                nMsg.setBizType(0x04);
                nMsg.setCmdType(2);
                nMsg.setCommand(3);//处理组邀请信息
                //发送给userId
                Map<String, Object> dataMap=new HashMap<String, Object>();
                if (gp!=null) dataMap.put("GroupInfo", gp.toHashMap4View());
                dataMap.put("DealType", isRefuse?"2":"1");
                dataMap.put("InType", type+"");
                dataMap.put("OperatorId", operId);
                if (isRefuse&&!StringUtils.isNullOrEmptyOrSpace(refuseMsg)) dataMap.put("RefuseMsg", refuseMsg);
                dataMap.put("DealTime", System.currentTimeMillis());
                MapContent mc=new MapContent(dataMap);
                nMsg.setMsgContent(mc);

                if (type==1) {
                    dataMap.put("_TOUSERS", inviteUserId);
                } else if (type==2) {
                    dataMap.put("_TOUSERS", userId);
                }
                dataMap.put("_AFFIRMTYPE", "3");
                sc.addSendMsg(nMsg);
            }
        }
        return m;
    }

    /**
     * 审核接收或拒绝
     * @param inviteUserId 邀请人
     * @param beInvitedUserId 被邀请人
     * @param groupId 用户组Id
     * @param isRefuse 是否拒绝
     * @param refuseMsg 拒绝理由
     * @return
     */
    public Map<String, Object> dealCheck(String inviteUserId, String beInvitedUserId, String groupId, boolean isRefuse, String refuseMsg, String operId) {
        Map<String, Object> m=new HashMap<String, Object>();
        if (userDao.getInfoObject("getUserById", inviteUserId)==null) {
            m.put("ReturnType", "10041");
            m.put("Message", "邀请人不存在");
            return m;
        }
        if (userDao.getInfoObject("getUserById", beInvitedUserId)==null) {
            m.put("ReturnType", "10051");
            m.put("Message", "被邀请人不存在");
            return m;
        }
        Map<String, Object> param=new HashMap<String, Object>();

        param.put("aUserId", inviteUserId);
        param.put("bUserId", beInvitedUserId);
        param.put("groupId", groupId);
        List<InviteGroupPo> igl=inviteGroupDao.queryForList("getInvitingList", param);

        if (igl==null||igl.size()==0) {
            m.put("ReturnType", "1006");
            m.put("Message", "没有邀请信息，不能处理");
        } else {
            InviteGroupPo igPo=igl.get(0);
            if (igPo.getAcceptFlag()!=0) {
                m.put("ReturnType", "1007");
                if (igPo.getAcceptFlag()==1||igPo.getAcceptFlag()==3) m.put("Message", "邀请已被接收");
                if (igPo.getAcceptFlag()==2||igPo.getAcceptFlag()==4) m.put("Message", "邀请已被拒绝");
            } else {
                if (igPo.getManagerFlag()!=0) {
                    m.put("ReturnType", "1007");
                    if (igPo.getManagerFlag()==1) m.put("Message", "管理员已同意这个申请");
                    if (igPo.getManagerFlag()==2) m.put("Message", "管理员已拒绝这个申请");
                } else {//真正的处理
                    if (isRefuse) {
                        igPo.setAcceptFlag(5);
                        igPo.setManagerFlag(2);
                    } else {
                        igPo.setManagerFlag(1);
                    }
                    igPo.setRefuseMessage(refuseMsg);
                    inviteGroupDao.update(igPo);//更新组邀请信息
                }
                m.put("ReturnType", "1001");
            }

            @SuppressWarnings("unchecked")
            SocketClient sc=((CacheEle<SocketClient>)SystemCache.getCache(WtAppEngineConstants.SOCKET_OBJ)).getContent();
            if (isRefuse&&sc!=null) {
                //通知：发送消息
                MsgNormal nMsg=new MsgNormal();
                nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                nMsg.setFromType(1);
                nMsg.setToType(1);
                nMsg.setMsgType(0);
                nMsg.setAffirm(1);
                nMsg.setBizType(0x04);
                nMsg.setCmdType(2);
                nMsg.setCommand(8);//处理组邀请信息
                //发送给userId
                Map<String, Object> dataMap=new HashMap<String, Object>();
                GroupPo gp=getGroupById(groupId);
                if (gp!=null) dataMap.put("GroupInfo", gp.toHashMap4View());
                UserPo u=userDao.getInfoObject("getUserById", inviteUserId);
                Map<String, Object> um=u.toHashMap4Mobile();
                um.remove("PhoneNum");
                um.remove("Email");
                dataMap.put("InviteUserInfo", um);
                dataMap.put("OperatorId", operId);
                u=userDao.getInfoObject("getUserById", beInvitedUserId);
                um=u.toHashMap4Mobile();
                um.remove("PhoneNum");
                um.remove("Email");
                dataMap.put("BeInvitedUserInfo", um);
                if (!StringUtils.isNullOrEmptyOrSpace(refuseMsg)) dataMap.put("RefuseMsg", refuseMsg);
                dataMap.put("DealTime", System.currentTimeMillis()+"");
                MapContent mc=new MapContent(dataMap);
                nMsg.setMsgContent(mc);

                dataMap.put("_TOUSERS", inviteUserId);
                dataMap.put("_AFFIRMTYPE", "0");//不需要任何回复
                sc.addSendMsg(nMsg);
            }
        }
        return m;
    }

    /**
     * 踢出用户组
     * @param gp 用户组对象
     * @param userIds 用户Id，用逗号隔开
     * @return
     */
    public Map<String, Object> kickoutGroup(GroupPo gp, String userIds, String operId) {
        String groupId=gp.getGroupId();
        List<UserPo> ul=userDao.queryForList("getGroupMembers", groupId);

        List<UserPo> beKickoutUserList=new ArrayList<UserPo>();//被正式踢出用户组的用户信息
        Map<String, String> param=new HashMap<String, String>();

        Map<String, Object> ret=new HashMap<String, Object>();
        ret.put("GroupId", groupId);
        ret.put("DeleteGroup", "0");
        List<Map<String, String>> resultList=new ArrayList<Map<String, String>>();
        ret.put("ResultList", resultList);
        String[] ua=userIds.split(",");

        for (String userId: ua) {
            Map<String, String> oneResult=new HashMap<String, String>();
            String _userId=userId.trim();
            oneResult.put("UserId", _userId);
            resultList.add(oneResult);
            if (gp.getAdminUserIds().equals(_userId)) {
                oneResult.put("DealType", "2");
                continue;
            }
            int i=-1;
            if (ul!=null&&!ul.isEmpty()) {
                for (int j=0; j<ul.size(); j++) {
                    if (ul.get(j).getUserId().equals(_userId)) {
                        beKickoutUserList.add(ul.get(j));
                        i=j;
                        break;
                    }
                }
            }
            if (i==-1) oneResult.put("DealType", "3");
            else {//正式踢出某一个用户的操作
                ul.remove(i);
                oneResult.put("DealType", "1");
                //删除组内用户
                param.clear();
                param.put("userId", _userId);
                param.put("groupId", groupId);
                groupDao.delete("deleteGroupUser", param);
                //删除别名
                userAliasService.delUserAliasInGroup(groupId, _userId);
            }
        }
        //都处理后的处理
        if (ul.size()==1) {//删除组
            boolean canDissolve=true;
            if (ul.size()==1) {
                if (ul.get(0).getUserId().equals(gp.getCreateUserId())) canDissolve=false;
            }
            if (canDissolve) {
                groupDao.delete(gp.getGroupId());
                //删除所有的组内人员信息
                param.clear();
                param.put("groupId", groupId);
                groupDao.delete("deleteGroupUser", param);
                //处理组邀请信息表，把flag设置为2
                inviteGroupDao.update("setFlag2", groupId);
                ret.put("DeleteGroup", "1");//返回值，告诉调用者，由于组内人员只有1人，所以要删除组
                //删除组内所有成员的别名
                userAliasService.delAliasInGroup(groupId);
            }
        }

        @SuppressWarnings("unchecked")
        SocketClient sc=((CacheEle<SocketClient>)SystemCache.getCache(WtAppEngineConstants.SOCKET_OBJ)).getContent();
        if (beKickoutUserList!=null&&!beKickoutUserList.isEmpty()&&sc!=null) {
            //通知
            MsgNormal nMsg=new MsgNormal();
            nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            nMsg.setFromType(1);
            nMsg.setToType(1);
            nMsg.setMsgType(0);
            nMsg.setAffirm(1);
            nMsg.setBizType(0x04);
            nMsg.setCmdType(2);
            nMsg.setCommand(5);//退组用户消息
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("GroupId", gp.getGroupId());
            dataMap.put("OperatorId", operId);
            List<Map<String, Object>> userMapList=new ArrayList<Map<String, Object>>();
            for (UserPo _up: beKickoutUserList) userMapList.add(_up.toHashMap4Mobile());
            dataMap.put("UserList", userMapList);
            MapContent mc=new MapContent(dataMap);
            nMsg.setMsgContent(mc);
            dataMap.put("_TOGROUPS", gp.getGroupId());
            dataMap.put("_TOUSERS", userIds);
            dataMap.put("_AFFIRMTYPE", "3");
            sc.addSendMsg(nMsg);

            //同步消息：删除组用户
            MsgNormal sMsg=new MsgNormal();
            sMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            sMsg.setFromType(1);
            sMsg.setToType(1);
            sMsg.setMsgType(0);
            sMsg.setAffirm(1);
            sMsg.setBizType(0x08);
            sMsg.setCmdType(2);//组
            sMsg.setCommand(5);//删除组内用户
            Map<String, Object> dataMap1=new HashMap<String, Object>();
            dataMap1.put("GroupId", gp.getGroupId());
            String _userIds="";
            for (UserPo _up: beKickoutUserList) {
                _userIds+=","+_up.getUserId();
            }
            dataMap1.put("UserIds", _userIds.substring(1));
            MapContent mc1=new MapContent(dataMap1);
            sMsg.setMsgContent(mc1);
            sc.addSendMsg(sMsg);
        }
        if (ul.size()==1&&sc!=null) {
            //同步消息：组信息修改
            MsgNormal sMsg=new MsgNormal();
            sMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            sMsg.setFromType(1);
            sMsg.setToType(1);
            sMsg.setMsgType(0);
            sMsg.setAffirm(1);
            sMsg.setBizType(0x08);
            sMsg.setCmdType(2);//组
            sMsg.setCommand(3);//删除
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("GroupId", gp.getGroupId());
            MapContent mc=new MapContent(dataMap);
            sMsg.setMsgContent(mc);
            sc.addSendMsg(sMsg);

            //通知消息：组信息修改
            String toUser="";
            if (ul!=null&&!ul.isEmpty()) {
                for (int i=0;i<ul.size(); i++) {
                    toUser+=","+ul.get(i).getUserId();
                }
            }
            if (StringUtils.isNullOrEmptyOrSpace(toUser)) {
                MsgNormal nMsg=new MsgNormal();
                nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                nMsg.setFromType(1);
                nMsg.setToType(1);
                nMsg.setMsgType(0);
                nMsg.setAffirm(1);
                nMsg.setBizType(0x04);
                nMsg.setCmdType(2);
                nMsg.setCommand(6);//删除组，或解散组
                Map<String, Object> dataMap1=new HashMap<String, Object>();
                dataMap1.put("GroupId", gp.getGroupId());
                dataMap1.put("OperatorId", operId);
                dataMap1.put("DelReason", "2");//=2因为退组而删除组;=1直接删除组
                MapContent mc1=new MapContent(dataMap1);
                nMsg.setMsgContent(mc1);
                dataMap1.put("_TOUSERS", toUser.substring(1));
                dataMap.put("_AFFIRMTYPE", "3");
                sc.addSendMsg(nMsg);
            }
        }
        return ret;
    }

    /**
     * 解散组
     * @param gp 被解散的组
     * @return
     */
    public Map<String, Object> dissolve(GroupPo gp, String operId) {
        String groupId=gp.getGroupId();

        Map<String, Object> ret=new HashMap<String, Object>();
        groupDao.delete(gp.getGroupId());
        Map<String, String> param=new HashMap<String, String>();
        //删除所有的组内人员信息
        param.put("groupId", groupId);
        groupDao.delete("deleteGroupUser", param);
        //处理组邀请信息表，把flag设置为2
        inviteGroupDao.update("setFlag2", groupId);
        //删除组内所有成员的别名
        userAliasService.delAliasInGroup(groupId);

        @SuppressWarnings("unchecked")
        SocketClient sc=((CacheEle<SocketClient>)SystemCache.getCache(WtAppEngineConstants.SOCKET_OBJ)).getContent();
        if (sc!=null) {
            //通知:告诉大家
            MsgNormal nMsg=new MsgNormal();
            nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            nMsg.setFromType(1);
            nMsg.setToType(1);
            nMsg.setMsgType(0);
            nMsg.setAffirm(1);
            nMsg.setBizType(0x04);
            nMsg.setCmdType(2);
            nMsg.setCommand(6);//删除组，或解散组
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("GroupId", gp.getGroupId());
            dataMap.put("OperatorId", operId);
            dataMap.put("DelReason", "1");//==1直接删除组;2因为退组而删除组
            MapContent mc=new MapContent(dataMap);
            nMsg.setMsgContent(mc);
            dataMap.put("_TOGROUPS", gp.getGroupId());
            dataMap.put("_AFFIRMTYPE", "3");
            sc.addSendMsg(nMsg);

            //同步消息：删除组内成员
            MsgNormal sMsg=new MsgNormal();
            sMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            sMsg.setFromType(1);
            sMsg.setToType(1);
            sMsg.setMsgType(0);
            sMsg.setAffirm(1);
            sMsg.setBizType(0x08);
            sMsg.setCmdType(2);//组
            sMsg.setCommand(3);//删除组
            Map<String, Object> dataMap1=new HashMap<String, Object>();
            dataMap1.put("GroupId", gp.getGroupId());
            MapContent mc1=new MapContent(dataMap1);
            sMsg.setMsgContent(mc1);
            sc.addSendMsg(sMsg);
        }
        ret.put("ReturnType", "1001");
        return ret;
    }

    /**
     * 移交管理员权限
     * @param gp 组对象
     * @param toUserId 被移交用户Id
     * @return
     */
    public Map<String, Object> changGroupAdminner(GroupPo gp, String toUserId, String operId) {
        Map<String, Object> param=new HashMap<String, Object>();
        //1、判断是否已经在组
        param.put("userId", toUserId);
        param.put("groupId", gp.getGroupId());
        boolean find=(groupDao.getCount("getUserInGroup", param)>0);
        param.clear();

        Map<String, Object> ret=new HashMap<String, Object>();
        if (!find) {
            ret.put("ReturnType", "10041");
            ret.put("Message", "被移交用户不在该组");
        } else {
            param.put("groupId", gp.getGroupId());
            param.put("adminUserIds", toUserId);
            groupDao.update(param);
            ret.put("ReturnType", "1001");

            @SuppressWarnings("unchecked")
            SocketClient sc=((CacheEle<SocketClient>)SystemCache.getCache(WtAppEngineConstants.SOCKET_OBJ)).getContent();
            if (sc!=null) {
                //通知消息：权限转移消息
                MsgNormal nMsg=new MsgNormal();
                nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                nMsg.setFromType(1);
                nMsg.setToType(0);
                nMsg.setMsgType(0);
                nMsg.setAffirm(1);
                nMsg.setBizType(0x04);
                nMsg.setCmdType(2);
                nMsg.setCommand(7);
                Map<String, Object> dataMap=new HashMap<String, Object>();
                dataMap.put("GroupId", gp.getGroupId());
                dataMap.put("OperatorId", operId);
                UserPo u=userDao.getInfoObject("getUserById", toUserId);
                dataMap.put("NewAdminInfo", u.toHashMap4Mobile());
                MapContent mc=new MapContent(dataMap);
                nMsg.setMsgContent(mc);
                dataMap.put("_TOGROUPS", gp.getGroupId());
                dataMap.put("_AFFIRMTYPE", "0");//不需要任何回复
                sc.addSendMsg(nMsg);
            }
        }
        return ret;
    }

    public Map<String, Object> updateGroupUser(Map<String, String> param, String userId, GroupPo gp) {
        String updateUserId=param.get("updateUserId");

        Map<String, Object> ret=new HashMap<String, Object>();
        if (userId.equals(updateUserId)) {
            ret.put("ReturnType", "1006");
            ret.put("Message", "修改人和被修改人不能是同一个人");
        } else {
            Map<String, Object> paraM=new HashMap<String, Object>();
            //1、判断是否已经在组
            paraM.put("groupId", gp.getGroupId());
            paraM.put("userId", updateUserId);
            boolean find=(groupDao.getCount("getUserInGroup", paraM)>0);
            paraM.put("userId", userId);
            boolean find2=(groupDao.getCount("getUserInGroup", paraM)>0);

            if (!find) {
                ret.put("ReturnType", "10041");
                ret.put("Message", "被修改用户不在该组");
            } else {
                if (!find2) {
                    ret.put("ReturnType", "10021");
                    ret.put("Message", "修改用户不在该组");
                } else {
                    UserAliasPo uaPo=new UserAliasPo();
                    uaPo.setTypeId(gp.getGroupId());
                    uaPo.setMainUserId(userId);
                    uaPo.setAliasUserId(updateUserId);
                    uaPo.setAliasName(param.get("userAliasName"));
                    uaPo.setAliasDescn(param.get("userAliasDescn"));
                    int flag=userAliasService.save(uaPo);
                    if (flag==-1) {
                        ret.put("ReturnType", "1005");
                        ret.put("Message", "无法获得修改所需的新信息");
                    } else if (flag==-2) {
                        ret.put("ReturnType", "1002");
                        ret.put("Message", "无法得到修改用户");
                    } else if (flag==-3) {
                        ret.put("ReturnType", "1004");
                        ret.put("Message", "无法得到被修改用户");
                    } else if (flag==1) {
                        ret.put("ReturnType", "1001");
                        ret.put("Message", "新增了用户组内别名");
                    } else if (flag==2) {
                        ret.put("ReturnType", "10011");
                        ret.put("Message", "修改了用户组内别名");
                    } else {
                        ret.put("ReturnType", "10012");
                        ret.put("Message", "无需修改");
                    }
                }
            }
        }
        return ret;
    }

    /*
     * 得到所有用户组信息，返回的用户组列表中的元素是用户组模型，包括该组下的用户。
     * 此方法目前用于初始化对讲用户组内存数据库。
     * @return
     */
    private List<Group> getAllGroup() {
        Map<String, String> param=new HashMap<String, String>();
        param.put("orderByClause", "id");
        List<GroupPo> gl=groupDao.queryForList(param);
        if (gl!=null&&gl.size()>0) {
            List<Group> ret=new ArrayList<Group>();
            Group item=null;
            for (GroupPo gp: gl) {
                item=new Group();
                item.buildFromPo(gp);
                ret.add(item);
            }
            List<Map<String, Object>> ul=userDao.queryForListAutoTranform("getListUserInGroup", null);
            if (ul!=null&&ul.size()>0) {
                Map<String, Object> up=null;
                UserPo _up=null;
                Group _g=null;
                int i=0;
                String addGroupId="";
                while (i<ul.size()) {
                    up=ul.get(i++);
                    _up=new UserPo();
                    _up.fromHashMap(up);
                    _up.setUserId((String)up.get("id"));
                    if (!addGroupId.equals((String)up.get("groupId"))) {
                        addGroupId=(String)up.get("groupId");
                        _g=null;
                        for (Group g: ret) {
                            if (g.getGroupId().equals(addGroupId)) {
                                _g=g;
                                break;
                            }
                        }
                    }
                    if (_g!=null) _g.addOneUser(_up);
                }
            }
            return ret;
        }
        return null;
    }
}