package com.woting.exceptionC;

/**
 * 缓存加载异常，内部码为1000，基本信息为‘缓存加载’
 * @author wh
 */
public class Wtcm1000CException extends WtcmCException {
    private static final long serialVersionUID = -4861897750983245256L;
    private static String myBaseMsg = "内容管理—缓存加载"; //基本信息
    private static int myCode = 1000; //内部编码

    /**
     * 构造没有详细消息内容的——内容管理—缓存加载
     * @throws Wtcm1000CException 若设置的分类码或内部码不符合规范
     */
    public Wtcm1000CException() {
        super(myCode, myBaseMsg);
    }

    /**
     * 构造有详细消息内容的——内容管理—缓存加载
     * @param message 详细消息
     */
    public Wtcm1000CException(String msg) {
        super(myCode, myBaseMsg, msg);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息构造新——内容管理—缓存加载
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Wtcm1000CException(Throwable cause) {
        super(myCode, myBaseMsg, cause);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息，以及详细消息构造新——内容管理—缓存加载
     * @param message 详细消息
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Wtcm1000CException(String msg, Throwable cause) {
        super(myCode, myBaseMsg, msg, cause);
    }

    public Wtcm1000CException(String msg, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(myCode, myBaseMsg, msg, cause, enableSuppression, writableStackTrace);
    }
}