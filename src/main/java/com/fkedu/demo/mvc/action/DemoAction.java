package com.fkedu.demo.mvc.action;

import com.fkedu.demo.service.IDemoService;
import com.fkedu.mvcframework.annotation.FKAutowired;
import com.fkedu.mvcframework.annotation.FKController;
import com.fkedu.mvcframework.annotation.FKRequestMapping;
import com.fkedu.mvcframework.annotation.FKRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by fk on 2018/1/22.
 */

@FKController
@FKRequestMapping("/web")
public class DemoAction {

    @FKAutowired(required = false)
    private IDemoService demoService;

    @FKRequestMapping("/query.json")
    public void query(HttpServletRequest request, HttpServletResponse resp,@FKRequestParam("name") String name){
        String result = demoService.get(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FKRequestMapping("/edit.json")
    public void edit(HttpServletRequest request, HttpServletResponse resp,
                     @FKRequestParam("id") Integer id){
        System.out.println(id);
    }

}
