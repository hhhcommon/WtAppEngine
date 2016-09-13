package com.spiritdata.framework.ext.redis;

import org.springframework.data.redis.connection.RedisConnection;
import com.spiritdata.framework.exceptionC.Plat5001CException;
import com.spiritdata.framework.util.SequenceUUID;

/**
 * 用Redis实现的阻塞锁。
 * 阻塞锁只有返回成功的情况，若加锁不成功则抛出异常Plat5001CException。
 * @author wanghui
 */
public class RedisBlockLock implements ExpirableBlockKey {
    private BlockLockConfig blConf; //阻塞锁设置
    private RedisConnection conn; //redis连接

    private byte[] lockKey; //锁标识
    private byte[] lockValue; //锁值，尽量是一个随机数
    private long expireTime=30000; //过期时间，默认锁的时间是30秒钟

    public BlockLockConfig getBlConf() {
        return blConf;
    }

    public void setBlConf(BlockLockConfig blConf) {
        this.blConf = blConf;
    }

    public long getExpireTime() {
        return expireTime;
    }

    @Override
    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    /**
     * 构造函数，其他未设定的值将按如下配置设置：
     * <pre>
     * 阻塞锁设置：采用默认设置{@linkplain com.spiritdata.framework.ext.redis.BlockLockConfig BlockLockConfig}
     * 锁值：此时锁的值为系统给出的一个随机数
     * 过期时间：采用默认只100毫秒
     * <pre>
     * @param key 锁的key值
     * @param conn redis连接
     */
    public RedisBlockLock(String key, RedisConnection conn) {
        super();

        if (conn==null) throw new IllegalArgumentException();
        this.conn=conn;

        this.blConf=new BlockLockConfig();

        this.lockKey=key.getBytes();
        this.lockValue=SequenceUUID.getPureUUID().getBytes();
    }

    /**
     * 构造函数，其他未设定的值将按如下配置设置：
     * <pre>
     * 阻塞锁设置：采用默认设置{@linkplain com.spiritdata.framework.ext.redis.BlockLockConfig BlockLockConfig}
     * 锁值：此时锁的值为系统给出的一个随机数
     * <pre>
     * @param key 锁的key值
     * @param conn redis连接
     * @param expireTime 过期时间
     */
    public RedisBlockLock(String key, RedisConnection conn, long expireTime) {
        super();
        if (conn==null) throw new IllegalArgumentException();
        this.conn=conn;

        this.blConf=new BlockLockConfig();

        this.lockKey=key.getBytes();
        this.lockValue=SequenceUUID.getPureUUID().getBytes();

        this.expireTime=expireTime;
    }

    /**
     * 构造函数，其他未设定的值将按如下配置设置：
     * <pre>
     * 锁值：此时锁的值为系统给出的一个随机数
     * 过期时间：采用默认只100毫秒
     * <pre>
     * @param key 锁的key值
     * @param conn redis连接
     * @param blConf 锁配置信息
     */
    public RedisBlockLock(String key, RedisConnection conn, BlockLockConfig blConf) {
        super();
        if (conn==null) throw new IllegalArgumentException();
        this.conn=conn;

        this.blConf=(blConf==null?new BlockLockConfig():blConf);

        this.lockKey=key.getBytes();
        this.lockValue=SequenceUUID.getPureUUID().getBytes();
    }

    /**
     * 构造函数，其他未设定的值将按如下配置设置：
     * <pre>
     * 锁值：此时锁的值为系统给出的一个随机数
     * <pre>
     * @param key 锁的key值
     * @param conn redis连接
     * @param expireTime 过期时间
     * @param blConf 锁配置信息
     */
    public RedisBlockLock(String key, RedisConnection conn, long expireTime, BlockLockConfig blConf) {
        super();

        if (conn==null) throw new IllegalArgumentException();
        this.conn=conn;

        this.blConf=(blConf==null?new BlockLockConfig():blConf);

        this.lockKey=key.getBytes();
        this.lockValue=SequenceUUID.getPureUUID().getBytes();
    }

    /**
     * 构造函数，其他未设定的值将按如下配置设置：
     * <pre>
     * 阻塞锁设置：采用默认设置{@linkplain com.spiritdata.framework.ext.redis.BlockLockConfig BlockLockConfig}
     * 过期时间：采用默认只100毫秒
     * <pre>
     * @param key 锁的key
     * @param value 锁的值
     * @param conn redis连接
     */
    public RedisBlockLock(String key, String value, RedisConnection conn) {
        super();

        if (conn==null) throw new IllegalArgumentException();
        this.conn=conn;

        this.blConf=new BlockLockConfig();

        this.lockKey=key.getBytes();
        this.lockValue=value.getBytes();
    }

