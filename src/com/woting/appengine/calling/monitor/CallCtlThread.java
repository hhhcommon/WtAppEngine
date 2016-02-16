package com.woting.appengine.calling.monitor;

import com.woting.appengine.calling.mem.CallingMemoryManage;
import com.woting.appengine.calling.model.OneCall;

/**
 * 电话控制线程
 * @author wanghui
 *
 */
public class CallCtlThread extends Thread {
    private OneCall callData;//所控制的通话数据
    private CallingMemoryManage cmm=CallingMemoryManage.getInstance();

    /**
     * 构造函数，必须给定一个通话控制数据
     * @param callData
     */
    public CallCtlThread(OneCall callData) {
        super();
        super.setName("电话控制线程[callId="+callData.getCallId()+"]");
        this.callData = callData;
    }

    /**
     * 控制过程
     * @see java.lang.Thread#run()
     */
    public void run() {
        
    }
}