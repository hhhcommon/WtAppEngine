package com.spiritdata.framework.exceptionC;

/**
 * 阻塞锁异常，内部码为5101，基本信息为'K-V中K生成异常'。
 * 请参看:
 * {@linkplain com.woting.passport.session 包[com.woting.passport.session]}
 * @author wh
 */
public class Plat5101CException extends PlatCException {
    private static final long serialVersionUID = -3106815771021862676L;

    private static String myBaseMsg = "K-V中K生成异常";
    private static int myCode = 5101;

    /**
     * 构造没有详细消息内容的——'K-V中K生成'异常
     */
    public Plat5101CException() {
        super(myCode, myBaseMsg);
    }

    /**
     * 构造有详细消息内容的——'K-V中K生成'异常
     * @param message 详细消息
     */
    public Plat5101CException(String msg) {
        super(myCode, myBaseMsg, msg);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息构造新——'K-V中K生成'异常
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Plat5101CException(Throwable cause) {
        super(myCode, myBaseMsg, cause);
    }

    /**
     * 根据指定的原因和(cause==null?null:cause.toString())的详细消息，以及详细消息构造新——'K-V中K生成'异常
     * @param message 详细消息
     * @param cause 异常原因，以后通过Throwable.getCause()方法获取它。允许使用null值，指出原因不存在或者是未知的异常
     */
    public Plat5101CException(String msg, Throwable cause) {
        super(myCode, myBaseMsg, msg, cause);
    }

    public Plat5101CException(String msg, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(myCode, myBaseMsg, msg, cause, enableSuppression, writableStackTrace);
    }
}