package com.woting.intercom.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private List<UserPo> entryGroupUserList;
    //组内发言人
    private UserPo speaker;

    public Group getGroup() {
        return group;
    }
    public List<UserPo> getEntryGroupUserList() {
        return entryGroupUserList;
    }

    /**
     * 构造函数，根据用户组模型构造对讲用户组结构
     * @param g 用户组模型
     */
    public GroupInterCom(Group g) {
        this.group=g;
        this.entryGroupUserList=new ArrayList<UserPo>();
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
        if (this.entryGroupUserList.size()==0) {
            ret.put("E", null);
            return ret;
        } else {
            boolean exist=false;
            for (UserPo u: this.entryGroupUserList) {
                if (u.getUserId().equals(speaker.getUserId())) {
                    exist=true;
                    break;
                }
            }
            if (!exist) {
                ret.put("E", null);
                return ret;
            }
        }
        if (this.speaker!=null) ret.put("F", this.speaker);
        else {
            this.speaker=speaker;
            ret.put("T", speaker);
        }
        return ret;
    }
    /**
     * 删除当前对讲者，退出对讲
     * @param speaker 需要退出对讲的对讲人
     * @return 若当前不存在对讲人，返回-1；若当前对讲人与需要退出者为同一人，则清空当前对讲人，返回1；若当前对讲人与需要退出者不同，则返回0；
     */
    synchronized int removeSpeaker(UserPo speaker) {
        if (this.speaker==null) return -1;
        if (speaker.getUserId().equals(this.speaker.getUserId())) {
            this.speaker=null;
            return 1;
        } else  return 0;
    }
}