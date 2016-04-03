package com.woting.exceptionC;

/**
 * 不符合逻辑的第一次Etl过程，内部码为1002，基本信息为‘非法Etl1配置’
 * @author wh
 */
public class Wtcc1002CException extends WtccCException {
    private static final long serialVersionUID = 5854393142306188860L;
    private static String myBaseMsg = "第一次Etl—非法Etl1配置"; //基本信息
    private static int myCode = 1001; //内部编码

    /**
     * 构造没有详细消息内容的——第一次Etl—非法Etl1配置
     * @throws Wtcc1002CException 若设置的分类码或内部码不符合规范
     */
    public Wtcc1002CException() {
        super(myCode, myBaseMsg);
    }

    /**
     * 构造有详细消息内容的——第一次Etl—非法Etl1配置
     * @param message 详细消息
     */
    public Wtcc1002CException(String msg) {
        super(myCode, myBaseMsg, msg);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息构造新——第一次Etl—非法Etl1配置
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Wtcc1002CException(Throwable cause) {
        super(myCode, myBaseMsg, cause);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息，以及详细消息构造新——第一次Etl—非法Etl1配置
     * @param message 详细消息
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Wtcc1002CException(String msg, Throwable cause) {
        super(myCode, myBaseMsg, msg, cause);
    }

    public Wtcc1002CException(String msg, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(myCode, myBaseMsg, msg, cause, enableSuppression, writableStackTrace);
    }
}
