package com.woting.exceptionC;

/**
 * Wt内容管理中字典处理，内部码为0301，基本信息为'字典处理'异常
 * @author wh
 */
public class Wtcm0301CException extends WtcmCException {
    private static final long serialVersionUID = -5329451830761851306L;

    private static String myBaseMsg = "字典处理";
    private static int myCode = 301;

    /**
     * 构造没有详细消息内容的——'字典处理'异常
     */
    public Wtcm0301CException() {
        super(myCode, myBaseMsg);
    }

    /**
     * 构造有详细消息内容的——'字典处理'异常
     * @param message 详细消息
     */
    public Wtcm0301CException(String msg) {
        super(myCode, myBaseMsg, msg);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息构造新——'字典处理'异常
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Wtcm0301CException(Throwable cause) {
        super(myCode, myBaseMsg, cause);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息，以及详细消息构造新——'字典处理'异常
     * @param message 详细消息
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Wtcm0301CException(String msg, Throwable cause) {
        super(myCode, myBaseMsg, msg, cause);
    }

    public Wtcm0301CException(String msg, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(myCode, myBaseMsg, msg, cause, enableSuppression, writableStackTrace);
    }
}