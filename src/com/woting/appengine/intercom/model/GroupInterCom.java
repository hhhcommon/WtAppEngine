package com.woting.appengine.intercom.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.push.core.message.CompareMsg;
import com.woting.push.core.message.Message;
import com.woting.passport.UGA.model.Group;
import com.woting.passport.UGA.persistence.pojo.UserPo;

/**
 * 对讲用户组结构
 * @author wanghui
 */
public class GroupInterCom {
    //组信息
    private Group group;
    //进入该组的用户列表
    private Map<String, UserPo> entryGroupUserMap;
    //组内发言人
    private UserPo speaker;//用于组控制
    //最后发言时间，用于清除发言人
    private long lastTalkTime=-1;
    //是否已经发送了结束对讲的消息
    public AtomicBoolean isSendEndPPTMsg=new AtomicBoolean(false);

    public Group getGroup() {
        return group;
    }
    public Map<String, UserPo> getEntryGroupUserMap() {
        return entryGroupUserMap;
    }

    public long getLastTalkTime() {
        return lastTalkTime;
    }
    public void setLastTalkTime(String userId) {
        if (this.speaker!=null&&userId.equals(this.speaker.getUserId())) {
            this.lastTalkTime=System.currentTimeMillis();
        }
    }

    /**
     * 构造函数，根据用户组模型构造对讲用户组结构
     * @param g 用户组模型
     */
    public GroupInterCom(Group g) {
        this.group=g;
        this.entryGroupUserMap=new HashMap<String, UserPo>();
        this.speaker=null;
    }

    /**
     * 得到当前对讲者
     * @return 若无人对讲，返回空
     */
    synchronized public UserPo getSpeaker() {
        return speaker;
    }
    /**
     * 设置对讲者，当且仅当，当前无对讲者时，才能设置成功
     * @param speaker 新对讲者ID
     * @return 返回的是Map类型若设置成功，设置新的对讲者，返回<"T",null>
     *          若新对讲者不在用户组，不允许设置，返回<"E",null>
     *          若原对讲者不为空，不允许设置，返回对讲者对象
     *          若原对讲者不为空，不允许设置，返回<"O",null>，只有<"F",this.speaker>，原一个人在对讲组
     */
    synchronized public Map<String, UserPo> setSpeaker(MobileKey speakerKey) {
        Map<String, UserPo> ret = new HashMap<String, UserPo>();
        if (this.entryGroupUserMap.size()==0) ret.put("E", null);
        else {
            UserPo _speaker=this.entryGroupUserMap.get(speakerKey.toString());
            if (_speaker==null) ret.put("E", null);
            else {
                //为测试，把判断Speaker的功能去掉：
                /*
                {
                    this.speaker=_speaker;
                    ret.put("T", speaker);
                    this.lastTalkTime=new Date(System.currentTimeMillis());
                }*/
                if (this.speaker!=null) ret.put("F", this.speaker);
                else if (this.entryGroupUserMap.size()<2) ret.put("O", null);
                else {
                    this.speaker=_speaker;
                    ret.put("T", speaker);
                    this.isSendEndPPTMsg.lazySet(false);
                }
            }
        }
        return ret;
    }
    /**
     * 结束对讲
     * @param speakerMk 需要退出对讲的对讲人
     * @return 若当前不存在对讲人，返回-1；若当前对讲人与需要退出者为同一人，则清空当前对讲人，返回1；若当前对讲人与需要退出者不同，则返回0；
     */
    synchronized public int endPTT(MobileKey speakerMk) {
        if (this.speaker==null) return -1;
        if (speakerMk.getUserId().equals(this.speaker.getUserId())) {
            //this.speaker=null;
            return 1;
        } else  return 0;
    }

    /**
     * 把用户加入组进入Map
     * @param mk 用户标识
     * @return
     *   returnType=1成功；2用户已在加入组Map，无需再次加入；3用户不在用户组
     *   entryGroupUsers=返回处理后的进入组用户Map；(当且仅当returnType=1)
     *   needBroadCast=是否需要广播消息；(当且仅当returnType=1)
     */
    synchronized public Map<String, Object> insertEntryUser(MobileKey mk) {
        return toggleEntryUser(mk, 0);
    }
    /**
     * 把用户剔出组进入Map
     * @param mk 用户标识
     * @return
     *   returnType=1成功；2用户不在进入组列表，无需剔出；3用户不在用户组
     *   entryGroupUsers=返回处理后的进入组用户Map；(当且仅当returnType=1)
     *   needBroadCast=是否需要广播消息；(当且仅当returnType=1)
     */
    synchronized public Map<String, Object> delEntryUser(MobileKey mk) {
        return toggleEntryUser(mk, 1);
    }

