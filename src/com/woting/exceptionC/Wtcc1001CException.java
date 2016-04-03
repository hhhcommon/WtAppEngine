package com.woting.exceptionC;

/**
 * 不符合逻辑的抓取方案异常，内部码为1001，基本信息为‘非法可执行方案’
 * @author wh
 */
public class Wtcc1001CException extends WtccCException {
    private static final long serialVersionUID = 753938954667561729L;
    private static String myBaseMsg = "抓取方案—非法可执行方案"; //基本信息
    private static int myCode = 1001; //内部编码

    /**
     * 构造没有详细消息内容的——抓取方案—非法可执行方案
     * @throws Wtcc1001CException 若设置的分类码或内部码不符合规范
     */
    public Wtcc1001CException() {
        super(myCode, myBaseMsg);
    }

    /**
     * 构造有详细消息内容的——抓取方案—非法可执行方案
     * @param message 详细消息
     */
    public Wtcc1001CException(String msg) {
        super(myCode, myBaseMsg, msg);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息构造新——抓取方案—非法可执行方案
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Wtcc1001CException(Throwable cause) {
        super(myCode, myBaseMsg, cause);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息，以及详细消息构造新——抓取方案—非法可执行方案
     * @param message 详细消息
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Wtcc1001CException(String msg, Throwable cause) {
        super(myCode, myBaseMsg, msg, cause);
    }

    public Wtcc1001CException(String msg, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(myCode, myBaseMsg, msg, cause, enableSuppression, writableStackTrace);
    }
}
