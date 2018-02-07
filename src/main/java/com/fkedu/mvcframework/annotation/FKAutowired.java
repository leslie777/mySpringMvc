package com.fkedu.mvcframework.annotation;

import java.lang.annotation.*;

/** @Autowired使用在setter或者constructor，成员变量中时，默认使用bytype方式注入，如果有多个实现类，会报错NoUniqueBeanDefinitionException，
        可以用@Qualifier("beanname")指定在参数上byname注入，不如使用@Resource
        可以指定required=false来非必需提供

    注意，@Resource 可以带name属性，不带则用名字byname
 * Created by fk on 2018/1/22.
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FKAutowired {
    boolean required() default true;
}
