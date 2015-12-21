package com.woting.mobile.push.monitor;

import java.util.HashMap;
import java.util.Map;

import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.mobile.MobileUtils;
import com.woting.mobile.model.MobileKey;
import com.woting.mobile.push.mem.PushMemoryManage;
import com.woting.mobile.push.mem.ReceiveMemory;
import com.woting.mobile.push.mem.SendMemory;
import com.woting.mobile.push.model.Message;

/**
 * 
 * @author wanghui
 */
public class DealReceivePureQueue extends Thread {

    private PushMemoryManage mpp=PushMemoryManage.getInstance();
    /**
     * 给线程起一个名字的构造函数
     * @param name 线程名称
     */
    public DealReceivePureQueue(String name) {
        super("原生消息接收队列处理线程"+((name==null||name.trim().length()==0)?"":"::"+name));
    }

    @Override
    public void run() {
        System.out.println(this.getName()+"开始执行");
        for (;;) {
            try {
                ReceiveMemory rm=mpp.getReceiveMemory();
                Map<String, Object> m=rm.pollPureQueue(); //执行后，原始消息接收队列中将不再有此消息
                if (m==null) continue;
                MobileKey mk=MobileUtils.getMobileKey(m);
                Map<String, Object> parseM=this.getMsgFromMap4GroupCTL(m);
                Message msg=null;
                if (parseM.get("err")!=null) {//直接写入发送队列
                    String __tmp=(String)m.get("NeedAffirm");
                    boolean isAffirm=__tmp==null?false:__tmp.trim().equals("1");
                    if (isAffirm) {//只有需要回执确认的消息才进行如下处理：直接写入
                        SendMemory sm=mpp.getSendMemory();
                        Message _msg=new Message();
                        _msg.setMsgContent("{\"returnType\":\"-2\",\"errMsg\":\""+parseM.get("err")+"\",\"sourceInfo\":{"+m.get("_S_STR")+"}}");
                        sm.addMsg2Queue(mk, _msg);
                    }
                } else {
                    msg=(Message)parseM.get("msg");
                }
                if (msg!=null) rm.addTypeMsgMap(msg.getMsgBizType(), msg);
                //以下为测试：看看是否能够发送出去
                if (msg!=null) {
                    SendMemory sm=mpp.getSendMemory();
                    Message retMsg=new Message();
                    retMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
                    retMsg.setReMsgId(msg.getMsgId());
                    retMsg.setMsgBizType(msg.getMsgBizType());
                    retMsg.setToAddr(msg.getFromAddr());
                    retMsg.setFromAddr(msg.getToAddr());
                    retMsg.setSendTime(System.currentTimeMillis());
                    retMsg.setMsgContent("");
                    retMsg.setReturnType("1001");
                    Map<String, Object> _m=new HashMap<String, Object>();
                    _m.put("CmdType", "Group");
                    _m.put("Command", "-1");
                    _m.put("GroupId", ((Map)m.get("Date")).get("GroupId"));
                    List<Map<String, String>> 
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * 把Map对象转换为消息对象，若转换失败，返回null 
     * @param m 消息原始数据结构的Map
     * @return
     */
    private Map<String, Object> getMsgFromMap4GroupCTL(Map<String, Object> m) {
        Map<String, Object> retM = new HashMap<String, Object>();
        Message msg=new Message();
        String err=null;
        String __tmp=(String)m.get("NeedAffirm");
        msg.setAffirem(__tmp==null?0:__tmp.trim().equals("1")?1:0);
        __tmp=(String)m.get("MsgId");
        boolean canContinue=true;
        if (StringUtils.isNullOrEmptyOrSpace(__tmp)) {
            canContinue=false;
            err+=","+"消息必须设置标识MsgId";
        }
        if (canContinue) {
            msg.setMsgId(__tmp);
            MobileKey mk=MobileUtils.getMobileKey(m);
            if (mk.isMobileSession()) {
                __tmp="{()@@("+mk.getMobileId()+"||M)}";
            } else {
                __tmp="{("+mk.getUserId()+"||wt)@@("+mk.getMobileId()+"||M)}";
            }
            msg.setFromAddr(__tmp);
            msg.setProxyAddrs("");
            msg.setToAddr("{(intercom)@@(www.woting.fm||S)}");
            msg.setMsgBizType(m.get("BizType")+"");
            msg.setMsgType(0);
            msg.setReceiveTime(System.currentTimeMillis());
            __tmp=(m.get("Command")+"").trim();
            canContinue=(__tmp.equals("-1")||__tmp.equals("0")||__tmp.equals("1"));
        }
        if (!canContinue) {
            err+=","+"请求命令不合法";
        } else {
            int _command=Integer.parseInt(__tmp);
            if (_command==-1||_command==1) {
                String _data=m.get("Data")+"";
                try {
                    Map<String, Object> _dm=(Map<String, Object>)JsonUtils.jsonToObj(_data, Map.class);
                    String _groupId=_dm.get("GroupId")+"";
                    if (_groupId.equals("null")) {
                        err=","+"无法获得用户组Id";
                    } else {
                        _groupId="{\"Command\":\""+_command+"\",\"GroupId\":\""+_groupId+"\"}";
                        msg.setMsgContent(_groupId);
                    }
                } catch(Exception e) {
                    canContinue=false;
                    err=","+"无法获得用户组Id，出现异常["+e.getMessage()+"]";
                }
            }
        }
        if (StringUtils.isNullOrEmptyOrSpace(err)) retM.put("msg", msg);
        else retM.put("err", err);
        return retM;
    }
}