package com.woting.appengine.calling.monitor;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.appengine.calling.CallingListener;
import com.woting.appengine.calling.mem.CallingMemoryManage;
import com.woting.appengine.calling.model.OneCall;
import com.woting.appengine.common.util.MobileUtils;
import com.woting.appengine.intercom.mem.GroupMemoryManage;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;
import com.woting.appengine.mobile.push.model.Message;
import com.woting.passport.UGA.service.UserService;

/**
 * 处理电话消息，包括把电话消息分发到每一个具体的处理线程
 * @author wanghui
 */
public class DealCalling extends Thread {
    private PushMemoryManage pmm=PushMemoryManage.getInstance();
    private GroupMemoryManage gmm=GroupMemoryManage.getInstance();
    private CallingMemoryManage cmm=CallingMemoryManage.getInstance();

    /**
     * 给线程起一个名字的构造函数
     * @param name 线程名称
     */
    public DealCalling(String name) {
        super("电话消息处理线程"+((name==null||name.trim().length()==0)?"":"::"+name));
    }

    @Override
    public void run() {
        System.out.println(this.getName()+"开始执行");
        String tempStr="";
        while(true) {
            try {
                sleep(10);
                //读取Receive内存中的typeMsgMap中的内容
                Message m=pmm.getReceiveMemory().pollTypeQueue("CALLING_CTL");
                if (m==null||!m.getCmdType().equals("CALL")) continue;

                tempStr="电话控制消息[MsgId="+m.getMsgId()+"]";
                (new ProcessCallMsg("{"+tempStr+"}分发线程", m)).start();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    //具体的处理线程
    class ProcessCallMsg extends Thread {
        private Message sourceMsg;//源消息
        protected ProcessCallMsg(String name, Message sourceMsg) {
            super.setName(name);
            this.sourceMsg=sourceMsg;
        }
        public void run() {
            Message retMsg=buildRetMsg(sourceMsg);
            Map<String, Object> dataMap=new HashMap<String, Object>();

            String callId="";
            try {
                callId+=((Map)sourceMsg.getMsgContent()).get("CallId");
            } catch(Exception e) {}
            //不管任何消息，若CallId为空，则都认为是非法的消息，这类消息不进行任何处理，丢弃掉
            if (callId.equals("")) return;

            OneCall oneCall=null;//通话对象
            MobileKey mk=MobileUtils.getMobileKey(sourceMsg);
            if (sourceMsg.getCommand().equals("1")) {//发起呼叫过程
                String diallorId=((Map)sourceMsg.getMsgContent()).get("DiallorId")+"";
                String callorId=((Map)sourceMsg.getMsgContent()).get("CallorId")+"";
                dataMap.put("CallId", callId);
                dataMap.put("DiallorId", diallorId);
                dataMap.put("CallorId", callorId);
                retMsg.setMsgContent(dataMap);
                //判断是否占线
//TODO ***占线的判断要用索机制，现在先不进行处理。这个比较复杂，涉及到多线程的问题。
                if (diallorId.equals(callorId))  retMsg.setReturnType("10033");
                else
                if (gmm.isTalk(callorId)) retMsg.setReturnType("10032");
                else
                if (cmm.isTalk(callorId)) retMsg.setReturnType("10031");
                if (retMsg.getReturnType()!=null&&!retMsg.getReturnType().equals("")) {//占线返回
                    pmm.getSendMemory().addMsg2Queue(mk, retMsg);
                    return;
                }
                //判断被叫者是否存在
                //从Spring中得到FriendService
                Object u=null;
                ServletContext sc = (ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent();
                if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                    UserService us = (UserService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("userService");
                    try {u=us.getUserById(callorId);} catch(Exception e){};
                }
                if (u==null) {//被叫者不存在
                    retMsg.setReturnType("1002");
                    pmm.getSendMemory().addMsg2Queue(mk, retMsg);
                    return;
                }
                
                //创建内存对象
                oneCall=new OneCall(callId, diallorId, callorId, CallingListener.getCallingConfig().getIT1_EXPIRE(), CallingListener.getCallingConfig().getIT2_EXPIRE());
                //加入内存
                int addFlag=CallingMemoryManage.getInstance().addOneCall(oneCall);
                if (addFlag!=1) {//返回错误信息
                    retMsg.setReturnType(addFlag==0?"1004":"1005");
                    pmm.getSendMemory().addMsg2Queue(mk, retMsg);
                    return;
                }
                oneCall.addPreMsg(sourceMsg);//设置第一个消息
                //启动处理进程
                CallCtlThread cct=new CallCtlThread(oneCall);
                cct.start();
            } else {//其他消息，放到具体的独立处理线程中处理
                //查找是否有对应的内存数据，如果没有，则说明通话已经结束，告诉传来者
                oneCall=cmm.getCallData(callId);
                if (oneCall==null) {
                    retMsg.setCommand("b3");
                    retMsg.setMsgType(-1);
                    dataMap.put("ServerMsg", "服务器处理进程不存在");
                    pmm.getSendMemory().addMsg2Queue(mk, retMsg);
                    return;
                }
                oneCall.addPreMsg(sourceMsg);//把消息压入队列
            }
        }
    }

    /*
     * 根据原消息，生成返回消息的壳 
     * @param msg
     * @return 返回消息壳
     */
    private Message buildRetMsg(Message msg) {
        Message retMsg=new Message();

        retMsg.setMsgId(SequenceUUID.getUUIDSubSegment(4));
        retMsg.setReMsgId(msg.getMsgId());
        retMsg.setToAddr(msg.getFromAddr());
        retMsg.setFromAddr(msg.getToAddr());
        retMsg.setMsgType(-1);//是应答消息
        retMsg.setAffirm(0);//不需要回复
        retMsg.setMsgBizType(msg.getMsgBizType());
        retMsg.setCmdType(msg.getCmdType());

        return retMsg;
    }
}