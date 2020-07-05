package com.atguigu.gmall1213.product.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.product.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/product/test")
public class TestController {

    @Autowired
    private TestService testService;

    @GetMapping("testLock")
    public Result testLock(){
        testService.testLock();
        return Result.ok();
    }
}
