package com.hakim.rxbluetooth;

/**
 * OKLine(ShenZhen) co.,Ltd.<br/>
 * Author : Shi Haijun <br/>
 * Email : haijun@okline.cn<br/>
 * Date : 2016/3/19 21:49<br/>
 * Desc :
 */
public class ConnectFailureException extends IllegalStateException {
    public ConnectFailureException() {
        super();
    }

    public ConnectFailureException(String detailMessage) {
        super(detailMessage);
    }

    public ConnectFailureException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public ConnectFailureException(Throwable throwable) {
        super(throwable);
    }
}
