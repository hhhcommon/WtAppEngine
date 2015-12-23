package com.woting.intercom.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.intercom.mem.GroupMemoryManage;
import com.woting.mobile.push.mem.PushMemoryManage;
import com.woting.mobile.push.model.Message;

public class DealInterCom extends Thread {
    private PushMemoryManage pmm=PushMemoryManage.getInstance();
    private GroupMemoryManage gmm=GroupMemoryManage.getInstance();

    /**
     * 给线程起一个名字的构造函数
     * @param name 线程名称
     */
    public DealInterCom(String name) {
        super("对讲过程监听处理线程"+((name==null||name.trim().length()==0)?"":"::"+name));
    }

    @Override
    public void run() {
        System.out.println(this.getName()+"开始执行");
        String tempStr = "";
        while(true) {
            try {
                sleep(10);
                //读取Receive内存中的typeMsgMap中的内容
                Message m = pmm.getReceiveMemory().pollTypeQueue("INTERCOM_CTL");
                if (m==null) continue;
                Map<String, Object> content = (Map<String, Object>)JsonUtils.jsonToObj(m.getMsgContent(), Map.class);
                if ((content.get("CmdType")+"").equals("GROUP")) {
                    if ((content.get("Command")+"").equals("1")) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-用户进入组::(User=("+m.getFromAddr()+");Group="+content.get("GroupId")+")";
                        System.out.println(tempStr);
                        (new EntryGroup("{"+tempStr+"}处理线程", m, content)).start();
                    } else if ((content.get("Command")+"").equals("-1")) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-用户退出组::(User=("+m.getFromAddr()+");Group="+content.get("GroupId")+")";
                        System.out.println(tempStr);
                        (new ExitGroup("{"+tempStr+"}处理线程", m, content)).start();
                    }
                } else if ((content.get("CmdType")+"").equals("PTT")) {
                    if ((content.get("Command")+"").equals("1")) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-开始对讲::(User=("+m.getFromAddr()+");Group="+content.get("GroupId")+")";
                        System.out.println(tempStr);
                        (new BeginPTT("{"+tempStr+"}处理线程", m, content)).start();
                    } else if ((content.get("Command")+"").equals("-1")) {
                        tempStr="处理消息[MsgId="+m.getMsgId()+"]-结束对讲::(User=("+m.getFromAddr()+");Group="+content.get("GroupId")+")";
                        System.out.println(tempStr);
                        (new EndPTT("{"+tempStr+"}处理线程", m, content)).start();
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * 进入组处理
     */
    class EntryGroup extends Thread {
        private Message sourceMsg;//源消息
        private Map<String, Object> data;//源消息数据字段内容
        protected EntryGroup(String name, Message sourceMsg, Map<String, Object> data) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
            this.data=data;
        }
        public void run() {
            //这只是一个Demo，传给发送对接
            //以下为测试：看看是否能够发送出去
            if (sourceMsg!=null) {
                Message retMsg=new Message();
                retMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                retMsg.setReMsgId(sourceMsg.getMsgId());
                retMsg.setMsgBizType(sourceMsg.getMsgBizType());
                retMsg.setToAddr(sourceMsg.getFromAddr());
                retMsg.setFromAddr(sourceMsg.getToAddr());
                retMsg.setSendTime(System.currentTimeMillis());
                retMsg.setMsgContent("");
                retMsg.setReturnType("1001");
                Map<String, Object> _m=new HashMap<String, Object>();
                _m.put("CmdType", "GROUP");
                _m.put("Command", "-1");
                _m.put("GroupId", data.get("GroupId"));
                List<Map<String, String>> _t=new ArrayList<Map<String, String>>();
                Map<String, String> _tm=new HashMap<String, String>();
                _t.add(_tm);
                _tm.put("UserId", "123456");
                _tm.put("UserName", "张先生1");
                _tm.put("InnerPhoneNum", "1001");
                _tm.put("Portrait", "images/person.png");
                _t.add(_tm);
                _tm=new HashMap<String, String>();
                _tm.put("UserId", "123453");
                _tm.put("UserName", "张先生2");
                _tm.put("InnerPhoneNum", "1002");
                _tm.put("Portrait", "images/person1.png");
                _t.add(_tm);
                _m.put("InGroupUsers", _t);
                retMsg.setMsgContent(JsonUtils.objToJson(_m));
                pmm.getSendMemory().addMsg2Queue(null, retMsg);
            }
        }
    }
    /*
     * 退出组处理
     */
    class ExitGroup extends Thread {
        private Message sourceMsg;//源消息
        private Map<String, Object> data;//源消息数据字段内容
        protected ExitGroup(String name, Message sourceMsg, Map<String, Object> data) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
            this.data=data;
        }
    }

    /*
     * 进入组处理
     */
    class BeginPTT extends Thread {
        private Message sourceMsg;//源消息
        private Map<String, Object> data;//源消息数据字段内容
        protected BeginPTT(String name, Message sourceMsg, Map<String, Object> data) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
            this.data=data;
        }
    }
    /*
     * 退出组处理
     */
    class EndPTT extends Thread {
        private Message sourceMsg;//源消息
        private Map<String, Object> data;//源消息数据字段内容
        protected EndPTT(String name, Message sourceMsg, Map<String, Object> data) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
            this.data=data;
        }
    }
}