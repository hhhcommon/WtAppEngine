package com.woting.mobile.push.model;

import java.io.Serializable;

/**
 * 发送或接收到的消息，包括数据和标识信息。<br/>
 * 标识信息类似协议中的包头；<br/>
 * 数据类似协议中的包体；这里规定，包体必须是json串。
 * @author wanghui
 */
/*
 * 消息地址说明：这里的地址只说明设备以及设备上的可分别的状态
 * 标识=>字符串
 * #分类=>字符串
 * 设备标识=>字符串
 * 设备分类=>字符串，目前只有S和M，S表示服务器，M表示移动端
 * 
 * #设备状态=标识||分类
 * 地址={(设备状态)@@(设备标识||设备分类)[地址别名]}
 * #地址别名=地址
 * 地址组=地址,地址,...,地址
 *
 * 特别：
 * 1-地址串保留字——{ } ( ) :: || ,
 * 2-前面带#是可选的属性，若地址不包含这个属性，则此属性前的连接保留字也要省略
 *
 * 例子：
 * 单地址1—{(abf31988c5e2||wt)@@(187020EEE01FC027B3218BA69FEB7D0D||M)}，由UserId和Imei组成的地址
 * 单地址2—{()@@(187020EEE01FC027B3218BA69FEB7D0D||M)}，仅由Imei组成的地址
 * 单地址3—{(intercom)@@(www.woting.com||S)}，我听科技服务器，对讲程序
 * 复杂地址4—{(intercom)@@(www.woting.com||S)[{(tomcat::8090)@@(192.34.23.4||S)}]}，我听服务器对讲程序，也就是192.34.23.4服务器上的端口号为8090的程序是Tomcat
 */
public class Message implements Serializable, Comparable<Message> {
    private transient static final long serialVersionUID = -8842284912678595536L;
    private String msgId; //32位消息id
    private String reMsgId; //32位消息id

    private String fromAddr; //消息发送地址
    private String proxyAddrs; //消息中转地址，用逗号隔开
    private String toAddr; //消息目标地址

    private int msgType; //消息类型:0是一般类型；1是回复类型
    private int affirem; //是否需要确认;0不需要1需要，默认值=0不需要确认
    private String returnType; //返回值类型

    private String msgBizType; //业务消息类型，根据此类型，框架会对消息进行分发，目前只有对讲一个业务类型"INTERCOM"
    private String cmdType; //命令类型
    private String command; //命令编号

    private long receiveTime; //消息收到时间
    private long sendTime; //消息应答发送时间
    private Object msgContent; //消息内容}

    public String getMsgId() {
        return msgId;
    }
    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
    public String getReMsgId() {
        return reMsgId;
    }
    public void setReMsgId(String reMsgId) {
        this.reMsgId = reMsgId;
    }

    public String getFromAddr() {
        return fromAddr;
    }
    public void setFromAddr(String fromAddr) {
        this.fromAddr = fromAddr;
    }
    public String getProxyAddrs() {
        return proxyAddrs;
    }
    public void setProxyAddrs(String proxyAddrs) {
        this.proxyAddrs = proxyAddrs;
    }
    public String getToAddr() {
        return toAddr;
    }
    public void setToAddr(String toAddr) {
        this.toAddr = toAddr;
    }

    public int getMsgType() {
        return msgType;
    }
    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }
    public int getAffirem() {
        return affirem;
    }
    public void setAffirem(int affirem) {
        this.affirem = affirem;
    }
    public String getReturnType() {
        return returnType;
    }
    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getMsgBizType() {
        return msgBizType;
    }
    public void setMsgBizType(String msgBizType) {
        this.msgBizType = msgBizType;
    }
    public String getCmdType() {
        return cmdType;
    }
    public void setCmdType(String cmdType) {
        this.cmdType = cmdType;
    }
    public String getCommand() {
        return command;
    }
    public void setCommand(String command) {
        this.command = command;
    }

    public long getReceiveTime() {
        return receiveTime;
    }
    public void setReceiveTime(long receiveTime) {
        this.receiveTime = receiveTime;
    }
    public long getSendTime() {
        return sendTime;
    }
    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }

    public Object getMsgContent() {
        return msgContent;
    }
    public void setMsgContent(Object msgContent) {
        this.msgContent = msgContent;
    }

    /**
     * 该条消息是否需要确认
     * @return 需要确认返回true，否则返回false
     */
    public boolean isAffirm() {
        return this.affirem==1;
    }

    /**
     * 用于消息排序
     */
    @Override
    public int compareTo(Message o) {
        long flag=this.sendTime-o.getSendTime();
        if (flag==0) return 0;
        if (flag>0) return 1;
        return -1;
    }

    /**
     * 是否是同类的消息
     * @param m 另一消息
     * @return
     */
    public boolean isSeamType(Message m) {
        if (this.fromAddr.equals(m.fromAddr)
          &&this.toAddr.equals(m.toAddr)
          &&this.msgBizType.equals(m.msgBizType)
          &&this.cmdType.equals(m.cmdType)
          &&this.command.equals(m.command)) return true;
        return false;
    }
}