    /**
     * 构造函数，其他未设定的值将按如下配置设置：
     * <pre>
     * 阻塞锁设置：采用默认设置{@linkplain com.spiritdata.framework.ext.redis.BlockLockConfig BlockLockConfig}
     * <pre>
     * @param key 锁的key
     * @param value 锁的值
     * @param conn redis连接
     * @param expireTime 过期时间
     */
    public RedisBlockLock(String key, String value, RedisConnection conn, long expireTime) {
        super();

        if (conn==null) throw new IllegalArgumentException();
        this.conn=conn;

        this.blConf=(blConf==null?new BlockLockConfig():blConf);

        this.lockKey=key.getBytes();
        this.lockValue=value.getBytes();

        this.expireTime=expireTime;
    }

    /**
     * 构造函数，其他未设定的值将按如下配置设置：
     * <pre>
     * 过期时间：采用默认只100毫秒
     * <pre>
     * @param key 锁的key
     * @param value 锁的值
     * @param conn redis连接
     * @param blConf 锁配置信息
     */
    public RedisBlockLock(String key, String value, RedisConnection conn, BlockLockConfig blConf) {
        super();

        if (conn==null) throw new IllegalArgumentException();
        this.conn=conn;

        this.blConf=new BlockLockConfig();

        this.lockKey=key.getBytes();
        this.lockValue=value.getBytes();
    }

    /**
     * 构造函数，所有可定义属性都在构造函数中设定
     * @param key 锁的key
     * @param value 锁的值
     * @param conn redis连接
     * @param expireTime 过期时间
     * @param blConf 锁配置信息
     */
    public RedisBlockLock(String key, String value, RedisConnection conn, long expireTime, BlockLockConfig blConf) {
        super();

        if (conn==null) throw new IllegalArgumentException();
        this.conn=conn;

        this.blConf=new BlockLockConfig();

        this.lockKey=key.getBytes();
        this.lockValue=value.getBytes();

        this.expireTime=expireTime;
    }

    /**
     * 得到锁，并上锁。
     * @throws Plat5001CException 若上锁失败，则抛出异常
     */
    @Override
    public void lock() throws Plat5001CException {
        boolean canContinue=false;
        long beginTime=System.currentTimeMillis();
        long curTime=System.currentTimeMillis();
        int loopIndex=0;
        int waitType=this.blConf.getWaitingType();
        while (true) {
            canContinue=conn.setNX(this.lockKey, this.lockValue);
            if (canContinue) canContinue=conn.pExpire(this.lockKey, this.expireTime);
            byte[] _value=conn.get(this.lockKey);
            canContinue=equalValue(_value);
            if (canContinue) return;

            curTime=System.currentTimeMillis();
            if (waitType==1) {
                canContinue=(loopIndex<this.blConf.getWaitingLoopCount());
            } else  if (waitType==2) {
                canContinue=(curTime-beginTime<this.blConf.getWaitingTime());
            } else  if (waitType==3) {
                canContinue=(loopIndex<this.blConf.getWaitingLoopCount())&&(curTime-beginTime<this.blConf.getWaitingTime());
            } else  if (waitType==4) {
                canContinue=(loopIndex<this.blConf.getWaitingLoopCount())||(curTime-beginTime<this.blConf.getWaitingTime());
            }
            if (!canContinue) throw new Plat5001CException("获取所失败");

            canContinue=false;
            loopIndex++;

            try {
                Thread.sleep(this.blConf.getLoopIntervalTime());
            } catch(Exception e) {
                new Plat5001CException("获取所失败", e);
            }
        }
    }

    /**
     * 延长锁的过期时间，延长时间为当前锁的过期时间。
     * @throws Plat5001CException 若锁被修改过，则抛出异常
     */
    @Override
    public void extendExpire() {
        extendExpire(this.expireTime);
    }

    /**
     * 按给定时间(毫秒)延长锁的过期时间。
     * @throws Plat5001CException 若锁被修改过，则抛出异常
     */
    @Override
    public void extendExpire(long extendTime) {
        byte[] _value=conn.get(lockKey);
        if (!equalValue(_value)) throw new Plat5001CException("延长锁过期时间时发现锁值被改变");
        if (!conn.pExpire(this.lockKey, extendTime)) throw new Plat5001CException("延长锁过期时间失败");
        
    }
    /**
     * 释放锁。<br/>
     * 注意：若有返回值，就说明解锁成功，只不过，若为true，基本可以保证在加锁期间，没有其他人成功获得锁。若返回false，则表明，可能有其他人获得锁或锁被删除了
     * @throws Plat5001CException 释放锁失败，则抛出异常
     */
    @Override
    public boolean unlock() throws Plat5001CException {
        byte[] _value=conn.get(this.lockKey);
        long ret=conn.del(this.lockKey);
        if (ret==0) return false; //加锁过程中，锁被莫名/恶意删除了
        if (ret!=1) throw new Plat5001CException("释放锁");
        return equalValue(_value);
    }

