package com.fkedu.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * Created by fk on 2018/1/22.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FKService {
    String value() default "";
}