    /*
     * 切换用户，用户组的进入与退出
     * @param mk 用户标识
     * @param type 0=进入;1=退出
     * @return
     *   returnType=1成功；2组状态不正确；3用户不在用户组
     *   entryGroupUsers=返回处理后的进入组用户Map；(当且仅当returnType=1)
     *   needBroadCast=是否需要广播消息；(当且仅当returnType=1)
     */
    synchronized private Map<String, Object> toggleEntryUser(MobileKey mk, int type) {
        Map<String, Object> retM=new HashMap<String, Object>();

        UserPo entryUp=null;
        List<UserPo> _tl=this.group.getUserList();
        if (_tl==null||_tl.size()==0) return null;
        //判断加入的用户是否属于这个组
        boolean exist=false;
        for (UserPo up: _tl) {
            if (mk.getUserId().equals(up.getUserId())) {
                exist=true;
                entryUp=up;
                break;
            }
        }
        if (!exist) {
            retM.put("returnType", "3");
            return retM;
        }

        //用户在加入组Map的状态
        retM.put("returnType", "2");
        exist=entryGroupUserMap.containsKey(mk.toString());

        if (!exist&&type==0) {//进入组处理
            int oldSize=entryGroupUserMap.size();
            String delKey=null;
            for (String key: this.entryGroupUserMap.keySet()) {
                if (key.indexOf("::"+mk.getUserId())!=-1) {
                    delKey=key;
                    break;
                }
            }
            if (delKey!=null) this.entryGroupUserMap.remove(delKey);
            this.entryGroupUserMap.put(mk.toString(), entryUp);
            retM.put("returnType", "1");
            retM.put("entryGroupUsers", this.cloneEntryGroupUserMap());
            if (oldSize>0) retM.put("needBroadCast", "1");
        } else if (exist&&type==1) {//退出组处理
            this.entryGroupUserMap.remove(mk.toString());
            retM.put("returnType", "1");
            retM.put("entryGroupUsers", this.cloneEntryGroupUserMap());
            if (this.entryGroupUserMap.size()>0) retM.put("needBroadCast", "1");
        }
        return retM;
    }
    private Map<String, UserPo> cloneEntryGroupUserMap() {
        Map<String, UserPo> _rm = new HashMap<String, UserPo>();
        for (String k: entryGroupUserMap.keySet()) {
            _rm.put(k, entryGroupUserMap.get(k));
        }
        return _rm;
    }
    /**
     * 删除对讲者
     */
    synchronized public void delSpeaker(String userId) {
        if (this.speaker!=null&&userId.equals(this.speaker.getUserId())) {
            this.speaker=null;
            this.lastTalkTime=-1;
        }
    }

    /**
     * 发送结束对讲的广播消息
     * @param gic
     * @param talkerId
     * @param groupId
     * @param mk
     * @param wt
     */
    public void sendEndPTT() {
        if (!this.isSendEndPPTMsg.get()) {
            System.out.println("===========对讲结束：释放Speaker资源：广播结束PPT的消息===============");
            PushMemoryManage pmm=PushMemoryManage.getInstance();
            //广播结束消息
            Message exitPttMsg=new Message();
            exitPttMsg.setFromAddr("{(intercom)@@(www.woting.fm||S)}");
            exitPttMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
            exitPttMsg.setMsgType(1);
            exitPttMsg.setMsgBizType("INTERCOM_CTL");
            exitPttMsg.setCmdType("PTT");
            exitPttMsg.setCommand("b2");
            Map<String, Object> dataMap=new HashMap<String, Object>();
            dataMap.put("GroupId", this.getGroup().getGroupId());
            dataMap.put("TalkUserId", this.speaker.getUserId());
            exitPttMsg.setMsgContent(dataMap);
            //发送广播消息
            Map<String, UserPo> entryGroupUsers=this.getEntryGroupUserMap();
            for (String k: entryGroupUsers.keySet()) {
                String _sp[] = k.split("::");
                MobileKey mk=new MobileKey();
                mk.setMobileId(_sp[0]);
                mk.setPCDType(Integer.parseInt(_sp[1]));
                mk.setUserId(_sp[2]);
                exitPttMsg.setToAddr(MobileUtils.getAddr(mk));
                pmm.getSendMemory().addUniqueMsg2Queue(mk, exitPttMsg, new CompareGroupMsg());
            }
            this.isSendEndPPTMsg.lazySet(true);
        }
    }
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