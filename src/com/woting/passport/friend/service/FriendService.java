package com.woting.passport.friend.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.DateUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.push.core.message.MsgNormal;
import com.woting.push.core.message.content.MapContent;
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.woting.passport.friend.persistence.pojo.FriendRelPo;
import com.woting.passport.friend.persistence.pojo.InviteFriendPo;
import com.woting.passport.useralias.model.UserAliasKey;
import com.woting.passport.useralias.persistence.pojo.UserAliasPo;
import com.woting.passport.useralias.service.UserAliasService;

public class FriendService {
    static private int INVITE_INTERVAL_TIME=5*60*1000;//毫秒数：5分钟
    static private int INVITE_REFUSE_TIME=5*60*1000;//7*24*60*60*1000;//毫秒数：一周
    private PushMemoryManage pmm=PushMemoryManage.getInstance();

    @Resource(name="defaultDAO")
    private MybatisDAO<UserPo> userDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<InviteFriendPo> inviteFriendDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<FriendRelPo> friendRelDao;

    @Resource
    private UserAliasService userAliasService;

    @PostConstruct
    public void initParam() {
        userDao.setNamespace("WT_USER");
        inviteFriendDao.setNamespace("WT_INVITE");
        friendRelDao.setNamespace("WT_FRIEND");
    }

