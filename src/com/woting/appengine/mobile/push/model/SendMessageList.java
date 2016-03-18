package com.woting.appengine.mobile.push.model;

import java.util.ArrayList;
import java.util.List;

import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.mobile.model.MobileKey;

/**
 * 已发送消息列表。要保证其列中消息指向同一个设备，并且消息定应该是需要确认的消息,m.isAffirm()为真。
 * 存储转发机制的确认。
 * @author wanghui
 */
public class SendMessageList {
    private MobileKey mk=null; //本列表的移动端标识，一个列表只能有一个标识，即此列表只能对应一个设备
    private List<Message> msgList;

    public SendMessageList(MobileKey mk) {
        super();
        this.mk=mk;
        this.msgList = new ArrayList<Message>();
    }

    /**
     * 按发送时间排序加入已发送列表
     * @param m 消息数据
     * @return 插入成功返回true，否则返回false
     * @throws IllegalAccessException 若消息不属于同一设备，则抛出此异常
     */
    public boolean add(Message m) throws IllegalAccessException {
        if (!m.isAffirm()) throw new IllegalAccessException("消息为不需要确认的消息，无需加入");
        if (this.mk!=null&&!this.mk.equals(MobileUtils.getMobileKey(m,1))) throw new IllegalAccessException("不是同一设备的消息，不能加入");

        if (this.msgList.size()==0) {
            if (this.mk==null) this.mk=MobileUtils.getMobileKey(m,1);
            return this.msgList.add(m);
        } else {
            //排序查找，从后向前
            for (int i=this.msgList.size()-1;i>=0; i--) {
                Message _m=this.msgList.get(i);
                if (_m.compareTo(m)<0) {
                    this.msgList.add(i, m);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获得此列表中指定位置上的消息。
     * @param index 要返回消息的索引
     * @return 指定位置上的消息
     */
    public Message get(int index) {
        return this.msgList.get(index);
    }

    /**
     * 返回此列表中的消息总数
     * @return 消息总数
     */
    public int size() {
        return this.msgList.size();
    }

    /**
     * 从消息列表中删除指定序号的消息
     */
    public void remove(int i) {
        this.msgList.remove(i);
    }
}