package com.woting.appengine.mobile.mediaflow.model;

import java.util.Map;

import com.woting.push.core.message.CompareMsg;
import com.woting.push.core.message.Message;

public class CompareAudioFlowMsg implements CompareMsg {
    public boolean compare(Message msg1, Message msg2) {
        if (msg1.getFromAddr().equals(msg2.getFromAddr())
          &&msg1.getToAddr().equals(msg2.getToAddr())
          &&msg1.getMsgBizType().equals(msg2.getMsgBizType())
          &&msg1.getCmdType().equals(msg2.getCmdType())
          &&msg1.getCommand().equals(msg2.getCommand()) ) {
            if (msg1.getMsgContent()==null&&msg2.getMsgContent()==null) return true;
            if (((msg1.getMsgContent()!=null&&msg2.getMsgContent()!=null))
              &&(((Map)msg1.getMsgContent()).get("ObjId").equals(((Map)msg2.getMsgContent()).get("ObjId")))
              &&(((Map)msg1.getMsgContent()).get("TalkId").equals(((Map)msg2.getMsgContent()).get("TalkId")))
              &&(((Map)msg1.getMsgContent()).get("SeqNum").equals(((Map)msg2.getMsgContent()).get("SeqNum")))) return true;
        }
        return false;
    }
}