    /**
     * 得到陌生人列表
     * @param userId 本人Id
     * @param searchStr 查找字符串
     * @return 陌生人列表或空
     */
    public List<UserPo> getStrangers(String userId, String searchStr) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return null;
        if (StringUtils.isNullOrEmptyOrSpace(searchStr)) return null;
        Map<String, Object> param=new HashMap<String, Object>();
        try {
            param.put("userId", userId);
            param.put("searchStr", searchStr);
            return userDao.queryForList("getStrangers", param);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 邀请陌生人为好友
     * @param userId 本人Id 邀请人
     * @param beInvitedUserId 被邀请人IdS
     * @param inviteMsg 邀请信息
     * @return 陌生人列表或空
     */
    public Map<String, Object> inviteFriend(String userId, String beInvitedUserId, String inviteMsg) {
        Map<String, Object> m=new HashMap<String, Object>();
        Map<String, Object> param=new HashMap<String, Object>();
        Map<String, Object> info;

        boolean canContinue=true, isUpdate=false;
        List<InviteFriendPo> ifl=null;
        List<FriendRelPo> fl=null;
        InviteFriendPo ifPo=null;

        //1-是否已经是好友了
        if (canContinue) {
            param.put("aUserId", userId);
            param.put("bUserId", beInvitedUserId);
            fl=friendRelDao.queryForList(param);
            if (fl!=null&&fl.size()>0) {
                m.put("ReturnType", "1004");
                m.put("Message", "已经是好友了");
                canContinue=false;
            }
        }
        //2-是否已经被对方邀请
        if (canContinue) {
            param.put("bUserId", userId);
            param.put("aUserId", beInvitedUserId);
            param.put("acceptFlag", "0");
            ifl=inviteFriendDao.queryForList(param);
            if (ifl!=null&&ifl.size()>0) {
                m.put("ReturnType", "1005");
                ifPo=ifl.get(0);
                info=new HashMap<String, Object>();
                info.put("InviteTime", ifPo.getInviteTime().getTime());
                info.put("InviteMessage", ifPo.getInviteMessage());
                info.put("InviteCount", ifPo.getInviteVector());
                //获得对方信息
                UserPo u=userDao.getInfoObject("getUserById", ifPo.getaUserId());
                info.put("InvitorName", u.getLoginName());
                info.put("InvitorPortrait", u.getPortraitMini());
                m.put("InviteInfo", info);
                canContinue=false;
            }
        }
        //3-是否是重复邀请，重复邀请，邀请内容相同，邀请间隔在INVITE_INTERVAL_TIME之内
        if (canContinue) {
            param.put("aUserId", userId);
            param.put("bUserId", beInvitedUserId);
            param.remove("acceptFlag");
            ifl=inviteFriendDao.queryForList(param);
            if (ifl!=null&&ifl.size()>0) {
                ifPo=ifl.get(0);
                if (ifPo.getAcceptFlag()==2) {//被拒绝了，再7天之内，不能再邀请
                    if (System.currentTimeMillis()-ifPo.getInviteTime().getTime()<=INVITE_REFUSE_TIME) {
                        m.put("ReturnType", "1006");
                        m.put("RefuseMsg", ifPo.getRefuseMessage());
                        canContinue=false;
                    } else isUpdate=true;
                    canContinue=false;
                } else if (ifPo.getAcceptFlag()==1) {//对方已接受了你的邀请，你们已经是好友了，这个逻辑应该在1中已经处理
                    m.put("ReturnType", "1004");
                    m.put("Message", "已经是好友了");
                    canContinue=false;
                } else {//你的邀请对方还没有处理
                    //如果邀请时间小于重复邀请间隔，且邀请内容相同，则视为重复邀请
                    if (System.currentTimeMillis()-ifPo.getInviteTime().getTime()<=INVITE_INTERVAL_TIME
                            &&inviteMsg.equals(ifPo.getInviteMessage())) {
                        m.put("ReturnType", "1007");
                        m.put("Message", "您已邀请，无需重复邀请");
                        canContinue=false;
                    } else isUpdate=true;
                }
            }
        }
        //正式保存邀请信息
        if (canContinue) {
            if (!isUpdate) {//新增
                ifPo = new InviteFriendPo();
                ifPo.setId(SequenceUUID.getUUIDSubSegment(4));
                ifPo.setaUserId(userId);
                ifPo.setbUserId(beInvitedUserId);
                ifPo.setInviteVector(1);
            } else {
                ifPo.setInviteTime(new Timestamp(System.currentTimeMillis()));
                ifPo.setInviteVector(ifPo.getInviteVector()+1);
                ifPo.setRefuseMessage("");
                ifPo.setInviteTime(null);
            }
            ifPo.setInviteMessage(inviteMsg);
            ifPo.setAcceptFlag(0);
            if (!isUpdate) inviteFriendDao.insert(ifPo);
            else inviteFriendDao.update(ifPo);

            //通知消息
            MsgNormal nMsg=new MsgNormal();
            nMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            nMsg.setFromType(1);
            nMsg.setToType(0);
            nMsg.setMsgType(0);
            nMsg.setAffirm(1);
            nMsg.setBizType(0x04);
            nMsg.setCmdType(1);
            nMsg.setCommand(1);
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("InviteMsg", inviteMsg);
            dataMap.put("InviteTime", System.currentTimeMillis());
            UserPo u=userDao.getInfoObject("getUserById", userId);
            Map<String, Object> um=u.toHashMap4Mobile();
            um.remove("PhoneNum");
            um.remove("Email");
            um.remove("Email");
            dataMap.put("InviteUserInfo", um);
            MapContent mc=new MapContent(dataMap);
            nMsg.setMsgContent(mc);
            pmm.getSendMemory().addMsg2NotifyQueue(beInvitedUserId, nMsg);//发送通知消息

            m.put("ReturnType", "1001");
            m.put("InviteCount", ifPo.getInviteVector());
        }
        return m;
    }

    /**
     * 得到邀请我的用户列表
     * @param userId 我的用户Id
     * @return 用户列表，除了用户信息外，还包括邀请相关的信息，如inviteTime,inviteMessage
     */
    public List<Map<String, Object>> getInvitedMeList(String userId) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return null;
        try {
            Map<String, Object> param=new HashMap<String, Object>();
            param.put("userId", userId);
            List<Map<String, Object>> ret=inviteFriendDao.queryForListAutoTranform("queryInvitedMeList", param);
            if (ret!=null&&ret.size()>0) return ret;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 处理邀请
     * @param userId 用户Id，我，被邀请人
     * @param inviteUserId 邀请人Id
     * @param isRefuse 是否为拒绝
     * @param refuseMsg 决绝理由
     * @return
     */
    public Map<String, Object> deal(String userId, String inviteUserId, boolean isRefuse, String refuseMsg) {
        Map<String, Object> m=new HashMap<String, Object>();
        InviteFriendPo ifPo=null;

        Map<String, Object> param=new HashMap<String, Object>();
        param.put("aUserId", inviteUserId);
        param.put("bUserId", userId);
        List<FriendRelPo> fl=friendRelDao.queryForList(param);
        if (fl!=null&&fl.size()>0) {
            m.put("ReturnType", "1004");
            m.put("Message", "已经是好友了");
        } else {
            param.put("acceptFlag", "0");
            List<InviteFriendPo> ifl=inviteFriendDao.queryForList(param);
            if (ifl==null||ifl.size()==0) {
                m.put("ReturnType", "1005");
                m.put("Message", "没有邀请信息，不能处理");
            } else {
                ifPo=ifl.get(0);
                ifPo.setAcceptTime(new Timestamp(System.currentTimeMillis()));
                if (!isRefuse) { //是接受
                    ifPo.setAcceptFlag(1);
                    m.put("DealType", "1");
                    //插入好友表
                    FriendRelPo frPo = new FriendRelPo();
                    frPo.setId(SequenceUUID.getUUIDSubSegment(4));
                    frPo.setaUserId(inviteUserId);
                    frPo.setbUserId(userId);
                    frPo.setInviteTime(ifPo.getAcceptTime());
                    frPo.setInviteVector(ifPo.getInviteVector());
                    friendRelDao.insert(frPo);
                } else { //是拒绝
                    ifPo.setRefuseMessage(refuseMsg);
                    ifPo.setAcceptFlag(2);
                    m.put("DealType", "2");
                }
                inviteFriendDao.update(ifPo);

                //发送消息——给邀请人
                MsgNormal bMsg=new MsgNormal();
                bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                bMsg.setFromType(1);
                bMsg.setMsgType(1);
                bMsg.setAffirm(0);
                bMsg.setBizType(0x04);
                bMsg.setCmdType(1);
                bMsg.setCommand(3);//处理组邀请信息
                //发送给inviteUserId
                bMsg.setToType(0);
                Map<String, Object> dataMap=new HashMap<String, Object>();
                dataMap.put("DealType", isRefuse?"2":"1");
                if (isRefuse&&!StringUtils.isNullOrEmptyOrSpace(refuseMsg)) dataMap.put("RefuseMsg", refuseMsg);
                dataMap.put("DealTime", System.currentTimeMillis());
                //加入被邀请人信息
                UserPo u=userDao.getInfoObject("getUserById", userId);
                Map<String, Object> um=u.toHashMap4Mobile();
                um.remove("PhoneNum");
                um.remove("Email");
                um.remove("Email");
                dataMap.put("BeInvitedUserInfo", um);
                MapContent mc=new MapContent(dataMap);
                bMsg.setMsgContent(mc);
                pmm.getSendMemory().addMsg2NotifyQueue(inviteUserId, bMsg);

                m.put("ReturnType", "1001");
            }
        }
        return m;
    }

    /**
     * 删除好友关系
     * @param userId 删除人Id
     * @param friendUserId 被删除人Id
     * @return
     */
    public Map<String, Object> del(String userId, String friendUserId) {
        Map<String, Object> ret=new HashMap<String, Object>();
        Map<String, Object> param=new HashMap<String, Object>();
        //1-判断是否是好友
        param.put("aUserId", userId);
        param.put("bUserId", friendUserId);
        List<FriendRelPo> fl=friendRelDao.queryForList(param);
        if (fl==null||fl.isEmpty()) {
            ret.put("ReturnType", "1005");
            ret.put("Message", "不是好友，不必删除");
        } else {//删除好友信息和邀请信息
            friendRelDao.delete("deleteByParam", param);
            inviteFriendDao.delete("deleteByParam", param);
            param.clear();
            param.put("aUserId", friendUserId);
            param.put("bUserId", userId);
            friendRelDao.delete("deleteByParam", param);
            inviteFriendDao.delete("deleteByParam", param);
            //删除好友的别名信息
            UserAliasKey uak=new UserAliasKey("FRIEND", userId, friendUserId);
            userAliasService.del(uak);
            ret.put("ReturnType", "1001");
            //发送消息
            MsgNormal bMsg=new MsgNormal();
            bMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            bMsg.setFromType(1);
            bMsg.setMsgType(1);
            bMsg.setAffirm(0);
            bMsg.setBizType(0x04);
            bMsg.setCmdType(1);
            bMsg.setCommand(5);//处理组邀请信息
            //发送给inviteUserId
            bMsg.setToType(0);
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("UserId", userId);
            dataMap.put("DealTime", System.currentTimeMillis());
            MapContent mc=new MapContent(dataMap);
            bMsg.setMsgContent(mc);
            pmm.getSendMemory().addMsg2NotifyQueue(friendUserId, bMsg);
        }
        return ret;
    }

    /**
     * 得到好友列表
     * @param userId 我的用户Id
     * @return 好友用户列表，除了用户信息外
     */
    public List<UserPo> getFriendList(String userId) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return null;
        try {
            Map<String, Object> param=new HashMap<String, Object>();
            param.put("userId", userId);
            List<UserPo> ret=userDao.queryForList("getFriends", param);
            if (ret!=null&&ret.size()>0) return ret;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 修改用户信息
     * @param updateUserMap 需要的数据
     * @return 200不是好友；300参数不完整;-1别名或别名描述为空,-2主用户不存在;-3别名用户不存在,1新增成功;2修改成功;0不需要保存
     */
    public int updateFriendInfo(Map<String, String> updateUserMap) {
        String mainUserId=updateUserMap.get("mainUserId");
        String friendUserId=updateUserMap.get("friendUserId");
        String alias=updateUserMap.get("alias");
        String aliasDescn=updateUserMap.get("aliasDescn");

        if (StringUtils.isNullOrEmptyOrSpace(mainUserId)||StringUtils.isNullOrEmptyOrSpace(friendUserId)||(StringUtils.isNullOrEmptyOrSpace(alias)&&StringUtils.isNullOrEmptyOrSpace(aliasDescn))) return 300;

        Map<String, Object> param=new HashMap<String, Object>();
        param.put("aUserId", mainUserId);
        param.put("bUserId", friendUserId);
        List<FriendRelPo> fl=friendRelDao.queryForList(param);
        if (fl==null||fl.size()==0) return 200;

        //修改或新增
        UserAliasPo uaPo=new UserAliasPo();
        uaPo.setTypeId("FRIEND");
        uaPo.setMainUserId(mainUserId);
        uaPo.setAliasUserId(friendUserId);
        uaPo.setAliasName(alias);
        uaPo.setAliasDescn(aliasDescn);
        return userAliasService.save(uaPo);
    }
}