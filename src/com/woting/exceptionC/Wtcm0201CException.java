package com.woting.exceptionC;

/**
 * Wt内容管理中栏目处理，内部码为0201，基本信息为'栏目处理'异常
 * @author wh
 */
public class Wtcm0201CException extends WtcmCException {
    private static final long serialVersionUID = -7915636493141406969L;

    private static String myBaseMsg = "栏目处理";
    private static int myCode = 201;

    /**
     * 构造没有详细消息内容的——'栏目处理'异常
     */
    public Wtcm0201CException() {
        super(myCode, myBaseMsg);
    }

    /**
     * 构造有详细消息内容的——'栏目处理'异常
     * @param message 详细消息
     */
    public Wtcm0201CException(String msg) {
        super(myCode, myBaseMsg, msg);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息构造新——'栏目处理'异常
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Wtcm0201CException(Throwable cause) {
        super(myCode, myBaseMsg, cause);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息，以及详细消息构造新——'栏目处理'异常
     * @param message 详细消息
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Wtcm0201CException(String msg, Throwable cause) {
        super(myCode, myBaseMsg, msg, cause);
    }

    public Wtcm0201CException(String msg, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(myCode, myBaseMsg, msg, cause, enableSuppression, writableStackTrace);
    }
}