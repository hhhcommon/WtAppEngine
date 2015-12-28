package com.woting.mobile.push.model;

/**
 * 比较两个消息是否是同批次的消息，消息应该有批次号
 * @author wanghui
 */
public interface CompareMsg {
     public boolean compare(Message msg1, Message msg2);
}