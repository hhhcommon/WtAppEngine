package com.woting.exceptionC;

import com.spiritdata.framework.CodeException;

public abstract class WtccCException extends CodeException {
    private static final long serialVersionUID = -6396939068332617918L;
    //代码分类(category)
    private static String category = "WTCC"; //分类码Wt Crawl Control

    //扩充父类的构造函数
    /**
     * 构造没有详细消息内容的——Wt抓取控制“带码异常”
     * @param c 内部码
     * @param bMsg 基础信息
     */
    protected WtccCException(int c, String bMsg) {
        super(category, c, bMsg);
    }

    /**
     * 构造有详细消息内容的——Wt抓取控制“带码异常”
     * @param c 内部码
     * @param bMsg 基础信息
     * @param message 详细消息
     */
    protected WtccCException(int c, String bMsg, String message) {
        super(category, c, bMsg, message);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息构造新——Wt抓取控制“带码异常”
     * @param c 内部码
     * @param bMsg 基础信息
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    protected WtccCException(int c, String bMsg, Throwable cause) {
        super(category, c, bMsg, cause);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息，以及详细消息构造新——Wt抓取控制“带码异常”
     * @param c 内部码
     * @param bMsg 基础信息
     * @param message 详细消息
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    protected WtccCException(int c, String bMsg, String message, Throwable cause) {
        super(category, c, bMsg, message, cause);
    }

    protected WtccCException(int c, String bMsg, String message,Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(category, c, bMsg, message, cause, enableSuppression, writableStackTrace);
    }
}