package com.woting.intercom.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.woting.mobile.model.MobileKey;
import com.woting.mobile.push.model.Message;
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
    private Map<MobileKey, UserPo> entryGroupUserMap;
    //组内发言人
    private UserPo speaker;

    public Group getGroup() {
        return group;
    }
    public Map<MobileKey, UserPo> getEntryGroupUserMap() {
        return entryGroupUserMap;
    }

    /**
     * 构造函数，根据用户组模型构造对讲用户组结构
     * @param g 用户组模型
     */
    public GroupInterCom(Group g) {
        this.group=g;
        this.entryGroupUserMap=new HashMap<MobileKey, UserPo>();
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
     * @param speaker 对讲者
     * @return 若设置成功，返回的Map键为"T"，值为新设置的对讲者；若设置不成功，返回的Map键为"F"，值为原来的对讲者，若当前对讲者不在用户组，若改用户不在此组在线名单，则不允许设置，返回<"E",null>
     */
    synchronized public Map<String, UserPo> setSpeaker(UserPo speaker) {
        Map<String, UserPo> ret = new HashMap<String, UserPo>();
        if (this.entryGroupUserMap.size()==0) ret.put("E", null);
        else {
            boolean exist=false;
            for (MobileKey k: this.entryGroupUserMap.keySet()) {
                if (k.getUserId().equals(speaker.getUserId())) {
                    exist=true;
                    break;
                }
            }
            if (!exist) ret.put("E", null);
            else {
                if (this.speaker!=null) ret.put("F", this.speaker);
                else {
                    this.speaker=speaker;
                    ret.put("T", speaker);
                }
            }
        }
        return ret;
    }
    /**
     * 删除当前对讲者，退出对讲
     * @param speaker 需要退出对讲的对讲人
     * @return 若当前不存在对讲人，返回-1；若当前对讲人与需要退出者为同一人，则清空当前对讲人，返回1；若当前对讲人与需要退出者不同，则返回0；
     */
    synchronized int removeSpeaker(MobileKey mk) {
        if (this.speaker==null) return -1;
        if (mk.getUserId().equals(this.speaker.getUserId())) {
            this.speaker=null;
            return 1;
        } else  return 0;
    }

    /**
     * 把用户加入组进入Map
     * @return 
     *   若成功，返回加入后的进入组用户Map；<br/>
     *   若用户已经在加入组列表，返回空列表；<br/>
     *   若用户不在用户组返回null
     */
    synchronized public Map<MobileKey, UserPo> insertEntryUser(MobileKey mk) {
        return toggleEntryUser(mk, 0);
    }

    /**
     * 把用户剔出组进入Map
     * @return 
     *   若成功，返回剔出后的进入组用户Map；<br/>
     *   若用户不在进入组列表，无需剔出，返回空列表；<br/>
     *   若用户不在用户组返回null
     */
    synchronized public Map<MobileKey, UserPo> delEntryUser(MobileKey mk) {
        return toggleEntryUser(mk, 1);
    }
    /*
     * 切换用户，
     * @param mk 用户标识
     * @param type 0=进入;1=退出
     * @return 
     *   若成功，返回处理后的进入组用户Map；<br/>
     *   若用户在加入组Map中的状态于Type不匹配，则返回空列表；<br/>
     *   若用户不在用户组返回null
     */
    synchronized public Map<MobileKey, UserPo> toggleEntryUser(MobileKey mk, int type) {
        UserPo entryUp=null;
        List<UserPo> _tl = this.group.getUserList();
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
        if (!exist) return null;

        Map<MobileKey, UserPo> _tm=new HashMap<MobileKey, UserPo>();
        if (this.entryGroupUserMap.size()>0) {
            //用户在加入组Map的状态
            exist=false;
            for (MobileKey k: this.entryGroupUserMap.keySet()) {
                if (k.getUserId().equals(speaker.getUserId())) {
                    exist=true;
                    break;
                }
            }
            if (!exist&&type==0) {//进入组处理
                this.entryGroupUserMap.put(mk, entryUp);
                _tm = this.entryGroupUserMap;
            } else if (exist&&type==1) {//退出组处理
                this.entryGroupUserMap.remove(mk);
                _tm = this.entryGroupUserMap;
            }
        }
        return _tm;
    }
}