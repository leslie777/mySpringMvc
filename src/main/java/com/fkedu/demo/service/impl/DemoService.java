package com.fkedu.demo.service.impl;

import com.fkedu.demo.service.IDemoService;
import com.fkedu.mvcframework.annotation.FKService;

/**
 * Created by fk on 2018/1/22.
 */

@FKService("tom")
public class DemoService implements IDemoService {

    @Override
    public String get(String name){
        return "My name is " + name;
    }
}
