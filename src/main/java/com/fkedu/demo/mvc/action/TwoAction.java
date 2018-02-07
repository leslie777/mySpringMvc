package com.fkedu.demo.mvc.action;

import com.fkedu.demo.service.IDemoService;
import com.fkedu.mvcframework.annotation.FKAutowired;
import com.fkedu.mvcframework.annotation.FKController;
import com.fkedu.mvcframework.annotation.FKRequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by fk on 2018/1/22.
 */
@FKController
@FKRequestMapping("/Two")
public class TwoAction {
    @FKAutowired(required = false)
    private IDemoService demoService;

    public void edit(HttpServletRequest req, HttpServletResponse resp,String name){
        String result = demoService.get(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
