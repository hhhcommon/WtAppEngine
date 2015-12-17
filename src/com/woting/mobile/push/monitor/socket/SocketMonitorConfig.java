package com.woting.mobile.push.monitor.socket;

/**
 * 套接字监控配置信息，用于内部控制。将来有可能需要根据不同情况来配置。<br/>
 * 在设置类，所有字段前都有_
 * @author wanghui
 */
public class SocketMonitorConfig {
    //以下主控线程控制参数
    private long _TimeOut=60000; //多长时间没有收到信息，若大于此时间没有获得信息，则系统认为Socket已经失效，将关闭相应的处理
    private long _MonitorDelay=1000; //主监控进程监控周期
    private long _TryDestoryAllCount=100; //尝试销毁次数，大于此数量仍未达到销毁条件，则强制销毁
    //以下分线程控制参数
    private long _BeatDelay=1000; //多长时间发送一次心跳
    private long _RecieveErr_ContinueCount=3; //接收消息处理中，连续收到错误|异常消息的次数，若大于这个数量，则系统将认为此Socket为恶意连接，将关闭相应的处理
    private long _RecieveErr_SumCount=1000; //接收消息处理中，总共收到错误|异常消息的次数，若大于这个数量，则系统将认为此Socket为恶意连接，将关闭相应的处理

    /**
     * 计算过期失效时间，目前只是简单的返回设定值。
     * 今后可能包括更为复杂的根据情况计算得出的过期失效时间
     * @return 失效时间，毫秒数
     */
    public long calculate_TimeOut() {
        return _TimeOut;
    }
    public void set_TimeOut(long _TimeOut) {
        this._TimeOut = _TimeOut;
    }

    public long get_MonitorDelay() {
        return _MonitorDelay;
    }
    public void set_MonitorDelay(long _MonitorDelay) {
        this._MonitorDelay = _MonitorDelay;
    }

    public long get_TryDestoryAllCount() {
        return _TryDestoryAllCount;
    }
    public void set_TryDestoryAllCount(long _TryDestoryAllCount) {
        this._TryDestoryAllCount = _TryDestoryAllCount;
    }

    public long get_BeatDelay() {
        return _BeatDelay;
    }
    public void set_BeatDelay(long _BeatDelay) {
        this._BeatDelay = _BeatDelay;
    }

    public long get_RecieveErr_ContinueCount() {
        return _RecieveErr_ContinueCount;
    }
    public void set_RecieveErr_ContinueCount(long _RecieveErr_ContinueCount) {
        this._RecieveErr_ContinueCount = _RecieveErr_ContinueCount;
    }

    public long get_RecieveErr_SumCount() {
        return _RecieveErr_SumCount;
    }
    public void set_RecieveErr_SumCount(long _RecieveErr_SumCount) {
        this._RecieveErr_SumCount = _RecieveErr_SumCount;
    }
}