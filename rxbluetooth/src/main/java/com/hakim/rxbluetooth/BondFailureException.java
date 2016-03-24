package com.hakim.rxbluetooth;

/**
 * OKLine(ShenZhen) co.,Ltd.<br/>
 * Author : Shi Haijun <br/>
 * Email : haijun@okline.cn<br/>
 * Date : 2016/3/19 21:43<br/>
 * Desc :
 */
public class BondFailureException extends Exception {
    public BondFailureException() {
        super();
    }

    public BondFailureException(String detailMessage) {
        super(detailMessage);
    }

    public BondFailureException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public BondFailureException(Throwable throwable) {
        super(throwable);
    }
}
