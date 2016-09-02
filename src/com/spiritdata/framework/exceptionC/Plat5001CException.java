package com.spiritdata.framework.exceptionC;

/**
 * 阻塞锁异常，内部码为5001，基本信息为'阻塞锁异常'。
 * 请参看:
 * {@linkplain com.spiritdata.framework.ext.redis.RedisBlockLock RedisBlockLock}
 * @author wh
 */
public class Plat5001CException extends PlatCException {
    private static final long serialVersionUID = -3106815771021862676L;

    private static String myBaseMsg = "阻塞锁异常";
    private static int myCode = 5001;

    /**
     * 构造没有详细消息内容的——'阻塞锁'异常
     */
    public Plat5001CException() {
        super(myCode, myBaseMsg);
    }

    /**
     * 构造有详细消息内容的——'阻塞锁'异常
     * @param message 详细消息
     */
    public Plat5001CException(String msg) {
        super(myCode, myBaseMsg, msg);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息构造新——'阻塞锁'异常
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Plat5001CException(Throwable cause) {
        super(myCode, myBaseMsg, cause);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息，以及详细消息构造新——'阻塞锁'异常
     * @param message 详细消息
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Plat5001CException(String msg, Throwable cause) {
        super(myCode, myBaseMsg, msg, cause);
    }

    public Plat5001CException(String msg, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(myCode, myBaseMsg, msg, cause, enableSuppression, writableStackTrace);
    }
}