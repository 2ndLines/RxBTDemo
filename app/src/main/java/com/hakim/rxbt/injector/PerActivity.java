package com.hakim.rxbt.injector;

import java.lang.annotation.Retention;

import javax.inject.Scope;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * OKLine(ShenZhen) co.,Ltd.<br/>
 * Author : Shi Haijun <br/>
 * Email : haijun@okline.cn<br/>
 * Date : 2016/3/24 11:17<br/>
 * Desc :
 */
@Scope
@Retention(RUNTIME)
public @interface PerActivity {
}