    /*
     * 给定的值是否与锁的值相同
     * @param value 给的的值，byte数组
     * @return 相同返回true，否则返回false
     */
    private boolean equalValue(byte[] value) {
        if (this.lockValue==null&&value==null) return true;
        if (value==null) return false;

        if (this.lockValue.length!=value.length) return false;
        else {
            for (int i=0; i<value.length; i++) {
                if (value[i]!=this.lockValue[i]) return false;
            }
            return true;
        }
    }

    //以下为静态方法，为便于锁的使用
    /**
     * 获得锁，并返回锁实例
     * 
     * @param key 锁的key值
     * @param conn redis连接
     * @return 可过期阻塞锁
     */
    public static ExpirableBlockKey lock(String key, RedisConnection conn) {
        ExpirableBlockKey retLock=new RedisBlockLock(key, conn);
        retLock.lock();
        return retLock;
    }

    /**
     * 获得锁，并返回锁实例
     * 
     * @param key 锁的key值
     * @param conn redis连接
     * @param expireTime 过期时间
     * @return 可过期阻塞锁
     */
    public static ExpirableBlockKey lock(String key, RedisConnection conn, long expireTime) {
        ExpirableBlockKey retLock=new RedisBlockLock(key, conn, expireTime);
        retLock.lock();
        return retLock;
    }

    /**
     * 获得锁，并返回锁实例
     * 
     * @param key 锁的key值
     * @param conn redis连接
     * @param blConf 锁配置信息
     * @return 可过期阻塞锁
     */
    public static ExpirableBlockKey lock(String key, RedisConnection conn, BlockLockConfig blConf) {
        ExpirableBlockKey retLock=new RedisBlockLock(key, conn, blConf);
        retLock.lock();
        return retLock;
    }

    /**
     * 获得锁，并返回锁实例
     * 
     * @param key 锁的key值
     * @param conn redis连接
     * @param expireTime 过期时间
     * @param blConf 锁配置信息
     * @return 可过期阻塞锁
     */
    public static ExpirableBlockKey lock(String key, RedisConnection conn, long expireTime, BlockLockConfig blConf) {
        ExpirableBlockKey retLock=new RedisBlockLock(key, conn, expireTime, blConf);
        retLock.lock();
        return retLock;
    }

    /**
     * 获得锁，并返回锁实例
     * 
     * @param key 锁的key
     * @param value 锁的值
     * @param conn redis连接
     * @return 可过期阻塞锁
     */
    public static ExpirableBlockKey lock(String key, String value, RedisConnection conn) {
        ExpirableBlockKey retLock=new RedisBlockLock(key, value, conn);
        retLock.lock();
        return retLock;
    }

    /**
     * 获得锁，并返回锁实例
     * 
     * @param key 锁的key
     * @param value 锁的值
     * @param conn redis连接
     * @param expireTime 过期时间
     * @return 可过期阻塞锁
     */
    public static ExpirableBlockKey lock(String key, String value, RedisConnection conn, long expireTime) {
        ExpirableBlockKey retLock=new RedisBlockLock(key, value, conn, expireTime);
        retLock.lock();
        return retLock;
    }

    /**
     * 获得锁，并返回锁实例
     * 
     * @param key 锁的key
     * @param value 锁的值
     * @param conn redis连接
     * @param blConf 锁配置信息
     * @return 可过期阻塞锁
     */
    public static ExpirableBlockKey lock(String key, String value, RedisConnection conn, BlockLockConfig blConf) {
        ExpirableBlockKey retLock=new RedisBlockLock(key, value, conn, blConf);
        retLock.lock();
        return retLock;
    }

    /**
     * 获得锁，并返回锁实例
     * 
     * @param key 锁的key
     * @param value 锁的值
     * @param conn redis连接
     * @param expireTime 过期时间
     * @param blConf 锁配置信息
     * @return 可过期阻塞锁
     */
    public static ExpirableBlockKey lock(String key, String value, RedisConnection conn, long expireTime, BlockLockConfig blConf) {
        ExpirableBlockKey retLock=new RedisBlockLock(key, value, conn, expireTime, blConf);
        retLock.lock();
        return retLock;
    }
}