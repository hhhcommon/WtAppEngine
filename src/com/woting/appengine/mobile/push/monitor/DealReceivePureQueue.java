package com.woting.appengine.mobile.push.monitor;

//import java.util.HashMap;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.appengine.mobile.push.mem.ReceiveMemory;
//import com.woting.appengine.mobile.push.mem.SendMemory;
import com.woting.push.core.message.Message;
import com.woting.push.core.message.MessageUtils;
import com.woting.push.core.message.MsgMedia;
import com.woting.push.core.message.MsgNormal;

/**
 * 处理收到的纯净数据
 * @author wanghui
 */
public class DealReceivePureQueue extends Thread {
    private PushMemoryManage pmm=PushMemoryManage.getInstance();

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
        while (true) {
            try {
                sleep(10);
                ReceiveMemory rm=pmm.getReceiveMemory();
                Message m=rm.pollPureQueue(); //执行后，原始消息接收队列中将不再有此消息
                if (m==null) continue;
//                Map<String, Object> parseM=this.getMsgFromMap4CTL(m);
//                Message msg=null;
//                if (parseM.get("err")!=null) {//直接写入发送队列
//                    String __tmp=(String)m.get("NeedAffirm");
//                    boolean isAffirm=__tmp==null?false:__tmp.trim().equals("1");
//                    if (isAffirm) {//只有需要回执确认的消息才进行如下处理：直接写入
//                        SendMemory sm=pmm.getSendMemory();
//                        Message _msg=new Message();
//                        _msg.setMsgContent("{\"returnType\":\"-2\",\"errMsg\":\""+parseM.get("err")+"\",\"sourceInfo\":{"+m.get("_S_STR")+"}}");
//                        sm.addMsg2Queue(mk, _msg);
//                    }
//                } else {
//                    msg=(Message)parseM.get("msg");
//                }
                if (m.isAffirm()&&!(m instanceof MsgMedia)) {
                    MobileUDKey mUdk=MobileUDKey.buildFromMsg(m);
                    pmm.getSendMemory().addMsg2Queue(mUdk, MessageUtils.buildAckMsg((MsgNormal)m));
                }
                String type=null;
                if (m instanceof MsgMedia) type="media";
                else type=""+((MsgNormal)m).getBizType();
                rm.appendTypeMsgMap(type, m);
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
//    private Map<String, Object> getMsgFromMap4CTL(Map<String, Object> m) {
//        Map<String, Object> retM = new HashMap<String, Object>();
//        Message msg=new Message();
//        String err=null;
//        String __tmp=(String)m.get("NeedAffirm");
//        msg.setAffirm(__tmp==null?0:__tmp.trim().equals("1")?1:0);
//        __tmp=(String)m.get("MsgId");
//        boolean canContinue=true;
//        if (StringUtils.isNullOrEmptyOrSpace(__tmp)) {
//            canContinue=false;
//            err+=","+"消息必须设置标识MsgId";
//        }
//        if (canContinue) {
//            msg.setMsgId(__tmp);
//            msg.setReMsgId(m.get("ReMsgId")+"");
//            MobileKey mk=MobileUtils.getMobileKey(m);
//            msg.setFromAddr(MobileUtils.getAddr(mk));
//            msg.setProxyAddrs("");
//            __tmp="";
//            if ((m.get("BizType")+"").equals("INTERCOM_CTL")) __tmp="intercom";
//            if ((m.get("BizType")+"").equals("AUDIOFLOW")) __tmp="audioflow";
//            if ((m.get("BizType")+"").equals("CALL_CTL")) __tmp="phone";
//            msg.setToAddr("{("+__tmp+")@@(www.woting.fm||S)}");
//            msg.setMsgType(Integer.parseInt(m.get("MsgType")+""));
//            msg.setSendTime(Long.parseLong(m.get("SendTime")+""));
//            msg.setReceiveTime(System.currentTimeMillis());
//
//            msg.setMsgBizType(m.get("BizType")+"");
//            msg.setCmdType(m.get("CmdType")+"");
//            msg.setCommand(m.get("Command")+"");
//            msg.setReturnType(m.get("ReturnType")+"");
//            msg.setMsgContent(m.get("Data"));
//        }
//        if (StringUtils.isNullOrEmptyOrSpace(err)) retM.put("msg", msg);
//        else retM.put("err", err);
//        return retM;
//    }
}