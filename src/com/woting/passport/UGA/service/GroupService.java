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
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.intercom.model.GroupInterCom;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.appengine.mobile.push.model.CompareMsg;
import com.woting.appengine.mobile.push.model.Message;
import com.woting.passport.UGA.model.Group;
import com.woting.passport.UGA.persistence.pojo.GroupPo;
import com.woting.passport.UGA.persistence.pojo.GroupUserPo;
import com.woting.passport.UGA.persistence.pojo.UserPo;

public class GroupService {
    private GroupMemoryManage gmm=GroupMemoryManage.getInstance();
    private PushMemoryManage pmm=PushMemoryManage.getInstance();

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
            //新建组加入对讲组内存
            gmm.addOneGroup(group);
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
            //更新对讲组信息
            GroupInterCom gic = gmm.getGroupInterCom(g.getGroupId());
            if (gic!=null) {
                gic.getGroup().addOneUser(u);
                Map<String, UserPo> entryGroupUsers=gic.getEntryGroupUserMap();
                if (!entryGroupUsers.isEmpty()) {
                    //发送广播消息，把加入新用户的消息通知大家
                    //生成消息
                    Message bMsg=new Message();
                    bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                    bMsg.setFromAddr("{(intercom)@@(www.woting.fm||S)}");
                    bMsg.setMsgType(1);
                    bMsg.setAffirm(0);
                    bMsg.setMsgBizType("GROUP_CTL");
                    bMsg.setCmdType("GROUP");//有人加入组的广播
                    bMsg.setCommand("b1");//新的组信息
                    Map<String, Object> dataMap=new HashMap<String, Object>();
                    dataMap.put("GroupId", g.getGroupId());
                    List<Map<String, Object>> rul=new ArrayList<Map<String, Object>>();
                    for (UserPo up: gic.getGroup().getUserList()) {
                        rul.add(up.toHashMap4Mobile());
                    }
                    dataMap.put("UserList", rul);
                    bMsg.setMsgContent(dataMap);
                    for (String k: entryGroupUsers.keySet()) {
                        String _sp[] = k.split("::");
                        MobileKey mk=new MobileKey();
                        mk.setMobileId(_sp[0]);
                        mk.setUserId(_sp[1]);
                        bMsg.setToAddr(MobileUtils.getAddr(mk));
                        pmm.getSendMemory().addUniqueMsg2Queue(mk, bMsg, new CompareGroupMsg());
                    }
                }
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
                boolean inserted=false;
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
     * 退出用户组，并广布组消息。
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
        //删除用户组
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
        if (upl.size()==1) {//删除组
            gmm.delOneGroup(g);
            groupDao.delete(gp.getGroupId());
            param.clear();
            param.put("groupId", groupId);
            groupDao.delete("deleteGroupUser", param);
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
            if (gic!=null) {
                Map<String, UserPo> entryGroupUsers=gic.getEntryGroupUserMap();
                if (!entryGroupUsers.isEmpty()) {
                    //生成消息
                    Message bMsg=new Message();
                    bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                    bMsg.setFromAddr("{(intercom)@@(www.woting.fm||S)}");
                    bMsg.setMsgType(1);
                    bMsg.setAffirm(0);
                    bMsg.setMsgBizType("GROUP_CTL");
                    bMsg.setCmdType("GROUP");//有人加入组的广播
                    bMsg.setCommand("b2");//新的组信息
                    Map<String, Object> dataMap=new HashMap<String, Object>();
                    dataMap.put("GroupId", g.getGroupId());
                    List<Map<String, Object>> rul=new ArrayList<Map<String, Object>>();
                    for (UserPo up: upl) {
                        rul.add(up.toHashMap4Mobile());
                    }
                    dataMap.put("UserList", rul);
                    bMsg.setMsgContent(dataMap);
                    for (String k: entryGroupUsers.keySet()) {
                        String _sp[] = k.split("::");
                        MobileKey mk=new MobileKey();
                        mk.setMobileId(_sp[0]);
                        mk.setUserId(_sp[1]);
                        bMsg.setToAddr(MobileUtils.getAddr(mk));
                        pmm.getSendMemory().addUniqueMsg2Queue(mk, bMsg, new CompareGroupMsg());
                    }
                    //若在对讲，也要推出对讲结构
                    
                }
            }
            return 1;
        }
    }
    /**
     * 搜索用户组
     * @param searchStr 搜索的字符串
     * @return 用户列表
     */
    public List<Group> searchGroup(String searchStr) {
        List<Group> ret = new ArrayList<Group>();
        Map<String, Group> _tempM=new HashMap<String, Group>();
        //从内存中查找
        Group _g;
        int max=0;
        Map<String, GroupInterCom> gm = gmm.getAllGroup();
        for (String sKey: gm.keySet()) {
            GroupInterCom gic=gm.get(sKey);
            _g=gic.getGroup();
            int ss = searchScore(searchStr, _g);
            if (ss>0) {
                _tempM.put(ss+"", _g);
                max=ss>max?ss:max;
            }
        }
        _g=null;
        for (int i=max; i>0; i--) {
            _g=_tempM.get(i+"");
            if (_g!=null) ret.add(_g);
            _g=null;
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
        if (g.getGroupName()!=null&&g.getGroupName().indexOf(oneWord)>0) ret+=10;
        if (g.getGroupNum()!=null&&g.getGroupNum().indexOf(oneWord)>0) ret+=10;
        List<UserPo> ul=g.getUserList();
        UserPo up;
        String userName;
        for (int i=0;i<ul.size(); i++) {
            up=ul.get(i);
            userName=up.getLoginName();
            if (userName!=null) {
                if (userName.equals(oneWord)) ret+=3;
                if (userName.indexOf(oneWord)>0) ret+=1;
            }
        }
        return ret;
    }

    
    class CompareGroupMsg implements CompareMsg {
        @Override
        public boolean compare(Message msg1, Message msg2) {
            if (msg1.getFromAddr().equals(msg2.getFromAddr())
              &&msg1.getToAddr().equals(msg2.getToAddr())
              &&msg1.getMsgBizType().equals(msg2.getMsgBizType())
              &&msg1.getCmdType().equals(msg2.getCmdType())
              &&msg1.getCommand().equals(msg2.getCommand()) ) {
                if (msg1.getMsgContent()==null&&msg2.getMsgContent()==null) return true;
                if (((msg1.getMsgContent()!=null&&msg2.getMsgContent()!=null))
                  &&(((Map)msg1.getMsgContent()).get("GroupId").equals(((Map)msg2.getMsgContent()).get("GroupId")))) return true;
            }
            return false;
        }
    }
}