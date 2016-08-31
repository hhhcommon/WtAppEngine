package com.spiritdata.framework.ext.redis;

import com.spiritdata.framework.exceptionC.Plat5001CException;

/**
 * 带过期功能的阻塞锁
 * @author wanghui
 */
public interface ExpirableBlockKey {
    public void setExpireTime(long expireTime);

    /**
     * 获得锁。
     */
    public void lock();

    /**
     * 延长锁的过期时间，延长时间为当前锁的过期时间。
     * @throws Plat5001CException 若锁被修改过，则抛出异常
     */
    public void extendExpire();

    /**
     * 按给定时间(毫秒)延长锁的过期时间。
     * @throws Plat5001CException 若锁被修改过，则抛出异常
     */
    public void extendExpire(long extendTime);

    /**
     * 释放锁。<br>
     * 注意：若有返回值()，就说明解锁成功，只不过，若为true，基本可以保证在加锁期间，没有其他人成功获得锁。若返回false，则表明，可能有其他人获得锁或锁被删除了
     */
    public boolean unlock();
}