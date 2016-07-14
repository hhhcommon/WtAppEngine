package com.woting.appengine.mobile.session.model;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.woting.appengine.mobile.model.MobileKey;

/**
 * 移动端会话
 * @author wh
 */
public class MobileSession implements Serializable {
    private static final long serialVersionUID=7375288306760265060L;

    private MobileKey key; //会话的键值
    private Map<String, Object> body; //会话数据体，会话的上下文数据

    public MobileKey getKey() {
        return key;
    }
    public void setKey(MobileKey key) {
        this.key=key;
    }
    public Map<String, Object> getBody() {
        return body;
    }
    public void setBody(Map<String, Object> body) {
        this.body=body;
    }

    protected long creationTime=System.currentTimeMillis();//创建时间
    protected volatile long lastAccessedTime=this.creationTime; //最后访问时间
    protected volatile transient boolean expiring=false; //是否过期

    public MobileSession(MobileKey key) {
        this.key=key;
        body=new ConcurrentHashMap<String, Object>();
    }

    /**
     * 设置过期
     */
    public void expire() {
        this.expiring=true;
    }

    /**
     * 访问会话
     */
    public void access() {
        this.expiring=false;
        this.lastAccessedTime=System.currentTimeMillis();
    }

    /**
     * 得到最后访问时间
     */
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    /**
     * 是否过期
     */
    public boolean expired() {
        return this.expiring;
    }

    /**
     * 增加一个属性
     */
    public void addAttribute(String key, Object value) {
        this.body.put(key, value);
    }

    /**
     * 删除一个属性
     */
    public void remove(String key) {
        this.body.remove(key);
    }

    /**
     * 获得一个属性
     */
    public Object getAttribute(String key) {
        return this.body.get(key);
    }

    /**
     * 清除会话数据体
     */
    public void clearBody() {
        this.body.clear();
    }
}