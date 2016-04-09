package com.woting.exceptionC;

/**
 * Wt内容管理中字典处理，内部码为0101，基本信息为'专辑绑定单曲'异常
 * @author wh
 */
public class Wtcm0101CException extends WtcmCException {
    private static final long serialVersionUID = -9099908628515445808L;

    private static String myBaseMsg = "专辑绑定单曲";
    private static int myCode = 101;

    /**
     * 构造没有详细消息内容的——'专辑绑定单曲'异常
     */
    public Wtcm0101CException() {
        super(myCode, myBaseMsg);
    }

    /**
     * 构造有详细消息内容的——'专辑绑定单曲'异常
     * @param message 详细消息
     */
    public Wtcm0101CException(String msg) {
        super(myCode, myBaseMsg, msg);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息构造新——'专辑绑定单曲'异常
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Wtcm0101CException(Throwable cause) {
        super(myCode, myBaseMsg, cause);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息，以及详细消息构造新——'专辑绑定单曲'异常
     * @param message 详细消息
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Wtcm0101CException(String msg, Throwable cause) {
        super(myCode, myBaseMsg, msg, cause);
    }

    public Wtcm0101CException(String msg, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(myCode, myBaseMsg, msg, cause, enableSuppression, writableStackTrace);
    }
}