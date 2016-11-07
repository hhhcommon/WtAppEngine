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
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.intercom.model.GroupInterCom;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.push.core.message.CompareMsg;
import com.woting.push.core.message.MsgNormal;
import com.woting.push.core.message.content.MapContent;
import com.woting.passport.UGA.model.Group;
import com.woting.passport.UGA.persis.pojo.GroupPo;
import com.woting.passport.UGA.persis.pojo.GroupUserPo;
import com.woting.passport.UGA.persis.pojo.UserPo;
import com.woting.passport.groupinvite.persis.pojo.InviteGroupPo;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.SessionService;
import com.woting.passport.useralias.persis.pojo.UserAliasPo;
import com.woting.passport.useralias.service.UserAliasService;

/**
 * 用户组处理，包括创建组，查询组；组邀请等信息
 * @author wanghui
 */
public class GroupService {
    private GroupMemoryManage gmm=GroupMemoryManage.getInstance();
    private PushMemoryManage pmm=PushMemoryManage.getInstance();

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

    @PostConstruct
    public void initParam() {
        userDao.setNamespace("WT_USER");
        groupDao.setNamespace("WT_GROUP");
        inviteGroupDao.setNamespace("WT_GROUPINVITE");
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
            //插入用户组所属用户
            List<UserPo> ul = group.getUserList();
            if (ul!=null&&ul.size()>0) {
                for (UserPo u: ul) this.insertGroupUser((GroupPo)group.convert2Po(), u, 0);
            }
            group.setCTime(new Timestamp(System.currentTimeMillis()));
            i=1;
            //新建组加入对讲组内存
            gmm.addOneGroup(group);
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
    public int insertGroupUser(GroupPo g, UserPo u, int isSelfIn) {
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
            //更新对讲组信息
            GroupInterCom gic = gmm.getGroupInterCom(g.getGroupId());
            if (gic!=null) {
                List<UserPo> upl=gic.getGroup().getUserList();
                //发送广播消息，把加入新用户的消息通知大家
                if (upl!=null&&!upl.isEmpty()) {
                    //生成消息
                    MsgNormal bMsg=new MsgNormal();
                    bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                    bMsg.setFromType(1);
                    bMsg.setToType(0);
                    bMsg.setMsgType(0);
                    bMsg.setAffirm(1);
                    bMsg.setBizType(1);
                    bMsg.setCmdType(1);
                    bMsg.setCommand(0x20);//有人加入组的广播
                    Map<String, Object> dataMap=new HashMap<String, Object>();
                    dataMap.put("GroupId", g.getGroupId());
                    dataMap.put("UserInfo", u.toHashMap4Mobile());
                    MapContent mc=new MapContent(dataMap);
                    bMsg.setMsgContent(mc);

                    //通知消息
                    MsgNormal nMsg=new MsgNormal();
                    nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                    nMsg.setFromType(1);
                    bMsg.setToType(0);
                    nMsg.setMsgType(0);
                    nMsg.setAffirm(1);
                    nMsg.setBizType(0x04);
                    nMsg.setCmdType(2);
                    nMsg.setCommand(4);
                    nMsg.setMsgContent(mc);

                    for (UserPo up: upl) {
                        List<MobileUDKey> l=(List<MobileUDKey>)sessionService.getActivedUserUDKs(up.getUserId());
                        if (l!=null&&!l.isEmpty()) {
                            for (MobileUDKey mUdk: l) {
                                if (mUdk!=null) {
                                    pmm.getSendMemory().addUniqueMsg2Queue(mUdk, bMsg, new CompareGroupMsg());
                                }
                            }
                        }
                        pmm.getSendMemory().addMsg2NotifyQueue(up.getUserId(), nMsg);//发送通知消息
                    }
                }
                gic.getGroup().addOneUser(u);
            }
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
    public List<Map<String, Object>> getGroupsByUserId(String userId) {
        try {
            return groupDao.queryForListAutoTranform("getGroupListByUserId", userId);
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
    public List<Map<String, Object>> getCreateGroupsByUserId(String userId) {
        try {
            return groupDao.queryForListAutoTranform("getCreateGroupListByUserId", userId);
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
            GroupInterCom gic = gmm.getGroupInterCom(groupId);
            return gic==null?userDao.queryForList("getGroupMembers", groupId):gic.getGroup().getUserList();
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
            for (GroupPo gp: gl) {
                item=new Group();
                item.buildFromPo(gp);
                ret.add(item);
            }
            List<Map<String, Object>> ul = this.userDao.queryForListAutoTranform("getListUserInGroup", null);
            if (ul!=null&&ul.size()>0) {
                Map<String, Object> up=null;
                UserPo _up=null;
                Group _g=null;
                int i=0;
                String addGroupId="";
                while (i<ul.size()) {
                    up=ul.get(i++);
                    _up = new UserPo();
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

    public GroupPo getGroupById(String groupId) {
        try {
            GroupInterCom gic = gmm.getGroupInterCom(groupId);
            return gic==null?groupDao.getInfoObject("getGroupById", groupId):gic.getGroup();
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
        try {
            GroupInterCom gic = gmm.getGroupInterCom(groupId);
            if (gic==null) {
                Map<String, Object> param = new HashMap<String, Object>();
                param.put("userId", userId);
                param.put("groupId", groupId);
                return groupDao.getCount("existUserInGroup", param);
            } else {
                int c=0;
                if (gic.getGroup().getUserList().size()>0) {
                    for (UserPo up:gic.getGroup().getUserList()) {
                        if (up.getUserId().equals(userId)) {
                            c=1;
                            break;
                        }
                    }
                }
                return c;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 更新用户，更新用户的信息，不对对讲组内存结构进行广播，单要修改
     * @param user 用户信息
     * @return 更新用户成功返回1，否则返回0
     */
    public int updateGroup(GroupPo g) {
        int i=0;
        try {
            groupDao.update(g);
            i=1;
            GroupInterCom gic = gmm.getGroupInterCom(g.getGroupId());
            if (gic!=null) {
                gic.getGroup().buildFromPo(g);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
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
        GroupInterCom gic = gmm.getGroupInterCom(groupId);
        Group g=null;
        List<UserPo> upl = null;
        if (gic!=null) {
            g=gic.getGroup();
            upl=g.getUserList();
        } else {
            upl=userDao.queryForList("getGroupMembers", groupId);
            g=new Group();
            g.buildFromPo(gp);
        }
        UserPo _u=null;

        if (upl.size()>0) {
            for (UserPo _up:upl) {
                if (_up.getUserId().equals(u.getUserId())) {
                    _u=_up;
                    break;
                }
            }
        }
        if (_u==null) return 0;

        List<UserPo> oldUpl=new ArrayList<UserPo>();
        for (UserPo _up:upl) {
            if (!_up.getUserId().equals(u.getUserId())) oldUpl.add(_up);
        }

        //删除组内用户
        Map<String, String> param=new HashMap<String, String>();
        param.put("userId", u.getUserId());
        param.put("groupId", groupId);
        groupDao.delete("deleteGroupUser", param);
        if (gic!=null) {//删除内存
            gic.getGroup().delOneUser(u);
            gic.getEntryGroupUserMap().remove(u.getUserId());
        } else {
            for (UserPo _up: upl) {
                if (_up.getUserId().equals(u.getUserId())) {
                    upl.remove(_up);
                    break;
                }
            }
        }
        //删除组内成员的别名
        userAliasService.delUserAliasInGroup(groupId, u.getUserId());

        //删除组
        if (upl.size()<=1) {
            if (upl.size()==1) {
                //删除所有的组内人员信息
                param.clear();
                param.put("groupId", groupId);
                groupDao.delete("deleteGroupUser", param);
            }
            gmm.delOneGroup(g);
            groupDao.delete(gp.getGroupId());
            //处理组邀请信息表，把flag设置为2
            inviteGroupDao.update("setFlag2", groupId);
            //删除组内所有成员的别名
            userAliasService.delAliasInGroup(groupId);
            return 2;
        } else {
            if (u.getUserId().equals(gp.getAdminUserIds())) {
                List<Object> gupl = (List<Object>)groupDao.queryForListAutoTranform("getGroupUserByGroupId", groupId);
                //移交管理员
                GroupUserPo gup=(GroupUserPo)gupl.get(0);
                for (int i=1; i<gupl.size(); i++) {
                    GroupUserPo _gup=(GroupUserPo)gupl.get(i);
                    if (gup.getCTime().after(_gup.getCTime())) gup=_gup;
                }
                gp.setAdminUserIds(gup.getUserId());
                groupDao.update(gp);
            }
            //发送广播消息，把退出用户的消息通知大家
            if (oldUpl!=null&&!oldUpl.isEmpty()) {
                //生成消息
                MsgNormal bMsg=new MsgNormal();
                bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                bMsg.setFromType(1);
                bMsg.setToType(0);
                bMsg.setMsgType(0);
                bMsg.setAffirm(1);
                bMsg.setBizType(1);
                bMsg.setCmdType(1);
                bMsg.setCommand(0x30);//退出组
                Map<String, Object> dataMap=new HashMap<String, Object>();
                dataMap.put("GroupId", g.getGroupId());
                Map<String, Object> um=u.toHashMap4Mobile();
                um.remove("PhoneNum");
                um.remove("Email");
                dataMap.put("UserInfo", um);
                MapContent mc=new MapContent(dataMap);
                bMsg.setMsgContent(mc);

                //通知消息
                MsgNormal nMsg=new MsgNormal();
                nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                nMsg.setFromType(1);
                nMsg.setToType(0);
                nMsg.setMsgType(0);
                nMsg.setAffirm(1);
                nMsg.setBizType(0x04);
                nMsg.setCmdType(2);
                nMsg.setCommand(5);
                nMsg.setMsgContent(mc);

                for (UserPo up: upl) {
                    List<MobileUDKey> l=(List<MobileUDKey>)sessionService.getActivedUserUDKs(up.getUserId());
                    if (l!=null&&!l.isEmpty()) {
                        for (MobileUDKey mUdk: l) {
                            if (mUdk!=null) {
                                pmm.getSendMemory().addUniqueMsg2Queue(mUdk, bMsg, new CompareGroupMsg());
                            }
                        }
                    }
                    pmm.getSendMemory().addMsg2NotifyQueue(up.getUserId(), nMsg);//发送通知消息
                }
            }
            return 1;
        }
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
    public Map<String, Object> inviteGroup(String userId, String beInvitedUserIds, String groupId, String inviteMsg, int isManaager) {
        Map<String, Object> m=new HashMap<String, Object>();

        //1、判断邀请人是否在组
        List<UserPo> gul = this.getGroupMembers(groupId);
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
            m.put("ReturnType", "1001");
            Map<String, Object> resultM=new HashMap<String, Object>();
            m.put("Result", resultM);
            resultM.put("GroupId", groupId);

            Map<String, Object> param=new HashMap<String, Object>();
            param.put("aUserId", userId);
            param.put("groupId", groupId);
            List<InviteGroupPo> igl=inviteGroupDao.queryForList("getInvitingGroupList", param);

            List<Map<String, String>> resultList=new ArrayList<Map<String, String>>();
            resultM.put("ResultList", resultList);

            InviteGroupPo igp=null;
            String[] ua=beInvitedUserIds.split(",");
            long inviteTime=System.currentTimeMillis();
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
                    if (igp!=null) {
                        inviteGroupDao.update("againInvite", igp.getId());
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
                        //如果是系统管理员，要自动设置为已经管理员审核
                        if (isManaager==1) igp.setManagerFlag(1);
                        inviteGroupDao.insert(igp);
                        oneResult.put("InviteCount", "1");
                        //发送消息
                        MsgNormal bMsg=new MsgNormal();
                        bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                        bMsg.setFromType(1);
                        bMsg.setToType(0);
                        bMsg.setMsgType(0);
                        bMsg.setAffirm(1);
                        bMsg.setBizType(0x04);
                        bMsg.setCmdType(2);
                        bMsg.setCommand(1);//邀请入组通知
                        //发送给beInvitedUserId
//                        bMsg.setToType("("+_beInvitedUserId+"||wt)");
                        Map<String, Object> dataMap=new HashMap<String, Object>();
                        dataMap.put("FriendId", userId);
                        GroupPo gp=this.getGroupById(groupId);
                        if (gp!=null) {
                            Map<String, Object> gMap=new HashMap<String, Object>();
                            gMap.put("GroupId", gp.getGroupId());
                            gMap.put("GroupName", gp.getGroupName());
                            gMap.put("GroupDescn", gp.getDescn());
                            dataMap.put("GroupInfo", gMap);
                        }
                        dataMap.put("InviteTime", inviteTime);
                        MapContent mc=new MapContent(dataMap);
                        bMsg.setMsgContent(mc);
                        pmm.getSendMemory().addMsg2NotifyQueue(_beInvitedUserId, bMsg);
                    }
                }
                resultList.add(oneResult);
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
    public Map<String, Object> applyGroup(String userId, String groupId, String adminId, String applyMsg) {
        Map<String, Object> m=new HashMap<String, Object>();
        Map<String, Object> param=new HashMap<String, Object>();
        boolean canContinue=true;

        //1、判断是否已经在组
        List<UserPo> gul = this.getGroupMembers(groupId);
        boolean find=false;//申请者是否在组
        if (gul!=null&&!gul.isEmpty()) {
            for (UserPo up: gul) {
                if (up.getUserId().equals(userId)) {
                    find=true;
                    break;
                }
            }
        }
        if (find) {
            m.put("ReturnType", "1005");
            m.put("RefuseMsg", "申请人已在用户组");
            canContinue=false;
        }

        //2、判断是否已经申请
        if (canContinue) {
            param.put("aUserId", adminId);
            param.put("bUserId", userId);
            param.put("groupId", groupId);
            List<InviteGroupPo> igl=inviteGroupDao.queryForList("getApplyList", param);
            if (igl!=null&&igl.size()>0) {
                for (InviteGroupPo igp: igl) {
                    if (igp.getAcceptFlag()==0) {
                        m.put("ReturnType", "1006");
                        m.put("RefuseMsg", "您已申请");
                        inviteGroupDao.update("againApply", igp.getId());
                        m.put("ApplyCount", Math.abs(igp.getInviteVector()-1));
                        canContinue=false;
                        break;
                    }
                }
            }
        }
        if (canContinue) {
            InviteGroupPo igp= new InviteGroupPo();
            igp.setId(SequenceUUID.getUUIDSubSegment(4));
            igp.setaUserId(adminId);
            igp.setbUserId(userId);
            igp.setGroupId(groupId);
            igp.setInviteMessage(applyMsg);
            igp.setInviteVector(-1);
            inviteGroupDao.insert(igp);
            m.put("ReturnType", "1001");
            //发送通知类消息
            MsgNormal bMsg=new MsgNormal();
            bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            bMsg.setFromType(1);
            bMsg.setToType(0);
            bMsg.setMsgType(0);
            bMsg.setAffirm(1);
            bMsg.setBizType(0x04);
            bMsg.setCmdType(2);
            bMsg.setCommand(2);//处理组邀请信息
            //发送给userId
            Map<String, Object> dataMap=new HashMap<String, Object>();
            UserPo u=userDao.getInfoObject("getUserById", userId);
            Map<String, Object> um=u.toHashMap4Mobile();
            um.remove("PhoneNum");
            um.remove("Email");
            um.remove("Email");
            dataMap.put("ApplyUserInfo", um);
            GroupPo gp=this.getGroupById(groupId);
            if (gp!=null) {
                Map<String, Object> gMap=new HashMap<String, Object>();
                gMap.put("GroupId", gp.getGroupId());
                gMap.put("GroupName", gp.getGroupName());
                gMap.put("GroupDescn", gp.getDescn());
                dataMap.put("GroupInfo", gMap);
            }
            dataMap.put("ApplyTime", System.currentTimeMillis());
            MapContent mc=new MapContent(dataMap);
            bMsg.setMsgContent(mc);
            pmm.getSendMemory().addMsg2NotifyQueue(adminId, bMsg);
        }
        return m;
    }

    /**
     * 搜索用户组
     * @param searchStr 搜索的字符串
     * @return 用户列表
     */
    public List<Group> searchGroup(String searchStr) {
        List<Group> ret = new ArrayList<Group>();
        Map<String, List<Group>> _tempM=new HashMap<String, List<Group>>();
        //从内存中查找
        Group _g;
        int max=0, min=0;
        Map<String, GroupInterCom> gm = gmm.getAllGroup();
        for (String sKey: gm.keySet()) {
            GroupInterCom gic=gm.get(sKey);
            _g=gic.getGroup();
            int ss = searchScore(searchStr, _g);
            if (ss>0) {
                if (_tempM.get(ss+"")==null) _tempM.put(ss+"", new ArrayList<Group>());
                _tempM.get(ss+"").add(_g);
                max=ss>max?ss:max;
                min=ss<min?ss:min;
            }
        }
        _g=null;
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
    public Map<String, Object> dealInvite(String userId, String inviteUserId, String groupId, boolean isRefuse, String refuseMsg, int type) {
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
            GroupPo gp=this.getGroupById(groupId);
            if (!isRefuse) { //是接受
                igPo.setAcceptFlag(1);
                m.put("DealType", "1");
                //判断是否已是用户，若已是，则不必插入了
                if (this.existUserInGroup(groupId, userId)==0) {
                    //插入用户组表
                    this.insertGroupUser(gp, userDao.getInfoObject("getUserById", userId), 0);
                    m.put("ReturnType", "1001");
                } else {
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
            //发送消息
            MsgNormal bMsg=new MsgNormal();
            bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            bMsg.setFromType(1);
            bMsg.setToType(0);
            bMsg.setMsgType(0);
            bMsg.setAffirm(1);
            bMsg.setBizType(0x04);
            bMsg.setCmdType(2);
            bMsg.setCommand(3);//处理组邀请信息
            //发送给userId
            Map<String, Object> dataMap=new HashMap<String, Object>();
            if (gp!=null) {
                Map<String, Object> gMap=new HashMap<String, Object>();
                gMap.put("GroupId", gp.getGroupId());
                gMap.put("GroupName", gp.getGroupName());
                gMap.put("GroupDescn", gp.getDescn());
                dataMap.put("GroupInfo", gMap);
            }
            dataMap.put("DealType", isRefuse?"2":"1");
            dataMap.put("InType", type+"");
            if (isRefuse&&!StringUtils.isNullOrEmptyOrSpace(refuseMsg)) dataMap.put("RefuseMsg", refuseMsg);
            dataMap.put("DealTime", System.currentTimeMillis());
            MapContent mc=new MapContent(dataMap);
            bMsg.setMsgContent(mc);
            if (type==1) pmm.getSendMemory().addMsg2NotifyQueue(inviteUserId, bMsg);//1邀请,给邀请人发送信息
            else 
            if (type==2) pmm.getSendMemory().addMsg2NotifyQueue(userId, bMsg);//2申请,给申请人发送信息
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
    public Map<String, Object>  dealCheck(String inviteUserId, String beInvitedUserId, String groupId, boolean isRefuse, String refuseMsg) {
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
            //发送通知消息
            if (isRefuse) {
                //发送消息
                MsgNormal bMsg=new MsgNormal();
                bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                bMsg.setFromType(1);
                bMsg.setToType(0);
                bMsg.setMsgType(0);
                bMsg.setAffirm(1);
                bMsg.setBizType(0x04);
                bMsg.setCmdType(2);
                bMsg.setCommand(8);//处理组邀请信息
                //发送给userId
                Map<String, Object> dataMap=new HashMap<String, Object>();
                GroupPo gp=this.getGroupById(groupId);
                if (gp!=null) {
                    Map<String, Object> gMap=new HashMap<String, Object>();
                    gMap.put("GroupId", gp.getGroupId());
                    gMap.put("GroupName", gp.getGroupName());
                    gMap.put("GroupDescn", gp.getDescn());
                    dataMap.put("GroupInfo", gMap);
                }
                UserPo u=userDao.getInfoObject("getUserById", inviteUserId);
                Map<String, Object> um=u.toHashMap4Mobile();
                um.remove("PhoneNum");
                um.remove("Email");
                dataMap.put("InviteUserInfo", um);
                u=userDao.getInfoObject("getUserById", beInvitedUserId);
                um=u.toHashMap4Mobile();
                um.remove("PhoneNum");
                um.remove("Email");
                dataMap.put("BeInvitedUserInfo", um);
                if (!StringUtils.isNullOrEmptyOrSpace(refuseMsg)) dataMap.put("RefuseMsg", refuseMsg);
                dataMap.put("DealTime", System.currentTimeMillis());
                MapContent mc=new MapContent(dataMap);
                bMsg.setMsgContent(mc);
                pmm.getSendMemory().addMsg2NotifyQueue(inviteUserId, bMsg);
            }
        }
        return m;
    }

    /**
     * 更新用户，更新用户的信息，不对对讲组内存结构进行广播，但要修改
     * @param userId 修改者id
     * @param g 所修改的组对象
     * @param newInfo 所修改的新信息
     * @return 更新用户成功返回1，否则返回0
     */
    public void updateGroup(Map<String, Object> newInfo, String userId, GroupPo g) {
        if (g.getAdminUserIds().equals(userId)) { //修改组本身信息
            if (newInfo.get("groupDescn")!=null) g.setDescn(newInfo.get("groupDescn")+"");
            if (newInfo.get("groupName")!=null) g.setGroupName(newInfo.get("groupName")+"");
            if (newInfo.get("groupSignature")!=null) g.setGroupSignature(newInfo.get("groupSignature")+"");
            this.updateGroup(g);
        }
        if (newInfo.get("groupName")!=null) newInfo.put("groupAlias", newInfo.get("groupName"));

        if (newInfo.get("groupAlias")==null&&newInfo.get("groupDescn")==null) return;
        newInfo.put("groupId", g.getGroupId());
        newInfo.put("userId", userId);
        groupDao.update("updateGroupUserByUserIdGroupId", newInfo);
        //组信息广播到组内所有成员
        //通知消息
        MsgNormal nMsg=new MsgNormal();
        nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        nMsg.setFromType(1);
        nMsg.setToType(0);
        nMsg.setMsgType(0);
        nMsg.setAffirm(1);
        nMsg.setBizType(0x04);
        nMsg.setCmdType(2);
        nMsg.setCommand(9);
        Map<String, Object> dataMap=new HashMap<String, Object>();
        Map<String, Object> gMap=new HashMap<String, Object>();
        gMap.put("GroupId", g.getGroupId());
        gMap.put("GroupNum", g.getGroupNum());
        gMap.put("GroupType", g.getGroupType());
        if (!StringUtils.isNullOrEmptyOrSpace(g.getGroupSignature())) gMap.put("GroupSignature", g.getGroupSignature());
        if (!StringUtils.isNullOrEmptyOrSpace(g.getGroupImg())) gMap.put("GroupImg", g.getGroupImg());
        gMap.put("GroupName", g.getGroupName());
        if (!StringUtils.isNullOrEmptyOrSpace(g.getCreateUserId())) gMap.put("GroupCreator", g.getCreateUserId());
        if (!StringUtils.isNullOrEmptyOrSpace((String)g.getAdminUserIds())) gMap.put("GroupManager", g.getAdminUserIds());
        gMap.put("GroupCount", g.getGroupCount());
        if (!StringUtils.isNullOrEmptyOrSpace((String)g.getDescn())) gMap.put("GroupOriDescn", g.getDescn());
        dataMap.put("GroupInfo", gMap);
        MapContent mc=new MapContent(dataMap);
        nMsg.setMsgContent(mc);

        GroupInterCom gic = gmm.getGroupInterCom(g.getGroupId());
        List<UserPo> upl = null;
        if (gic!=null) upl=gic.getGroup().getUserList();
        else upl=userDao.queryForList("getGroupMembers", g.getGroupId());
        //群发
        for (UserPo _up: upl) {
            pmm.getSendMemory().addMsg2NotifyQueue(_up.getUserId(), nMsg);//发送通知消息
        }
    }

    /**
     * 踢出用户组
     * @param gp 用户组对象
     * @param userIds 用户Id，用逗号隔开
     * @return
     */
    public Map<String, Object> kickoutGroup(GroupPo gp, String userIds) {
        String groupId=gp.getGroupId();
        GroupInterCom gic = gmm.getGroupInterCom(groupId);
        Group g=null;
        List<UserPo> upl = null;
        if (gic!=null) {
            g=gic.getGroup();
            upl=g.getUserList();
        } else {
            upl=userDao.queryForList("getGroupMembers", groupId);
            g=new Group();
            g.buildFromPo(gp);
        }
        List<UserPo> oldUpl=new ArrayList<UserPo>();
        for (UserPo _up:upl) oldUpl.add(_up);

        List<UserPo> beKickoutUserList=new ArrayList<UserPo>();//被正式踢出用户组的用户信息
        Map<String, String> param=new HashMap<String, String>();

        Map<String, Object> ret=new HashMap<String, Object>();
        ret.put("GroupId", groupId);
        ret.put("DeleteGroup", "0");
        List<Map<String, String>> resultList=new ArrayList<Map<String, String>>();
        ret.put("ResultList", resultList);
        String[] ua=userIds.split(",");

        UserPo up=null;
        for (String userId: ua) {
            Map<String, String> oneResult=new HashMap<String, String>();
            String _userId=userId.trim();
            oneResult.put("UserId", _userId);
            resultList.add(oneResult);
            if (g.getAdminUserIds().equals(_userId)) {
                oneResult.put("DealType", "2");
                continue;
            }
            if (gic!=null&&gic.getSpeaker()!=null&&gic.getSpeaker().getUserId().equals(_userId)) {
                oneResult.put("DealType", "4");
                continue;
            }
            up=null;
            if (upl!=null&&!upl.isEmpty()) {
                for (UserPo _up: upl) {
                    if (_up.getUserId().equals(_userId)) {
                        up=_up;
                        break;
                    }
                }
            }
            if (up==null) oneResult.put("DealType", "3");
            else {//正式踢出某一个用户的操作
                oneResult.put("DealType", "1");
                beKickoutUserList.add(up);
                //删除组内用户
                param.clear();
                param.put("userId", up.getUserId());
                param.put("groupId", groupId);
                groupDao.delete("deleteGroupUser", param);
                //删除内存
                if (gic!=null) {
                  gic.getGroup().delOneUser(up);
                  gic.getEntryGroupUserMap().remove(up.getUserId());
                } else {
                    for (UserPo _up: upl) {
                        if (_up.getUserId().equals(up.getUserId())) {
                            upl.remove(_up);
                            break;
                        }
                    }
                }
                //删除别名
                userAliasService.delUserAliasInGroup(groupId, up.getUserId());
            }
        }
        //都处理后的处理
        if (upl.size()==1) {//删除组
            gmm.delOneGroup(g);
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
        //发送广播消息，把退出用户的消息通知大家
        if (oldUpl!=null&&!oldUpl.isEmpty()) {
            //生成消息
            MsgNormal bMsg=new MsgNormal();
            bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            bMsg.setFromType(1);
            bMsg.setToType(1);
            bMsg.setMsgType(0);
            bMsg.setAffirm(1);
            bMsg.setBizType(1);
            bMsg.setCmdType(1);
            bMsg.setCommand(0x30);//退组用户消息
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("GroupId", g.getGroupId());
            List<Map<String, Object>> userMapList=new ArrayList<Map<String, Object>>();
            for (UserPo _up: beKickoutUserList) {
                Map<String, Object> um=_up.toHashMap4Mobile();
                um.remove("PhoneNum");
                um.remove("EMail");
                userMapList.add(um);
            }
            dataMap.put("UserList", userMapList);
            MapContent mc=new MapContent(dataMap);
            bMsg.setMsgContent(mc);

            //通知消息
            MsgNormal nMsg=new MsgNormal();
            nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            nMsg.setFromType(1);
            nMsg.setToType(0);
            nMsg.setMsgType(0);
            nMsg.setAffirm(1);
            nMsg.setBizType(0x04);
            nMsg.setCmdType(2);
            nMsg.setCommand(5);
            nMsg.setMsgContent(mc);

            for (UserPo _up: upl) {
                List<MobileUDKey> l=(List<MobileUDKey>)sessionService.getActivedUserUDKs(up.getUserId());
                if (l!=null&&!l.isEmpty()) {
                    for (MobileUDKey mUdk: l) {
                        if (mUdk!=null) {
                            pmm.getSendMemory().addUniqueMsg2Queue(mUdk, bMsg, new CompareGroupMsg());
                        }
                    }
                }
                pmm.getSendMemory().addMsg2NotifyQueue(_up.getUserId(), nMsg);//发送通知消息
            }
            //给被剔除的人发通知消息
            for (UserPo _up: beKickoutUserList) {
                pmm.getSendMemory().addMsg2NotifyQueue(_up.getUserId(), nMsg);//发送通知消息
            }
        }
        return ret;
    }

    /**
     * 解散组
     * @param gp 被解散的组
     * @return
     */
    public Map<String, Object> dissolve(GroupPo gp) {
        String groupId=gp.getGroupId();
        GroupInterCom gic = gmm.getGroupInterCom(groupId);
        Group g=null;
        List<UserPo> upl = null;
        if (gic!=null) {
            g=gic.getGroup();
            upl=g.getUserList();
        } else {
            upl=userDao.queryForList("getGroupMembers", groupId);
            g=new Group();
            g.buildFromPo(gp);
        }

        Map<String, Object> ret=new HashMap<String, Object>();
        if (gic!=null&&gic.getSpeaker()!=null) ret.put("ReturnType", "1004");
        else {
            if (gic!=null) gmm.delOneGroup(g);
            groupDao.delete(gp.getGroupId());
            Map<String, String> param=new HashMap<String, String>();
            //删除所有的组内人员信息
            param.put("groupId", groupId);
            groupDao.delete("deleteGroupUser", param);
            //处理组邀请信息表，把flag设置为2
            inviteGroupDao.update("setFlag2", groupId);
            //删除组内所有成员的别名
            userAliasService.delAliasInGroup(groupId);

            //发送广播消息，把退出用户的消息通知大家
            if (upl!=null&&!upl.isEmpty()) {
                //生成消息
                MsgNormal bMsg=new MsgNormal();
                bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                bMsg.setFromType(1);
                bMsg.setToType(0);
                bMsg.setMsgType(0);
                bMsg.setAffirm(1);
                bMsg.setBizType(1);
                bMsg.setCmdType(1);
                bMsg.setCommand(0x30);//退组用户消息
                Map<String, Object> dataMap=new HashMap<String, Object>();
                dataMap.put("GroupId", g.getGroupId());
                dataMap.put("Del", "1");
                MapContent mc=new MapContent(dataMap);
                bMsg.setMsgContent(mc);

                //通知消息
                MsgNormal nMsg=new MsgNormal();
                nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                nMsg.setFromType(1);
                nMsg.setToType(0);
                nMsg.setMsgType(0);
                nMsg.setAffirm(1);
                nMsg.setBizType(0x04);
                nMsg.setCmdType(2);
                nMsg.setCommand(6);
                nMsg.setMsgContent(mc);

                for (UserPo up: upl) {
                    List<MobileUDKey> l=(List<MobileUDKey>)sessionService.getActivedUserUDKs(up.getUserId());
                    if (l!=null&&!l.isEmpty()) {
                        for (MobileUDKey mUdk: l) {
                            if (mUdk!=null) {
                                pmm.getSendMemory().addUniqueMsg2Queue(mUdk, bMsg, new CompareGroupMsg());
                            }
                        }
                    }
                    pmm.getSendMemory().addMsg2NotifyQueue(up.getUserId(), nMsg);//发送通知消息
                }
            }
            ret.put("ReturnType", "1001");
        }
        return ret;
    }

    /**
     * 移交管理员权限
     * @param gp 组对象
     * @param toUserId 被移交用户Id
     * @return
     */
    public Map<String, Object> changGroupAdminner(GroupPo gp, String toUserId) {
        String groupId=gp.getGroupId();
        GroupInterCom gic = gmm.getGroupInterCom(groupId);
        Group g=null;
        List<UserPo> upl = null;
        if (gic!=null) {
            g=gic.getGroup();
            upl=g.getUserList();
        } else {
            upl=userDao.queryForList("getGroupMembers", groupId);
            g=new Group();
            g.buildFromPo(gp);
        }

        Map<String, Object> ret=new HashMap<String, Object>();
        boolean find=false;
        if (upl!=null&&!upl.isEmpty()) {
            for (UserPo _up:upl) {
                if (_up.getUserId().equals(toUserId)) {
                    find=true;
                    break;
                }
            }
        }
        if (!find) {
            ret.put("ReturnType", "10041");
            ret.put("Message", "被移交用户不在该组");
        } else {
            Map<String, Object> param=new HashMap<String, Object>();
            param.put("groupId", gp.getGroupId());
            param.put("adminUserIds", toUserId);
            groupDao.update(param);
            g.setAdminUserIds(toUserId);
            ret.put("ReturnType", "1001");
            //发送权限转移消息
            if (upl!=null&&!upl.isEmpty()) {
                //通知消息
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
                Map<String, Object> gMap=new HashMap<String, Object>();
                gMap.put("GroupId", gp.getGroupId());
                gMap.put("GroupName", gp.getGroupName());
                gMap.put("GroupDescn", gp.getDescn());
                dataMap.put("GroupInfo", gMap);
                UserPo u=userDao.getInfoObject("getUserById", toUserId);
                Map<String, Object> um=u.toHashMap4Mobile();
                um.remove("PhoneNum");
                um.remove("Email");
                dataMap.put("NewAdminInfo", um);
                MapContent mc=new MapContent(dataMap);
                nMsg.setMsgContent(mc);
                for (UserPo _up: upl) {
                    pmm.getSendMemory().addMsg2NotifyQueue(_up.getUserId(), nMsg);//发送通知消息
                }
            }
        }
        return ret;
    }

    public Map<String, Object> updateGroupUser(Map<String, String> param, String userId, GroupPo gp) {
        String groupId=gp.getGroupId();
        GroupInterCom gic = gmm.getGroupInterCom(groupId);
        Group g=null;
        List<UserPo> upl = null;
        if (gic!=null) {
            g=gic.getGroup();
            upl=g.getUserList();
        } else {
            upl=userDao.queryForList("getGroupMembers", groupId);
            g=new Group();
            g.buildFromPo(gp);
        }
        String updateUserId=param.get("updateUserId");

        Map<String, Object> ret=new HashMap<String, Object>();
        if (userId.equals(updateUserId)) {
            ret.put("ReturnType", "1006");
            ret.put("Message", "修改人和被修改人不能是同一个人");
        } else {
            boolean find=false;
            boolean find2=false;
            if (upl!=null&&!upl.isEmpty()) {
                for (UserPo _up:upl) {
                    if (!find) find=_up.getUserId().equals(updateUserId);
                    if (!find2) find2=_up.getUserId().equals(userId);
                    if (find&&find2) break;
                }
            }
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

    class CompareGroupMsg implements CompareMsg<MsgNormal> {
        @Override
        public boolean compare(MsgNormal msg1, MsgNormal msg2) {
            if (msg1.getFromType()==msg2.getFromType()
              &&msg1.getToType()==msg2.getToType()
              &&msg1.getBizType()==msg2.getBizType()
              &&msg1.getCmdType()==msg2.getCmdType()
              &&msg1.getCommand()==msg2.getCommand() ) {
                if (msg1.getMsgContent()==null&&msg2.getMsgContent()==null) return true;
                if (((msg1.getMsgContent()!=null&&msg2.getMsgContent()!=null))
                  &&(((MapContent)msg1.getMsgContent()).get("GroupId").equals(((MapContent)msg2.getMsgContent()).get("GroupId")))) return true;
            }
            return false;
        }
    }
}